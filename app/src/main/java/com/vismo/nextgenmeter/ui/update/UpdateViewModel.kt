package com.vismo.nextgenmeter.ui.update

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.PowerManager
import android.util.Log
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.storage.FirebaseStorage
import com.vismo.nextgenmeter.datastore.DeviceDataStore
import com.vismo.nextgenmeter.module.IoDispatcher
import com.vismo.nextgenmeter.repository.RemoteMeterControlRepository
import com.vismo.nextgenmeter.service.OnUpdateCompletedReceiver
import com.vismo.nxgnfirebasemodule.model.Update
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
                            writeUpdateResultToFireStore(update.copy(
                                completedOn = Timestamp.now()
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
            if (update != null && (update.type == Constant.OTA_FIRMWARE_TYPE || update.type == Constant.OTA_METERAPP_TYPE)) {
                FirebaseStorage.getInstance().getReferenceFromUrl(update.url).downloadUrl
                    .addOnSuccessListener {
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
                if (update.type == Constant.OTA_METERAPP_TYPE) {
                    installApk(targetFile)
                } else if (update.type == Constant.OTA_FIRMWARE_TYPE) {
                    remoteMeterControlRepository.requestPatchFirmwareToMCU(targetFile.absolutePath)
                }
            } catch (e: Exception) {
                _updateState.value = UpdateState.Error(e.message ?: "Unknown error", allowRetry = true)
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
                        completedOn = Timestamp.now()
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

    private fun writeUpdateResultToFireStore(update: Update) {
        viewModelScope.launch(ioDispatcher) {
            remoteMeterControlRepository.writeUpdateResultToFireStore(update)
        }
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