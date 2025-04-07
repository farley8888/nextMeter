package com.vismo.nextgenmeter

import android.content.Context
import android.os.PowerManager
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.crashlytics.CustomKeysAndValues
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.ilin.util.AmapLocationUtils
import com.ilin.util.ShellUtils
import com.vismo.nextgenmeter.datastore.DeviceDataStore
import com.vismo.nextgenmeter.datastore.TOGGLE_COMMS_WITH_MCU
import com.vismo.nextgenmeter.datastore.TripDataStore
import com.vismo.nextgenmeter.ui.topbar.TopAppBarUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import com.vismo.nextgenmeter.module.IoDispatcher
import com.vismo.nextgenmeter.module.MainDispatcher
import com.vismo.nextgenmeter.repository.DriverPreferenceRepository
import com.vismo.nextgenmeter.repository.FirebaseAuthRepository
import com.vismo.nextgenmeter.repository.FirebaseAuthRepository.Companion.AUTHORIZATION_HEADER
import com.vismo.nextgenmeter.repository.InternetConnectivityObserver
import com.vismo.nextgenmeter.repository.MeasureBoardRepository
import com.vismo.nextgenmeter.repository.MeterPreferenceRepository
import com.vismo.nextgenmeter.repository.NetworkTimeRepository
import com.vismo.nextgenmeter.repository.PeripheralControlRepository
import com.vismo.nextgenmeter.repository.RemoteMeterControlRepository
import com.vismo.nextgenmeter.repository.TripFileManager
import com.vismo.nextgenmeter.repository.TripRepository
import com.vismo.nextgenmeter.service.DeviceGodCodeUnlockState
import com.vismo.nextgenmeter.service.StorageBroadcastReceiver
import com.vismo.nextgenmeter.service.StorageReceiverStatus
import com.vismo.nextgenmeter.service.USBReceiverStatus
import com.vismo.nextgenmeter.ui.shared.SnackbarState
import com.vismo.nextgenmeter.ui.theme.gold600
import com.vismo.nextgenmeter.ui.theme.nobel800
import com.vismo.nextgenmeter.ui.theme.pastelGreen600
import com.vismo.nextgenmeter.ui.theme.primary700
import com.vismo.nextgenmeter.util.Constant
import com.vismo.nextgenmeter.util.GlobalUtils.maskLast
import com.vismo.nxgnfirebasemodule.DashManager
import com.vismo.nxgnfirebasemodule.DashManagerConfig
import com.vismo.nxgnfirebasemodule.model.GPS
import com.vismo.nxgnfirebasemodule.model.MeterLocation
import com.vismo.nxgnfirebasemodule.model.NOT_SET
import com.vismo.nxgnfirebasemodule.model.TripPaidStatus
import com.vismo.nxgnfirebasemodule.model.Update
import com.vismo.nxgnfirebasemodule.model.UpdateStatus
import com.vismo.nxgnfirebasemodule.model.snoozeForADay
import com.vismo.nxgnfirebasemodule.util.Constant.OTA_FIRMWARE_TYPE
import com.vismo.nxgnfirebasemodule.util.Constant.OTA_METERAPP_TYPE
import dagger.hilt.android.qualifiers.ApplicationContext
import io.sentry.IScope
import io.sentry.Sentry
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val measureBoardRepository: MeasureBoardRepository,
    private val peripheralControlRepository: PeripheralControlRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @MainDispatcher private val mainDispatcher: CoroutineDispatcher,
    private val remoteMeterControlRepository: RemoteMeterControlRepository,
    private val dashManagerConfig: DashManagerConfig,
    private val tripRepository: TripRepository,
    @ApplicationContext private val context: Context,
    private val firebaseAuthRepository: FirebaseAuthRepository,
    private val driverPreferenceRepository: DriverPreferenceRepository,
    private val internetConnectivityObserver: InternetConnectivityObserver,
    private val networkTimeRepository: NetworkTimeRepository,
    private val meterPreferenceRepository: MeterPreferenceRepository,
    private val crashlytics: FirebaseCrashlytics,
    tripFileManager: TripFileManager
    ) : ViewModel(){
    private val _topAppBarUiState = MutableStateFlow(TopAppBarUiState())
    val topAppBarUiState: StateFlow<TopAppBarUiState> = _topAppBarUiState

    private val _showLoginToggle = MutableStateFlow<Boolean?>(null)
    val showLoginToggle: StateFlow<Boolean?> = _showLoginToggle

    private val _showConnectionIconsToggle = MutableStateFlow<Boolean?>(null)
    val showConnectionIconsToggle: StateFlow<Boolean?> = _showConnectionIconsToggle

    private val dateFormat = SimpleDateFormat(TOOLBAR_UI_DATE_FORMAT, Locale.TRADITIONAL_CHINESE)

    private val toolbarUiDataUpdateMutex = Mutex()

    private var accEnquiryJob: Job? = null
    private var sleepJob: Job? = null
    private val _isScreenOff = MutableStateFlow(false)

    private val _snackBarContent = MutableStateFlow<Pair<String, SnackbarState>?>(null)
    val snackBarContent: StateFlow<Pair<String, SnackbarState>?> = _snackBarContent

    private val _clearApplicationCache = MutableStateFlow(false)
    val clearApplicationCache: StateFlow<Boolean> = _clearApplicationCache

    val aValidUpdate = remoteMeterControlRepository.remoteUpdateRequest
        .onEach { Log.d(TAG, "aValidUpdateFlow Debug - $it") }
        .filter {
            when (it?.type) {
            OTA_METERAPP_TYPE -> {
                val isValid = isBuildVersionHigherThanCurrentVersion(it.version)
                val newStatus = if (isValid) UpdateStatus.WAITING_FOR_DOWNLOAD else UpdateStatus.VERSION_ERROR
                remoteMeterControlRepository.writeUpdateResultToFireStore(it.copy(status = newStatus))
                isValid
            }
            OTA_FIRMWARE_TYPE -> {
                val isValid = it.version != remoteMeterControlRepository.meterInfo.firstOrNull()?.mcuInfo?.firmwareVersion
                val newStatus = if (isValid) UpdateStatus.WAITING_FOR_DOWNLOAD else UpdateStatus.COMPLETE
                remoteMeterControlRepository.writeUpdateResultToFireStore(
                    it.copy(
                        status = newStatus,
                        completedOn = if (newStatus == UpdateStatus.COMPLETE) Timestamp.now() else null
                    )
                )
                isValid
            }
            else -> false
        }
        }
        .stateIn(viewModelScope, started = SharingStarted.Eagerly, initialValue = null)

    private var isMCUTimeSet = false
    private var heartbeatJob: Job? = null
    private var busModelJob: Job? = null

    private fun isBuildVersionHigherThanCurrentVersion(version: String): Boolean {
        val currentVersionCode = "${BuildConfig.VERSION_NAME}.${BuildConfig.VERSION_CODE}" // Assuming current version is in the form 6.5.3.1034

        // Convert both version codes to integer arrays for comparison
        val currentVersionParts = currentVersionCode.split(".").map { it.toInt() }
        val newVersionParts = version.split(".").map { it.toInt() }

        // Compare version parts
        for (i in 0 until minOf(currentVersionParts.size, newVersionParts.size)) {
            if (newVersionParts[i] > currentVersionParts[i]) {
                return true // New version is higher
            } else if (newVersionParts[i] < currentVersionParts[i]) {
                return false // Current version is higher
            }
        }

        // If the parts are equal, compare by length (i.e., higher version may have more sub-version numbers)
        return newVersionParts.size > currentVersionParts.size
    }

    private fun observeFlows() {
        viewModelScope.launch(ioDispatcher) {
            launch { observeMCUTIme() }
            launch { observeHeartBeatInterval() }
            launch { observeMeterAndTripInfo() }
            launch { observeTripData() }
            launch { observeFirebaseAuthSuccess() }
            launch { observeShowLoginToggle() }
            launch { observeShowConnectionIconsToggle() }
            launch { observeStorageReceiverStatus() }
            launch { observeClearCacheOfApplication() }
            launch { observeReInitMCURepository() }
            launch { observeMCUHeartbeatSignal() }
            launch { observeBusModelSignal() }
            launch { observeMeterLocation() }
        }
    }

    private suspend fun observeMeterLocation() {
        dashManagerConfig.meterLocation.collectLatest {
            updateLocationIconVisibility(isVisible = it.gpsType != NOT_SET)
            when (it.gpsType) {
                is GPS -> {
                    AmapLocationUtils.getInstance().stopLocation()
                }
                NOT_SET -> {
                    AmapLocationUtils.getInstance().startLocation()
                }
                else -> {}
            }
        }
    }

    private fun resetHeartbeatUITimeout() {
        // Cancel any existing timer job
        heartbeatJob?.cancel()

        // Start a new timer job
        heartbeatJob = viewModelScope.launch {
            delay(3000)
            DeviceDataStore.setMCUHeartbeatActive(false)
        }
    }

    private fun resetBusModelUITimeout() {
        // Cancel any existing timer job
        busModelJob?.cancel()

        // Start a new timer job
        busModelJob = viewModelScope.launch {
            delay(3000)
            DeviceDataStore.setBusModelListenerDataReceived(false)
        }
    }

    private suspend fun observeReInitMCURepository() {
        DeviceDataStore.toggleCommunicationWithMCU.collectLatest { toggle ->
            when(toggle) {
                TOGGLE_COMMS_WITH_MCU.TOGGLE_OFF -> {
                    measureBoardRepository.stopCommunication()
                    withContext(mainDispatcher) {
                        Toast.makeText(context, "Communication stopped", Toast.LENGTH_SHORT).show()
                    }
                }
                TOGGLE_COMMS_WITH_MCU.TOGGLE_ON -> {
                    measureBoardRepository.startCommunicate()
                    withContext(mainDispatcher) {
                        Toast.makeText(context, "Communication started", Toast.LENGTH_SHORT).show()
                    }
                }
                else -> {}
            }
        }
    }

    private suspend fun observeClearCacheOfApplication() {
        DeviceDataStore.clearCacheOfApplication.collectLatest { clearCache ->
            if (clearCache) {
                _clearApplicationCache.value = true
                Sentry.captureMessage("Clearing cache of application")
                DeviceDataStore.setClearCacheOfApplication(false)
            }
        }
    }

    private fun observeStorageReceiverStatus() {
        viewModelScope.launch {
            launch {
                DeviceDataStore.storageReceiverStatus.collectLatest { status ->
                    Log.d(TAG, "StorageReceiverStatus: $status")
                    when(status) {
                        StorageReceiverStatus.Mounted -> onSdCardMounted()
                        StorageReceiverStatus.Unmounted -> onSdCardUnmounted()
                        else -> {}
                    }
                }
            }

            launch {
                DeviceDataStore.usbReceiverStatus.collectLatest { status ->
                    Log.d(TAG, "USBReceiverStatus: $status")
                    when (status) {
                        USBReceiverStatus.Attached -> onUsbConnected()
                        USBReceiverStatus.Detached -> onUsbDisconnected()
                        else -> {}
                    }
                }
            }
        }
    }

    fun setClearApplicationCache(boolean: Boolean) {
        _clearApplicationCache.value = boolean
    }

    fun snoozeUpdate(update: Update?) {
        viewModelScope.launch(ioDispatcher) {
            update?.let {
                val snoozedUpdate = it.snoozeForADay()
                remoteMeterControlRepository.writeUpdateResultToFireStore(snoozedUpdate)
            }
        }
    }

    private suspend fun observeShowLoginToggle() {
        meterPreferenceRepository.getShowLoginToggle().collectLatest { showLoginToggle ->
            _showLoginToggle.value = showLoginToggle
        }
    }

    private suspend fun observeShowConnectionIconsToggle() {
        meterPreferenceRepository.getShowConnectionIconsToggle().collectLatest { showConnectionIconsToggle ->
            _showConnectionIconsToggle.value = showConnectionIconsToggle
        }
    }

    private fun observeInternetStatus() {
        viewModelScope.launch {
            Log.d(TAG, "Internet status observer started")
            internetConnectivityObserver.internetStatus.collectLatest { status ->
                Log.d(TAG, "Internet status: $status")
                when (status) {
                    InternetConnectivityObserver.Status.InternetAvailable -> {
                        tryInternetTasks()
                        Log.d(TAG, "Internet available")
                    }

                    InternetConnectivityObserver.Status.InternetUnavailable -> {
                        Log.d(TAG, "Internet unavailable")
                    }
                }
            }
        }
    }

    private suspend fun tryInternetTasks() {
        Log.d(TAG, "Trying internet tasks")
        val headers = firebaseAuthRepository.getHeaders()
        if (!headers.containsKey(AUTHORIZATION_HEADER)) {
            firebaseAuthRepository.initToken(viewModelScope)
            Log.d(TAG, "FirebaseAuth initToken called")
        } else if (!DashManager.Companion.isInitialized) {
            remoteMeterControlRepository.initDashManager(viewModelScope)
            Log.d(TAG, "FirebaseAuth headers already present - calling dashManager.init()")
        }
        networkTimeRepository.fetchNetworkTime()?.let { networkTime ->
            if (!isMCUTimeSet) {
                measureBoardRepository.updateMeasureBoardTime(networkTime)
                isMCUTimeSet = true
                Log.d(TAG, "Network time set: $networkTime")
            }
        }
    }

    private suspend fun observeFirebaseAuthSuccess() {
        firebaseAuthRepository.isFirebaseAuthSuccess.collectLatest { isFirebaseAuthSuccess ->
            if (isFirebaseAuthSuccess && !DashManager.isInitialized) {
                remoteMeterControlRepository.initDashManager(viewModelScope)
                Log.d(TAG, "Firebase auth success - calling dashManager.init()")
            }
        }
    }

    private fun observeDriverInfo() {
        viewModelScope.launch {
            driverPreferenceRepository.getDriver().collectLatest { driver ->
                Log.d(TAG, "Driver: ${driver.driverPhoneNumber}")
                toolbarUiDataUpdateMutex.withLock {
                    _topAppBarUiState.value = _topAppBarUiState.value.copy(
                        driverPhoneNumber = driver.driverPhoneNumber.maskLast(4)
                    )
                }
            }
        }
    }

    private fun disableADBByDefaultForProd() {
        if (BuildConfig.FLAVOR == Constant.ENV_PRD) {
            disableADB()
        }
    }

    private suspend fun observeTripData() {
        TripDataStore.ongoingTripData.collectLatest {
            TripDataStore.setIsTripInProgress(it != null)
        }
    }

    private suspend fun observeBusModelSignal() {
        DeviceDataStore.isMCUHeartbeatActive.collectLatest { isMCUHeartbeatActive ->
            toolbarUiDataUpdateMutex.withLock {
                _topAppBarUiState.value = _topAppBarUiState.value.copy(
                    showMCUHeartbeatIncomingSignal = isMCUHeartbeatActive
                )
            }
            resetBusModelUITimeout()
        }
    }

    private suspend fun observeMCUHeartbeatSignal() {
        var isHeartbeatActive: Boolean
        val debounceDelay = 5000L // 5 seconds

        DeviceDataStore.isBusModelListenerDataReceived.collectLatest { isBusModelListenerDataReceived ->
            isHeartbeatActive = isBusModelListenerDataReceived
            toolbarUiDataUpdateMutex.withLock {
                _topAppBarUiState.value = _topAppBarUiState.value.copy(
                    showBusModelSignal = isBusModelListenerDataReceived
                )
            }
            resetHeartbeatUITimeout()

            if (!isBusModelListenerDataReceived) {
                delay(debounceDelay)
                // Recheck if it's still inactive after the delay
                if (!isHeartbeatActive) {
                    startCommunicate()
                    Sentry.captureException(Throwable(message = TAG_RESTARTING_MCU_COMMUNICATION))
                    withContext(mainDispatcher) {
                        Toast.makeText(context, TAG_RESTARTING_MCU_COMMUNICATION, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private suspend fun observeMeterAndTripInfo() {
        combine(
            remoteMeterControlRepository.meterInfo,
            tripRepository.currentTripPaidStatus
        ) { meterInfo, tripPaidStatus ->
            Pair(meterInfo, tripPaidStatus)
        }.collectLatest { (meterInfo, tripPaidStatus) ->
            meterInfo?.let {
                if (it.settings?.showLoginToggle != null && meterPreferenceRepository.getShowLoginToggle().first() != it.settings.showLoginToggle) {
                    meterPreferenceRepository.saveShowLoginToggle(it.settings.showLoginToggle)
                }

                if (it.settings?.showConnectionIconsToggle != null && meterPreferenceRepository.getShowConnectionIconsToggle().first() != it.settings.showConnectionIconsToggle) {
                    meterPreferenceRepository.saveShowConnectionIconsToggle(it.settings.showConnectionIconsToggle)
                }

                toolbarUiDataUpdateMutex.withLock {
                    _topAppBarUiState.value = _topAppBarUiState.value.copy(
                        showLoginToggle = it.settings?.showLoginToggle ?: false,
                        showConnectionIconsToggle = it.settings?.showConnectionIconsToggle ?: true
                    )
                }
                if (driverPreferenceRepository.getDriverOnce().driverPhoneNumber != it.session?.driver?.driverPhoneNumber) {
                    if (it.session?.driver?.driverPhoneNumber != null) {
                        driverPreferenceRepository.saveDriver(it.session.driver)
                        Sentry.configureScope { scope: IScope ->
                            scope.setTag("driver_phone_number", it.session.driver.driverPhoneNumber )
                        }
                    } else {
                        driverPreferenceRepository.resetDriver()
                        Sentry.configureScope { scope: IScope ->
                            scope.removeTag("driver_phone_number")
                        }
                    }
                }
            }

            manageDashPayColor(tripPaidStatus, meterInfo?.session != null)
        }
    }

    private suspend fun manageDashPayColor(tripPaidStatus: TripPaidStatus, isSessionExists: Boolean) {
        val toolbarColor = when (tripPaidStatus) {
            TripPaidStatus.NOT_PAID -> if (isSessionExists) primary700 else nobel800
            TripPaidStatus.COMPLETELY_PAID -> pastelGreen600
            TripPaidStatus.PARTIALLY_PAID -> gold600
        }
        toolbarUiDataUpdateMutex.withLock {
            _topAppBarUiState.value = _topAppBarUiState.value.copy(
                color = toolbarColor
            )
        }
    }

    private suspend fun observeHeartBeatInterval() {
        remoteMeterControlRepository.heartBeatInterval.collectLatest { interval ->
            if (interval > 0) {
                while (true) {
                    remoteMeterControlRepository.sendHeartBeat()
                    delay(interval* 1000L)
                }
            }
        }
    }

    private suspend fun observeMCUTIme() {
        DeviceDataStore.mcuTime.collectLatest {
            it?.let { dateTime ->
                try {
                    val formatter = SimpleDateFormat(MCU_DATE_FORMAT, Locale.ENGLISH)
                    val date = formatter.parse(dateTime)
                    date?.let {
                        val formattedDate = dateFormat.format(date)
                        toolbarUiDataUpdateMutex.withLock {
                            _topAppBarUiState.value = _topAppBarUiState.value.copy(
                                dateTime = formattedDate
                            )
                        }
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "Error parsing date: $dateTime")
                }
            }
        }
    }

    fun updateBackButtonVisibility(isVisible: Boolean) {
        viewModelScope.launch {
            toolbarUiDataUpdateMutex.withLock {
                _topAppBarUiState.value = _topAppBarUiState.value.copy(
                    isBackButtonVisible = isVisible
                )
            }
        }
    }

    private fun updateLocationIconVisibility(isVisible: Boolean = true) {
        if (_topAppBarUiState.value.isLocationIconVisible != isVisible) {
            viewModelScope.launch {
                toolbarUiDataUpdateMutex.withLock {
                    _topAppBarUiState.value = _topAppBarUiState.value.copy(
                        isLocationIconVisible = isVisible
                    )
                }
            }
        }
    }

    fun updateSignalStrength(signalStrength: Int) {
        if (_topAppBarUiState.value.signalStrength == signalStrength) return
        viewModelScope.launch {
            toolbarUiDataUpdateMutex.withLock {
                _topAppBarUiState.value = _topAppBarUiState.value.copy(
                    signalStrength = signalStrength
                )
            }
        }
    }

    fun setWifiIconVisibility(isVisible: Boolean) {
        viewModelScope.launch {
            toolbarUiDataUpdateMutex.withLock {
                _topAppBarUiState.value = _topAppBarUiState.value.copy(
                    isWifiIconVisible = isVisible
                )
            }
        }
    }

    fun setLocation(meterLocation: MeterLocation) {
        dashManagerConfig.setLocation(meterLocation)
    }

    fun startCommunicate() {
        measureBoardRepository.startCommunicate()
    }

    fun stopCommunicate() {
        measureBoardRepository.stopCommunication()
    }


    init {
        measureBoardRepository.init(viewModelScope)
        remoteMeterControlRepository.observeFlows(viewModelScope)
        tripRepository.initObservers(viewModelScope)
        observeFlows()
        startACCStatusInquiries()
        observeInternetStatus()
        observeDriverInfo()
        disableADBByDefaultForProd()
        setCrashlyticsKeys()
        viewModelScope.launch(ioDispatcher) {
            tripFileManager.initializeTrips()
        }
        Log.d(TAG, "MainViewModel initialized")
    }

    private fun setCrashlyticsKeys() {
        viewModelScope.launch(ioDispatcher) {
            val savedDeviceId = meterPreferenceRepository.getDeviceId().firstOrNull() ?: ""
            val savedLicensePlate = meterPreferenceRepository.getLicensePlate().firstOrNull() ?: ""
            crashlytics.setUserId(savedLicensePlate)
            val keys = CustomKeysAndValues.Builder()
                .putString("device_id", savedDeviceId)
                .putString("license_plate", savedLicensePlate)
                .build()
            crashlytics.setCustomKeys(keys)
        }
    }

    private fun startACCStatusInquiries() {
        accEnquiryJob = viewModelScope.launch(ioDispatcher) {
            while (true) {
                inquireApplicationStatus()
                delay(INQUIRE_ACC_STATUS_INTERVAL)
            }
        }
    }

    private suspend fun inquireApplicationStatus() {
        val acc = ShellUtils.execShellCmd("cat /sys/class/gpio/gpio75/value")
        if (acc == ACC_SLEEP_STATUS) {
            sleepDevice()
        } else {
            wakeUpDevice()
        }
    }

    private suspend fun sleepDevice() {
        val isTripInProgress = TripDataStore.isTripInProgress.firstOrNull() ?: false
        if (isTripInProgress || _isScreenOff.value) return

        sleepJob = viewModelScope.launch(ioDispatcher) {
            delay(BACKLIGHT_OFF_DELAY)
            if (!isTripInProgress) {
                _isScreenOff.value = true
                toggleBackLight(false)
                switchToLowPowerMode()
                DeviceDataStore.setIsDeviceAsleep(isAsleep = true)
                dashManagerConfig.setIsDeviceAsleep(isAsleep = true)
                Log.d(TAG, "sleepDevice: Device is in sleep mode")
            }
        }
    }

    private fun wakeUpDevice() {
        sleepJob?.cancel()
        if (!_isScreenOff.value) return

        toggleBackLight(true)
        _isScreenOff.value = false
        DeviceDataStore.setIsDeviceAsleep(isAsleep = false)
        dashManagerConfig.setIsDeviceAsleep(isAsleep = false)
        Log.d(TAG, "wakeUpDevice: Device is in wake up mode")
    }

    private fun toggleBackLight(turnOn: Boolean) {
        viewModelScope.launch(ioDispatcher) {
            if (turnOn) {
                ShellUtils.execEcho("echo 255 > /sys/class/backlight/backlight/brightness")
                ShellUtils.execEcho("echo 1 > /sys/class/gpio/gpio70/value")
            } else {
                ShellUtils.execEcho("echo 0 > /sys/class/backlight/backlight/brightness")
                ShellUtils.execEcho("echo 0 > /sys/class/gpio/gpio70/value")
            }
        }
    }

    private fun switchToLowPowerMode() {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "$TAG::LowPowerModeWakelock")
        wakeLock.acquire(TURN_OFF_DEVICE_AFTER_BACKLIGHT_OFF_DELAY) // Acquire for 1 minute
        wakeLock.release()
    }

    private fun onUsbConnected() {
        _snackBarContent.value = Pair("USB設備已連接", SnackbarState.SUCCESS)
    }

    private fun onUsbDisconnected() {
        _snackBarContent.value = Pair("USB設備已斷開", SnackbarState.DEFAULT)
    }

    private fun onSdCardMounted() {
        readFromSDCard()

    }

    private fun readFromSDCard() {
        try {
            val sdCard = ContextCompat.getExternalFilesDirs(context, null).last().absolutePath
            val path = sdCard.split("/Android").get(0)
            val file = File(path, "usbadb.txt")
            val text = file.readText()
            onTextReceived(text)
        } catch (e: IOException) {
            // Handle exceptions
            Log.e("ReadFile", "Error reading file", e)
            _snackBarContent.value = Pair("Error reading file", SnackbarState.ERROR)
        }
    }

    private fun onTextReceived(text: String) {
        // Handle the text
        val trimmedText = text.trim()
        if (trimmedText == StorageBroadcastReceiver.STATIC_GOD_CODE) {
            enableADB()
            _snackBarContent.value = Pair("USB unlocked!", SnackbarState.SUCCESS)
            DeviceDataStore.setDeviceGodCodeUnlockState(DeviceGodCodeUnlockState.Unlocked)
        } else {
            // show error message
            _snackBarContent.value = Pair("USB locked!", SnackbarState.ERROR)
        }
    }

    private fun onSdCardUnmounted() {
        if(BuildConfig.FLAVOR == Constant.ENV_PRD) {
            disableADB()
        }

        _snackBarContent.value = Pair("SD card removed", SnackbarState.DEFAULT)
        DeviceDataStore.setDeviceGodCodeUnlockState(DeviceGodCodeUnlockState.Locked)
    }

    private fun enableADB() {
        viewModelScope.launch {
            ShellUtils.execShellCmd("setprop persist.service.adb.enable 1")
            Log.w(TAG, "ADB enabled")
        }
    }

    private fun disableADB() {
        viewModelScope.launch {
            ShellUtils.execShellCmd("setprop persist.service.adb.enable 0")
            Log.w(TAG, "ADB disabled")
        }
    }

    fun emitBeepSound() {
        measureBoardRepository.emitBeepSound(5, 0, 1)
    }

    fun resetSnackBarContent() {
        _snackBarContent.value = null
    }

    override fun onCleared() {
        super.onCleared()
        remoteMeterControlRepository.onCleared()
        peripheralControlRepository.close()
        accEnquiryJob?.cancel()
        busModelJob?.cancel()
        heartbeatJob?.cancel()
    }

    companion object {
        private const val TAG = "MainViewModel"
        private const val INQUIRE_ACC_STATUS_INTERVAL = 5000L // 5 seconds
        private const val BACKLIGHT_OFF_DELAY = 10_000L // 10 seconds
        private const val TURN_OFF_DEVICE_AFTER_BACKLIGHT_OFF_DELAY = 60_000L // 1 minute - standby mode
        private const val TOOLBAR_UI_DATE_FORMAT = "M月d日 HH:mm"
        private const val MCU_DATE_FORMAT = "yyyyMMddHHmm"
        private const val ACC_SLEEP_STATUS = "1"
        const val TAG_RESTARTING_MCU_COMMUNICATION = "Restarting MCU communication"
    }
}
