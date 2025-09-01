package com.vismo.nextgenmeter.ui.update

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
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
import com.google.android.gms.tasks.Task
import com.google.firebase.Timestamp
import com.google.firebase.storage.FirebaseStorage
import com.vismo.nextgenmeter.BuildConfig
import com.vismo.nextgenmeter.datastore.DeviceDataStore
import com.vismo.nextgenmeter.module.IoDispatcher
import com.vismo.nextgenmeter.repository.RemoteMeterControlRepository
import com.vismo.nextgenmeter.service.OnUpdateCompletedReceiver
import com.vismo.nextgenmeter.util.AndroidROMOTAUpdateManager
import com.vismo.nextgenmeter.util.Constant.ENV_PRD
import com.vismo.nextgenmeter.util.MeasureBoardUtils
import com.vismo.nextgenmeter.util.RomOtaState
import com.vismo.nxgnfirebasemodule.model.Update
import com.vismo.nxgnfirebasemodule.model.UpdateStatus
import com.vismo.nxgnfirebasemodule.model.WifiCredential
import com.vismo.nxgnfirebasemodule.util.Constant
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.IOException
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

@HiltViewModel
class UpdateViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val remoteMeterControlRepository: RemoteMeterControlRepository,
    private val androidROMOTAUpdateManager: AndroidROMOTAUpdateManager,

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
            // Observe ROM OTA progress broadcasted by AndroidROMOTAUpdateManager
            launch {
                androidROMOTAUpdateManager.romOtaEvents.collectLatest { event ->
                    handleRomOtaEvent(event)
                }
            }
        }
    }

    private suspend fun handleRomOtaEvent(event: RomOtaState) {
        Log.d(TAG, "ROM OTA event: state=${event.state}, value=${event.value}, msg=${event.message}")
        when (event.state) {
            1 -> { // download progress 1~100
                _updateState.value = UpdateState.Downloading(event.value.coerceIn(0, 100), isCancellable = true)
            }
            2 -> { // download complete
                _updateState.value = UpdateState.Installing
                updateDetails.firstOrNull()?.let { update ->
                    remoteMeterControlRepository.saveRecentlyCompletedUpdateId(update.id)
                    writeUpdateResultToFireStore(update.copy(
                        completedOn = Timestamp.now(),
                        status = UpdateStatus.COMPLETE
                    ))
                }
                remoteMeterControlRepository.updateBoardShutdownMinsDelayAfterAcc(MeasureBoardUtils.DEFAULT_MEASURE_BOARD_ACC_OFF_DELAY_MINS) // set back to 15 mins delay
            }
            3 -> { // start install
                _updateState.value = UpdateState.Installing
            }
            4 -> { // user paused install
                _updateState.value = UpdateState.Error(event.message ?: "Install paused", allowRetry = true)
            }
            5 -> { // is newest
                _updateState.value = UpdateState.NoUpdateFound
                updateDetails.firstOrNull()?.let { update ->
                    remoteMeterControlRepository.saveRecentlyCompletedUpdateId(update.id)
                    writeUpdateResultToFireStore(update.copy(
                        completedOn = Timestamp.now(),
                        status = UpdateStatus.VERSION_ERROR
                    ))
                }
            }
            6 -> { // specified device not include self
                _updateState.value = UpdateState.NoUpdateFound
                updateDetails.firstOrNull()?.let { update ->
                    remoteMeterControlRepository.saveRecentlyCompletedUpdateId(update.id)
                    writeUpdateResultToFireStore(update.copy(
                        completedOn = Timestamp.now(),
                        status = UpdateStatus.NOT_IN_WHITELIST
                    ))
                }
            }
            else -> {
                // unknown state, keep last but log
                updateDetails.firstOrNull()?.let { update ->
                    remoteMeterControlRepository.saveRecentlyCompletedUpdateId(update.id)
                    writeUpdateResultToFireStore(update.copy(
                        completedOn = Timestamp.now(),
                        status = UpdateStatus.UNKNOWN_ERROR
                    ))
                }
                Log.w(TAG, "Unknown ROM OTA state: ${event.state}")
            }
        }
    }

    private fun checkForUpdates() {
        viewModelScope.launch(ioDispatcher) {
            val update = remoteMeterControlRepository.remoteUpdateRequest.firstOrNull()
            val wifiCredential = remoteMeterControlRepository.meterSdkConfiguration.firstOrNull()?.wifiCredential
            if (update != null && (update.type == Constant.OTA_FIRMWARE_TYPE || update.type == Constant.OTA_METERAPP_TYPE)) {
                try {
                    val downloadUri = update.url?.let {
                        FirebaseStorage.getInstance()
                            .getReferenceFromUrl(it).downloadUrl.await()
                    }

                    attemptWifiConnectionAndDownload(wifiCredential, downloadUri!!, update)

                } catch (e: Exception) {
                    _updateState.value =
                        UpdateState.Error(e.message ?: "Could not get download URL")
                }

            }  else if (update != null && update.type == Constant.OTA_ANDROID_ROM_TYPE) {
                androidROMOTAUpdateManager.attemptROMUpdate()
                writeUpdateResultToFireStore(update.copy(
                    status = UpdateStatus.DOWNLOADING
                ))
                _updateState.value = UpdateState.Downloading(0, isCancellable = true)
                remoteMeterControlRepository.updateBoardShutdownMinsDelayAfterAcc(60) // 60 mins delay if ROM update has started
            } else {
                _updateState.value = UpdateState.NoUpdateFound
            }
        }
    }

    private fun attemptWifiConnectionAndDownload(
        wifiCredential: WifiCredential?,
        downloadUri: Uri,
        update: Update
    ) {
        val ssid = wifiCredential?.ssid
        val password = wifiCredential?.password

        if (ssid.isNullOrBlank() || password.isNullOrBlank()) {
            Log.d(TAG, "No Wi-Fi credentials provided. Downloading on default network.")
            // Ensure we're not bound to any specific network
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            connectivityManager.bindProcessToNetwork(null)
            downloadFile(downloadUri, update, null) // Proceed with default network
            return
        }

        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        var networkCallback: ConnectivityManager.NetworkCallback? = null
        var downloadJob: Job? = null
        val isDownloadStarted = AtomicBoolean(false)
        var timeoutJob: Job? = null

        viewModelScope.launch(ioDispatcher) {
            // First, check if we're already connected to the target Wi-Fi
            val currentNetwork = getCurrentWifiNetwork(ssid)
            if (currentNetwork != null) {
                Log.d(TAG, "Already connected to target Wi-Fi: $ssid")
                val capabilities = connectivityManager.getNetworkCapabilities(currentNetwork)
                if (capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true) {
                    Log.d(TAG, "Current Wi-Fi has validated internet. Starting download immediately.")
                    if (isDownloadStarted.compareAndSet(false, true)) {
                        connectivityManager.bindProcessToNetwork(currentNetwork)
                        downloadJob = launch { 
                            downloadFile(downloadUri, update, currentNetwork) 
                        }
                        return@launch
                    }
                }
            }
            
            // Attempt to connect using legacy method
            Log.d(TAG, "Attempting to connect to Wi-Fi: $ssid")
            connectToWifiLegacy(context, ssid, password)
            
            // Give some time for the Wi-Fi connection to establish
            delay(3000)

            val networkRequest = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            downloadJob?.cancel()
            networkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    super.onAvailable(network)
                    Log.d(TAG, "Network available: $network")
                    // Just log that network is available, don't start download yet
                    // Wait for onCapabilitiesChanged to confirm internet access
                }

                override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                    super.onCapabilitiesChanged(network, networkCapabilities)
                    Log.d(TAG, "Network capabilities changed: $network")
                    Log.d(TAG, "Capabilities - Wi-Fi: ${networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)}")
                    Log.d(TAG, "Capabilities - Internet: ${networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)}")
                    Log.d(TAG, "Capabilities - Validated: ${networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)}")
                    Log.d(TAG, "Download already started: ${isDownloadStarted.get()}")
                    
                    // Only start download when we have a Wi-Fi network with validated internet access
                    if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) &&
                        networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                        networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) {
                        
                        if (isDownloadStarted.compareAndSet(false, true)) {
                            Log.d(TAG, "Wi-Fi network validated with internet access. Starting download.")
                            
                            // Cancel timeout since we're starting download
                            timeoutJob?.cancel()
                            
                            try {
                                connectivityManager.bindProcessToNetwork(network)
                                downloadJob = launch { 
                                    downloadFile(downloadUri, update, network) 
                                }
                                
                            } catch (e: Exception) {
                                Log.e(TAG, "Error binding to network: ${e.message}")
                                // Fallback to default network
                                isDownloadStarted.set(false)
                                if (isDownloadStarted.compareAndSet(false, true)) {
                                    // Ensure we unbind from any specific network
                                    connectivityManager.bindProcessToNetwork(null)
                                    Log.d(TAG, "Network binding error fallback: using default network routing")
                                    
                                    downloadJob = launch { 
                                        downloadFile(downloadUri, update, null) 
                                    }
                                }
                            }
                        }
                    }
                }

                override fun onLost(network: Network) {
                    super.onLost(network)
                    Log.d(TAG, "Network lost: $network")
                }

                override fun onUnavailable() {
                    super.onUnavailable()
                    Log.w(TAG, "Network unavailable")
                }
            }

            try {
                connectivityManager.requestNetwork(networkRequest, networkCallback!!)
                
                // Check immediately after registering if we already have a suitable network
                launch {
                    delay(1000) // Give the callback a moment to register
                    if (!isDownloadStarted.get()) {
                        val currentWifiNetwork = getCurrentWifiNetwork(ssid)
                        if (currentWifiNetwork != null) {
                            val capabilities = connectivityManager.getNetworkCapabilities(currentWifiNetwork)
                            if (capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true) {
                                Log.d(TAG, "Found validated Wi-Fi network immediately after callback registration")
                                if (isDownloadStarted.compareAndSet(false, true)) {
                                    timeoutJob?.cancel()
                                    connectivityManager.bindProcessToNetwork(currentWifiNetwork)
                                    downloadJob = launch { 
                                        downloadFile(downloadUri, update, currentWifiNetwork) 
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Start timeout job
                timeoutJob = launch {
                    delay(10_000) // 10-second timeout
                    
                    // If timeout is reached and download hasn't started, fallback to default network
                    if (isDownloadStarted.compareAndSet(false, true)) {
                        Log.w(TAG, "Wi-Fi connection timed out. Downloading on default network.")
                        
                        // Clean up network callback
                        try {
                            networkCallback?.let { connectivityManager.unregisterNetworkCallback(it) }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error unregistering network callback during timeout: ${e.message}")
                        }
                        
                        // Ensure we unbind from any specific network to use default routing
                        connectivityManager.bindProcessToNetwork(null)
                        Log.d(TAG, "Unbound from specific network, using default network routing")
                        
                        downloadJob = launch { 
                            downloadFile(downloadUri, update, null) 
                        }
                    }
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error requesting network: ${e.message}")
                timeoutJob?.cancel()
                
                if (isDownloadStarted.compareAndSet(false, true)) {
                    // Ensure we're not bound to any specific network
                    connectivityManager.bindProcessToNetwork(null)
                    Log.d(TAG, "Error fallback: using default network routing")
                    
                    downloadJob = launch { 
                        downloadFile(downloadUri, update, null) 
                    }
                }
            }
        }
    }

    fun retryDownload() {
        val networkStatus = getCurrentNetworkStatus()
        Log.d(TAG, "Retrying download... Current network status: $networkStatus")
        
        // Reset state
        _updateState.value = UpdateState.Idle
        
        // Clean up any existing network callbacks and jobs
        cleanupNetworkResources()
        
        checkForUpdates()
    }
    
    private fun cleanupNetworkResources() {
        try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            
            // Reset network binding to allow default routing
            connectivityManager.bindProcessToNetwork(null)
            
            Log.d(TAG, "Network resources cleaned up - unbound from specific network, using default routing")
            
            // Log current network status after cleanup
            val networkStatus = getCurrentNetworkStatus()
            Log.d(TAG, "Network status after cleanup: $networkStatus")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up network resources: ${e.message}")
        }
    }

    private fun getCurrentWifiNetwork(targetSsid: String): Network? {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            
            // Check if currently connected to target SSID
            val currentWifiInfo = wifiManager.connectionInfo
            Log.d(TAG, "Current Wi-Fi SSID: ${currentWifiInfo?.ssid}, Target: \"$targetSsid\"")
            
            if (currentWifiInfo?.ssid == "\"$targetSsid\"") {
                // Get the current active network
                val activeNetwork = connectivityManager.activeNetwork
                val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
                
                Log.d(TAG, "Active network: $activeNetwork")
                Log.d(TAG, "Is Wi-Fi: ${capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)}")
                Log.d(TAG, "Has Internet: ${capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)}")
                Log.d(TAG, "Is Validated: ${capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)}")
                
                if (capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true) {
                    return activeNetwork
                }
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error checking current Wi-Fi network: ${e.message}")
            null
        }
    }

    private fun getCurrentNetworkStatus(): String {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetwork = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
            
            when {
                capabilities == null -> "No active network"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                    val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    val isValidated = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                    "Wi-Fi (Internet: $hasInternet, Validated: $isValidated)"
                }
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                    val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    val isValidated = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                    "Cellular/Mobile Data (Internet: $hasInternet, Validated: $isValidated)"
                }
                else -> "Other network"
            }
        } catch (e: Exception) {
            "Error checking network: ${e.message}"
        }
    }

    private fun downloadFile(uri: Uri, update: Update, network: Network?) {
        viewModelScope.launch(ioDispatcher) {
            Log.d(TAG, "Starting download with network: $network")
            val currentNetworkStatus = getCurrentNetworkStatus()
            Log.d(TAG, "Current network status at download start: $currentNetworkStatus")
            
            delay(3_000L) // Delay to ensure Wi-Fi is connected
            _updateState.value = UpdateState.Downloading(0)
            writeUpdateResultToFireStore(update.copy(status = UpdateStatus.DOWNLOADING))

            val client = if (network != null) {
                OkHttpClient.Builder().socketFactory(network.socketFactory).build()
            } else {
                OkHttpClient()
            }

            try {
                val fileName = update.url?.substringAfterLast("/")?.ifEmpty { "update.apk" }
                val targetFile = File(context.getExternalFilesDir(null), fileName!!)
                Log.d(TAG, "Target file path: ${targetFile.absolutePath}")

                if (targetFile.exists() && !targetFile.delete()) {
                    Log.e(TAG, "Failed to delete existing APK.")
                    _updateState.value = UpdateState.Error("Failed to delete existing APK.")
                    return@launch
                }

                val request = Request.Builder().url(uri.toString()).build()
                val response = client.newCall(request).execute()

                if (!response.isSuccessful) throw IOException("Download failed: ${response.code}")

                val body = response.body ?: throw IOException("Response body is null")
                val totalSize = body.contentLength()

                body.source().use { source ->
                    targetFile.outputStream().use { output ->
                        val buffer = ByteArray(8192)
                        var downloadedSize = 0L
                        var bytesRead: Int
                        var progress = 0

                        while (source.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            downloadedSize += bytesRead

                            if (totalSize > 0) {
                                val newProgress = ((downloadedSize * 100) / totalSize).toInt()
                                if (newProgress != progress) {
                                    progress = newProgress
                                    _updateState.value = UpdateState.Downloading(progress)
                                }
                            }
                        }
                    }
                }

                _updateState.value = UpdateState.Installing
                writeUpdateResultToFireStore(update.copy(status = UpdateStatus.INSTALLING))

                if (update.type == Constant.OTA_METERAPP_TYPE) {
                    remoteMeterControlRepository.saveRecentlyCompletedUpdateId(
                        updateDetails.firstOrNull()?.id ?: ""
                    )
                    installApk(targetFile)
                } else if (update.type == Constant.OTA_FIRMWARE_TYPE) {
                    remoteMeterControlRepository.requestPatchFirmwareToMCU(targetFile.absolutePath)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Download error: ", e)
                _updateState.value =
                    UpdateState.Error(e.message ?: "Unknown error", allowRetry = true)
                writeUpdateResultToFireStore(update.copy(status = UpdateStatus.DOWNLOAD_ERROR))
            } finally {
                cleanupNetworkResources()
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
                        status = UpdateStatus.COMPLETE
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

    private suspend fun connectToWifiLegacy(context: Context, ssid: String?, password: String?) {
        try {
            if (ssid == null || password == null) {
                Log.e("WiFiConnection", "SSID or password is null")
                return
            }

            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

            // Check if WiFi is enabled
            if (!wifiManager.isWifiEnabled) {
                Log.d("WiFiConnection", "Enabling WiFi...")
                wifiManager.isWifiEnabled = true
                
                // Wait for WiFi to be enabled
                var retries = 0
                while (!wifiManager.isWifiEnabled && retries < 10) {
                    delay(1000)
                    retries++
                }
                
                if (!wifiManager.isWifiEnabled) {
                    Log.e("WiFiConnection", "Failed to enable WiFi after 10 seconds")
                    return
                }
            }

            // Check if we're already connected to the target network
            val currentNetwork = wifiManager.connectionInfo
            if (currentNetwork != null && currentNetwork.ssid == "\"$ssid\"") {
                Log.d("WiFiConnection", "Already connected to $ssid")
                return
            }

            // Remove any existing configurations for this SSID
            val configuredNetworks = wifiManager.configuredNetworks ?: emptyList()
            configuredNetworks.forEach { config ->
                if (config.SSID == "\"$ssid\"") {
                    wifiManager.removeNetwork(config.networkId)
                    Log.d("WiFiConnection", "Removed existing configuration for $ssid")
                }
            }

            // Create a WiFi configuration
            val wifiConfig = WifiConfiguration().apply {
                SSID = "\"$ssid\""
                preSharedKey = "\"$password\""
                allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK)
                allowedProtocols.set(WifiConfiguration.Protocol.RSN)
                allowedProtocols.set(WifiConfiguration.Protocol.WPA)
                allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP)
                allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP)
                allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40)
                allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104)
                allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP)
                allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP)
            }

            // Add the WiFi configuration to the system
            val networkId = wifiManager.addNetwork(wifiConfig)

            if (networkId != -1) {
                Log.d("WiFiConnection", "WiFi configuration added for $ssid")
                
                // Disconnect from the current network
                wifiManager.disconnect()
                delay(1000)

                // Enable and connect to the new network
                wifiManager.enableNetwork(networkId, true)
                wifiManager.reconnect()

                Log.d("WiFiConnection", "Attempting to connect to $ssid")
                
                // Wait for connection
                var connectionRetries = 0
                while (connectionRetries < 15) { // 15 seconds timeout
                    delay(1000)
                    val info = wifiManager.connectionInfo
                    if (info != null && info.ssid == "\"$ssid\"" && info.networkId != -1) {
                        Log.d("WiFiConnection", "Successfully connected to $ssid")
                        return
                    }
                    connectionRetries++
                }
                
                Log.w("WiFiConnection", "Connection to $ssid timed out")
            } else {
                Log.e("WiFiConnection", "Failed to add network configuration for $ssid")
            }
        } catch (e: Exception) {
            Log.e("WiFiConnection", "Error connecting to WiFi: ${e.message}", e)
        }
    }

    fun skipAndroidROMOta() {
        androidROMOTAUpdateManager.terminateOngoingROMUpdate()
        remoteMeterControlRepository.updateBoardShutdownMinsDelayAfterAcc(MeasureBoardUtils.DEFAULT_MEASURE_BOARD_ACC_OFF_DELAY_MINS) // default - 15 mins
    }

    private suspend fun writeUpdateResultToFireStore(update: Update) {
        remoteMeterControlRepository.writeUpdateResultToFireStore(update).await()
    }

    companion object {
        private const val TAG = "UpdateViewModel"
    }
}

sealed class UpdateState {
    data object Idle : UpdateState()
    data object NoUpdateFound : UpdateState()
    data class Downloading(val progress: Int, val isCancellable: Boolean = false) : UpdateState()
    data object Installing : UpdateState()
    data object Success : UpdateState()
    data class Error(val message: String, val allowRetry: Boolean = true) : UpdateState()
}