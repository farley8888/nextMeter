package com.vismo.cablemeter

import android.content.Context
import android.os.PowerManager
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ilin.util.ShellUtils
import com.vismo.cablemeter.datastore.MCUParamsDataStore
import com.vismo.cablemeter.datastore.TripDataStore
import com.vismo.cablemeter.ui.topbar.TopAppBarUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import com.vismo.cablemeter.module.IoDispatcher
import com.vismo.cablemeter.repository.FirebaseAuthRepository
import com.vismo.cablemeter.repository.MeasureBoardRepository
import com.vismo.cablemeter.repository.PeripheralControlRepository
import com.vismo.cablemeter.repository.RemoteMeterControlRepository
import com.vismo.cablemeter.repository.TripRepository
import com.vismo.cablemeter.ui.theme.gold600
import com.vismo.cablemeter.ui.theme.nobel600
import com.vismo.cablemeter.ui.theme.pastelGreen600
import com.vismo.cablemeter.ui.theme.primary700
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
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
    private val firebaseAuthRepository: FirebaseAuthRepository
    ) : ViewModel(){
    private val _topAppBarUiState = MutableStateFlow(TopAppBarUiState())
    val topAppBarUiState: StateFlow<TopAppBarUiState> = _topAppBarUiState

    private val _showLoginToggle = MutableStateFlow<Boolean?>(null)
    val showLoginToggle: StateFlow<Boolean?> = _showLoginToggle

    private val dateFormat = SimpleDateFormat(TOOLBAR_UI_DATE_FORMAT, Locale.TRADITIONAL_CHINESE)

    private val toolbarUiDataUpdateMutex = Mutex()

    private val _isTripInProgress = MutableStateFlow(false)
    val isTripInProgress: StateFlow<Boolean> = _isTripInProgress

    private var accEnquiryJob: Job? = null
    private var sleepJob: Job? = null
    private val _isScreenOff = MutableStateFlow(false)

    private fun observeFlows() {
        viewModelScope.launch(ioDispatcher) {
            launch { observeMCUTIme() }
            launch { observeHeartBeatInterval() }
            launch { observeMeterAndTripInfo() }
            launch { observeTripDate() }
        }
    }

    private suspend fun observeTripDate() {
        TripDataStore.tripData.collectLatest {
            if (it != null && it.endTime == null) {
                updateBackButtonVisibility(false)
                _isTripInProgress.value = true
            } else {
                updateBackButtonVisibility(true)
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
                _showLoginToggle.value = it.settings?.showLoginToggle

                toolbarUiDataUpdateMutex.withLock {
                    _topAppBarUiState.value = _topAppBarUiState.value.copy(
                        showLoginToggle = it.settings?.showLoginToggle ?: false,
                        driverPhoneNumber = it.session?.driver?.driverPhoneNumber ?: "",
                    )
                }
            }

            val toolbarColor = when (tripPaidStatus) {
                TripPaidStatus.NOT_PAID -> if (meterInfo?.session != null) primary700 else nobel600
                TripPaidStatus.COMPLETELY_PAID -> pastelGreen600
                TripPaidStatus.PARTIALLY_PAID -> gold600
            }
            toolbarUiDataUpdateMutex.withLock {
                _topAppBarUiState.value = _topAppBarUiState.value.copy(
                    color = toolbarColor
                )
            }
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
        MCUParamsDataStore.mcuTime.collectLatest {
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
