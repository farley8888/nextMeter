package com.vismo.nextgenmeter.ui.update

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.os.PowerManager
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.storage.FirebaseStorage
import com.vismo.nextgenmeter.BuildConfig
import com.vismo.nextgenmeter.datastore.DeviceDataStore
import com.vismo.nextgenmeter.module.IoDispatcher
import com.vismo.nextgenmeter.repository.RemoteMeterControlRepository
import com.vismo.nextgenmeter.service.OnUpdateCompletedReceiver
import com.vismo.nextgenmeter.util.Constant.ENV_PRD
import com.vismo.nxgnfirebasemodule.model.Update
import com.vismo.nxgnfirebasemodule.model.UpdateStatus
import com.vismo.nxgnfirebasemodule.util.Constant
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.net.URL
import javax.inject.Inject

@HiltViewModel
class UpdateViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val remoteMeterControlRepository: RemoteMeterControlRepository,
): ViewModel() {
    private val _updateState: MutableStateFlow<UpdateState> = MutableStateFlow(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState

    val updateDetails = remoteMeterControlRepository.remoteUpdateRequest.stateIn(viewModelScope, started = SharingStarted.Eagerly, initialValue = null)

    init {
        checkForUpdates()
        observeFlows()
    }

    private fun observeFlows() {
        viewModelScope.launch(ioDispatcher) {
            launch {
                DeviceDataStore.isFirmwareUpdateComplete.collectLatest {
                    Log.d(TAG, " is firmware update complete - $it")
                    if(it) {
                        _updateState.value = UpdateState.Success
                        updateDetails.firstOrNull()?.let { update ->
                            remoteMeterControlRepository.saveRecentlyCompletedUpdateId(update.id)
                            writeUpdateResultToFireStore(update.copy(
                                completedOn = Timestamp.now(),
                                status = UpdateStatus.WAITING_FOR_RESTART
                            ))
                        }
                        delay(4000)
                        val powerManager = context.getSystemService(PowerManager::class.java) as PowerManager
                        powerManager.reboot("firmware upgraded")
                    }
                }
            }
        }
    }

    private fun checkForUpdates() {
        viewModelScope.launch(ioDispatcher) {
            val update = remoteMeterControlRepository.remoteUpdateRequest.firstOrNull()
            val wifiCredential = remoteMeterControlRepository.meterSdkConfiguration.firstOrNull()?.wifiCredential
            if (update != null && (update.type == Constant.OTA_FIRMWARE_TYPE || update.type == Constant.OTA_METERAPP_TYPE)) {
                FirebaseStorage.getInstance().getReferenceFromUrl(update.url).downloadUrl
                    .addOnSuccessListener {
                        connectToWifiLegacy(context, ssid = wifiCredential?.ssid, password = wifiCredential?.password)
                        downloadFile(it, update)
                    }
                    .addOnFailureListener {
                        _updateState.value = UpdateState.Error(it.message ?: "Could not download error")
                    }

            } else {
                _updateState.value = UpdateState.NoUpdateFound
            }
        }
    }

    fun retryDownload() {
        _updateState.value = UpdateState.Idle
        checkForUpdates()
    }

    private fun downloadFile(uri: Uri, update: Update) {
        // Download the APK
        viewModelScope.launch(ioDispatcher) {
            _updateState.value = UpdateState.Downloading(0)
            writeUpdateResultToFireStore(
                update.copy(
                    status = UpdateStatus.DOWNLOADING
                )
            )
            try {
                val fileName = update.url.substringAfterLast("/").ifEmpty { "update.apk" }
                val externalFilesDir = context.getExternalFilesDir(null)
                if (externalFilesDir == null) {
                    _updateState.value = UpdateState.Error("External files directory not available.")
                    return@launch
                }
                val targetFile = File(externalFilesDir, fileName)
                Log.d(TAG, "Target file path: ${targetFile.absolutePath}")

                // Delete existing APK if present
                if (targetFile.exists()) {
                    Log.d(TAG, "Deleting existing APK: ${targetFile.absolutePath}")
                    val deleted = targetFile.delete()
                    if (!deleted) {
                        Log.e(TAG, "Failed to delete existing APK.")
                        _updateState.value = UpdateState.Error("Failed to delete existing APK.")
                        return@launch
                    }
                }

                val url = URL(uri.toString())
                val connection = url.openConnection()
                connection.connect()
                val inputStream = connection.getInputStream()
                val totalSize = connection.contentLengthLong

                targetFile.outputStream().use { output ->
                    val buffer = ByteArray(1024)
                    var downloadedSize = 0L
                    var bytesRead: Int
                    var progress = 0

                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloadedSize += bytesRead

                        // Calculate progress only if totalSize is known and greater than zero
                        if (totalSize > 0) {
                            val newProgress = ((downloadedSize * 100) / totalSize).toInt()
                            // Update progress only if it has changed to reduce unnecessary UI updates
                            if (newProgress != progress) {
                                progress = newProgress
                                _updateState.value = UpdateState.Downloading(progress)
                            }
                        } else {
                            // Handle unknown total size, e.g., show previous progress
                            _updateState.value = UpdateState.Downloading(progress)
                        }
                    }
                }
                inputStream.close()

                // Validate the downloaded APK
                _updateState.value = UpdateState.Installing
                writeUpdateResultToFireStore(
                    update.copy(
                        status = UpdateStatus.INSTALLING
                    )
                )
                if (update.type == Constant.OTA_METERAPP_TYPE) {
                    remoteMeterControlRepository.saveRecentlyCompletedUpdateId(updateDetails.firstOrNull()?.id ?: "")
                    installApk(targetFile)
                } else if (update.type == Constant.OTA_FIRMWARE_TYPE) {
                    remoteMeterControlRepository.requestPatchFirmwareToMCU(targetFile.absolutePath)
                }
            } catch (e: Exception) {
                _updateState.value = UpdateState.Error(e.message ?: "Unknown error", allowRetry = true)
                writeUpdateResultToFireStore(
                    update.copy(
                        status = UpdateStatus.DOWNLOAD_ERROR
                    )
                )
            } finally {
                if(BuildConfig.FLAVOR == ENV_PRD) {
                    disconnectAndDisableWifi(context)
                }
            }
        }
    }

    private fun installApk(apkFile: File) {
        viewModelScope.launch(Dispatchers.Main) {
            try {
                if (!apkFile.exists()) {
                    throw Exception("APK file does not exist at path: ${apkFile.absolutePath}")
                }
                val installer = context.packageManager.packageInstaller
                val resolver = context.contentResolver

                val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
                params.setAppPackageName(context.packageName)

                val apkUri = FileProvider.getUriForFile(context, context.packageName + ".fileProvider", apkFile)
                Log.d(TAG, "apkUri: ${apkUri.path}")
                resolver.openInputStream(apkUri)?.use { apkStream ->
                    val length = DocumentFile.fromSingleUri(context, apkUri)?.length() ?: -1
                    val sessionId = installer.createSession(params)
                    val session = installer.openSession(sessionId)

                    session.openWrite(apkFile.name, 0, length).use { sessionStream ->
                        apkStream.copyTo(sessionStream)
                         session.fsync(sessionStream)
                    }

                    Log.d(TAG, "Ready to install")

                    val intent = Intent(context, OnUpdateCompletedReceiver::class.java)
                    intent.action = Intent.ACTION_MY_PACKAGE_REPLACED

                    val pi = PendingIntent.getBroadcast(
                        context,
                        0,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                    )

                    session.commit(pi.intentSender)
                    session.close()
                }

                _updateState.value = UpdateState.Success
                updateDetails.firstOrNull()?.let {
                    writeUpdateResultToFireStore(it.copy(
                        completedOn = Timestamp.now(),
                        status = UpdateStatus.WAITING_FOR_RESTART
                    ))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Installation error: ${e.message}")
                _updateState.value = UpdateState.Error("Installation failed: ${e.message}")
            } finally {
                // Clean up the APK file
                if (apkFile.exists()) {
                    apkFile.delete()
                }
            }
        }
    }

    private fun disconnectAndDisableWifi(context: Context) {
        try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            wifiManager.disconnect()
            wifiManager.isWifiEnabled = false
        } catch (e: Exception) {
            Log.e("WiFiConnection", "Error disconnecting from WiFi: ${e.message}")
        }
    }

    private fun connectToWifiLegacy(context: Context, ssid: String?, password: String?) {
        try {
            if (ssid == null || password == null) {
                Toast.makeText(context, "SSID is null", Toast.LENGTH_SHORT).show()
                Log.e("WiFiConnection", "SSID or password is null")
                return
            }

            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

            // Check if WiFi is enabled
            if (!wifiManager.isWifiEnabled) {
                wifiManager.isWifiEnabled = true // Enable WiFi if it's disabled
            }

            // Create a WiFi configuration
            val wifiConfig = WifiConfiguration().apply {
                SSID = "\"$ssid\""
                preSharedKey = "\"$password\""
                allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK) // WPA2
            }

            // Add the WiFi configuration to the system
            val networkId = wifiManager.addNetwork(wifiConfig)

            if (networkId != -1) {
                // Disconnect from the current network
                wifiManager.disconnect()

                // Enable and connect to the new network
                wifiManager.enableNetwork(networkId, true)
                wifiManager.reconnect()

                Log.d("WiFiConnection", "Connected to $ssid")
            } else {
                Log.e("WiFiConnection", "Failed to connect to $ssid")
                Toast.makeText(context, "Failed to connect to $ssid", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("WiFiConnection", "Error connecting to WiFi: ${e.message}")
            Toast.makeText(context, "Error connecting to WiFi: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun writeUpdateResultToFireStore(update: Update) {
        remoteMeterControlRepository.writeUpdateResultToFireStore(update)
    }

    companion object {
        private const val TAG = "UpdateViewModel"
    }
}

sealed class UpdateState {
    data object Idle : UpdateState()
    data object NoUpdateFound : UpdateState()
    data class Downloading(val progress: Int) : UpdateState()
    data object Installing : UpdateState()
    data object Success : UpdateState()
    data class Error(val message: String, val allowRetry: Boolean = false) : UpdateState()
}