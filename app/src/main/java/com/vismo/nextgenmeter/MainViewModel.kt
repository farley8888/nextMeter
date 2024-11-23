package com.vismo.nextgenmeter

import android.content.Context
import android.os.PowerManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ilin.util.ShellUtils
import com.vismo.nextgenmeter.datastore.DeviceDataStore
import com.vismo.nextgenmeter.datastore.TripDataStore
import com.vismo.nextgenmeter.ui.topbar.TopAppBarUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import com.vismo.nextgenmeter.module.IoDispatcher
import com.vismo.nextgenmeter.repository.DriverPreferenceRepository
import com.vismo.nextgenmeter.repository.FirebaseAuthRepository
import com.vismo.nextgenmeter.repository.FirebaseAuthRepository.Companion.AUTHORIZATION_HEADER
import com.vismo.nextgenmeter.repository.InternetConnectivityObserver
import com.vismo.nextgenmeter.repository.MeasureBoardRepository
import com.vismo.nextgenmeter.repository.MeterPreferenceRepository
import com.vismo.nextgenmeter.repository.NetworkTimeRepository
import com.vismo.nextgenmeter.repository.PeripheralControlRepository
import com.vismo.nextgenmeter.repository.RemoteMeterControlRepository
import com.vismo.nextgenmeter.repository.TripRepository
import com.vismo.nextgenmeter.service.StorageReceiverStatus
import com.vismo.nextgenmeter.service.USBReceiverStatus
import com.vismo.nextgenmeter.ui.shared.SnackbarState
import com.vismo.nextgenmeter.ui.theme.gold600
import com.vismo.nextgenmeter.ui.theme.nobel600
import com.vismo.nextgenmeter.ui.theme.pastelGreen600
import com.vismo.nextgenmeter.ui.theme.primary700
import com.vismo.nextgenmeter.util.Constant
import com.vismo.nextgenmeter.util.GlobalUtils.maskLast
import com.vismo.nxgnfirebasemodule.DashManager
import com.vismo.nxgnfirebasemodule.DashManagerConfig
import com.vismo.nxgnfirebasemodule.model.MeterLocation
import com.vismo.nxgnfirebasemodule.model.TripPaidStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
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
    private val remoteMCUControlRepository: RemoteMeterControlRepository,
    private val dashManagerConfig: DashManagerConfig,
    private val tripRepository: TripRepository,
    @ApplicationContext private val context: Context,
    private val firebaseAuthRepository: FirebaseAuthRepository,
    private val driverPreferenceRepository: DriverPreferenceRepository,
    private val dashManager: DashManager,
    private val internetConnectivityObserver: InternetConnectivityObserver,
    private val networkTimeRepository: NetworkTimeRepository,
    private val meterPreferenceRepository: MeterPreferenceRepository
    ) : ViewModel(){
    private val _topAppBarUiState = MutableStateFlow(TopAppBarUiState())
    val topAppBarUiState: StateFlow<TopAppBarUiState> = _topAppBarUiState

    private val _showLoginToggle = MutableStateFlow<Boolean?>(null)
    val showLoginToggle: StateFlow<Boolean?> = _showLoginToggle

    private val _showConnectionIconsToggle = MutableStateFlow<Boolean?>(null)
    val showConnectionIconsToggle: StateFlow<Boolean?> = _showConnectionIconsToggle

    private val dateFormat = SimpleDateFormat(TOOLBAR_UI_DATE_FORMAT, Locale.TRADITIONAL_CHINESE)

    private val toolbarUiDataUpdateMutex = Mutex()

    private val _isTripInProgress = MutableStateFlow(false)
    val isTripInProgress: StateFlow<Boolean> = _isTripInProgress

    private var accEnquiryJob: Job? = null
    private var sleepJob: Job? = null
    private val _isScreenOff = MutableStateFlow(false)

    private val _snackBarContent = MutableStateFlow<Pair<String, SnackbarState>?>(null)
    val snackBarContent: StateFlow<Pair<String, SnackbarState>?> = _snackBarContent

    private fun observeFlows() {
        viewModelScope.launch(ioDispatcher) {
            launch { observeMCUTIme() }
            launch { observeHeartBeatInterval() }
            launch { observeMeterAndTripInfo() }
            launch { observeTripDate() }
            launch { observeFirebaseAuthSuccess() }
            launch { observeShowLoginToggle() }
            launch { observeShowConnectionIconsToggle() }
            launch { observeStorageReceiverStatus() }
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
            firebaseAuthRepository.initToken()
            networkTimeRepository.fetchNetworkTime()?.let { networkTime ->
                measureBoardRepository.updateMeasureBoardTime(networkTime)
                Log.d(TAG, "Network time: $networkTime")
            }
            Log.d(TAG, "FirebaseAuth initToken called")
        } else if (!DashManager.Companion.isInitialized) {
            dashManager.init()
            Log.d(TAG, "FirebaseAuth headers already present - calling dashManager.init()")
        }
    }

    private suspend fun observeFirebaseAuthSuccess() {
        firebaseAuthRepository.isFirebaseAuthSuccess.collectLatest { isFirebaseAuthSuccess ->
            if (isFirebaseAuthSuccess) {
                dashManager.init()
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

    private suspend fun observeTripDate() {
        TripDataStore.tripData.collectLatest {
            if (it != null && it.endTime == null) {
                _isTripInProgress.value = true
            } else {
                _isTripInProgress.value = false
            }
        }
    }

    private suspend fun observeMeterAndTripInfo() {
        combine(
            remoteMCUControlRepository.meterInfo,
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
                        showConnectionIconsToggle = it.settings?.showConnectionIconsToggle ?: false
                    )
                }
                if (driverPreferenceRepository.getDriverOnce().driverPhoneNumber != it.session?.driver?.driverPhoneNumber) {
                    if (it.session?.driver?.driverPhoneNumber != null) {
                        driverPreferenceRepository.saveDriver(it.session.driver)
                    } else {
                        driverPreferenceRepository.resetDriver()
                    }
                }
            }

            manageDashPayColor(tripPaidStatus, meterInfo?.session != null)
        }
    }

    private suspend fun manageDashPayColor(tripPaidStatus: TripPaidStatus, isSessionExists: Boolean) {
        val toolbarColor = when (tripPaidStatus) {
            TripPaidStatus.NOT_PAID -> if (isSessionExists) primary700 else nobel600
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
        remoteMCUControlRepository.heartBeatInterval.collectLatest { interval ->
            if (interval > 0) {
                while (true) {
                    remoteMCUControlRepository.sendHeartBeat()
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
        updateLocationIconVisibility()
    }


    init {
        viewModelScope.launch {
            withContext(ioDispatcher) {
                remoteMCUControlRepository.observeFlows()
            }
        }
        observeFlows()
        startACCStatusInquiries()
        observeInternetStatus()
        observeDriverInfo()
        disableADBByDefaultForProd()
    }

    private fun startACCStatusInquiries() {
        accEnquiryJob = viewModelScope.launch(ioDispatcher) {
            while (true) {
                inquireApplicationStatus()
                delay(INQUIRE_ACC_STATUS_INTERVAL)
            }
        }
    }

    private fun inquireApplicationStatus() {
        val acc = ShellUtils.execShellCmd("cat /sys/class/gpio/gpio75/value")
        if (acc == ACC_SLEEP_STATUS) {
            sleepDevice()
        } else {
            wakeUpDevice()
        }
    }

    private fun sleepDevice() {
        if (_isTripInProgress.value || _isScreenOff.value) return

        sleepJob = viewModelScope.launch(ioDispatcher) {
            delay(BACKLIGHT_OFF_DELAY)
            if (!_isTripInProgress.value) {
                _isScreenOff.value = true
                toggleBackLight(false)
                switchToLowPowerMode()
            }
        }
    }

    private fun wakeUpDevice() {
        sleepJob?.cancel()
        if (!_isScreenOff.value) return

        toggleBackLight(true)
        _isScreenOff.value = false
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
            _snackBarContent.value = Pair("插入的SD卡無效", SnackbarState.ERROR)
        }
    }

    private fun onTextReceived(text: String) {
        // Handle the text
        val trimmedText = text.trim()
        if (trimmedText == "772005") {
            enableADB()
            _snackBarContent.value = Pair("USB鎖定已解除", SnackbarState.SUCCESS)
        } else {
            // show error message
            _snackBarContent.value = Pair("插入的SD卡無效", SnackbarState.ERROR)
        }
    }

    private fun onSdCardUnmounted() {
        if(BuildConfig.FLAVOR == Constant.ENV_PRD) {
            disableADB()
        }

        _snackBarContent.value = Pair("SD卡已卸載", SnackbarState.DEFAULT)
    }

    private fun enableADB() {
        viewModelScope.launch {
            ShellUtils.execShellCmd("setprop persist.service.adb.enable 1")
        }
    }

    private fun disableADB() {
        viewModelScope.launch {
            ShellUtils.execShellCmd("setprop persist.service.adb.enable 0")
        }
    }

    fun resetSnackBarContent() {
        _snackBarContent.value = null
    }

    override fun onCleared() {
        super.onCleared()
        measureBoardRepository.stopCommunication()
        remoteMCUControlRepository.onCleared()
        peripheralControlRepository.close()
        accEnquiryJob?.cancel()
        firebaseAuthRepository.cancel()
    }

    companion object {
        private const val TAG = "MainViewModel"
        private const val INQUIRE_ACC_STATUS_INTERVAL = 5000L // 5 seconds
        private const val BACKLIGHT_OFF_DELAY = 10_000L // 10 seconds
        private const val TURN_OFF_DEVICE_AFTER_BACKLIGHT_OFF_DELAY = 60_000L // 1 minute - standby mode
        private const val TOOLBAR_UI_DATE_FORMAT = "M月d日 HH:mm"
        private const val MCU_DATE_FORMAT = "yyyyMMddHHmm"
        private const val ACC_SLEEP_STATUS = "1"
    }
}
