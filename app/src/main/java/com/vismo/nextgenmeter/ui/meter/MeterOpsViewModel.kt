package com.vismo.nextgenmeter.ui.meter

import android.content.Context
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vismo.nextgenmeter.BuildConfig
import com.vismo.nextgenmeter.datastore.DeviceDataStore
import com.vismo.nextgenmeter.datastore.TripDataStore
import com.vismo.nextgenmeter.model.Quadruple
import com.vismo.nextgenmeter.repository.TripRepository
import com.vismo.nextgenmeter.model.TripData
import com.vismo.nextgenmeter.model.TripStatus
import com.vismo.nextgenmeter.model.getRemainingLockTimeInSeconds
import com.vismo.nextgenmeter.model.isAbnormalPulseStatus
import com.vismo.nextgenmeter.model.shouldLockMeter
import com.vismo.nextgenmeter.module.IoDispatcher
import com.vismo.nextgenmeter.repository.LogShippingRepository
import com.vismo.nextgenmeter.repository.MeterPreferenceRepository
import com.vismo.nextgenmeter.repository.PeripheralControlRepository
import com.vismo.nextgenmeter.repository.RemoteMeterControlRepository
import com.vismo.nextgenmeter.service.DeviceGodCodeUnlockState
import com.vismo.nextgenmeter.ui.meter.TtsLanguagePref.Companion.KEY_EN
import com.vismo.nextgenmeter.ui.meter.TtsLanguagePref.Companion.KEY_ZH_CN
import com.vismo.nextgenmeter.ui.meter.TtsLanguagePref.Companion.KEY_ZH_HK
import com.vismo.nextgenmeter.ui.shared.SnackbarState
import com.vismo.nextgenmeter.ui.theme.gold600
import com.vismo.nextgenmeter.ui.theme.pastelGreen600
import com.vismo.nextgenmeter.ui.theme.red
import com.vismo.nextgenmeter.util.LocaleHelper
import com.vismo.nextgenmeter.util.MeasureBoardUtils
import com.vismo.nextgenmeter.util.TtsUtil
import com.vismo.nxgnfirebasemodule.model.TripPaidStatus
import com.vismo.nxgnfirebasemodule.model.isKeyLogEnabled
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
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
import javax.inject.Inject

@HiltViewModel
class MeterOpsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val tripRepository: TripRepository,
    private val logShippingRepository: LogShippingRepository,
    private val peripheralControlRepository: PeripheralControlRepository,
    private val localeHelper: LocaleHelper,
    private val ttsUtil: TtsUtil,
    private val meterPreferenceRepository: MeterPreferenceRepository,
    private val remoteMeterControlRepository: RemoteMeterControlRepository
) : ViewModel() {

    private val TAG = "MeterOpsViewModel"

    private val _ongoingTrip = MutableStateFlow<TripData?>(null)
    private val _uiState = MutableStateFlow<MeterOpsUiData>(
        MeterOpsUiData(
            status = ForHire,
            fare = "",
            extras = "",
            distanceInKM = "",
            duration = "",
            totalFare = "",
        )
    )
    val uiState = _uiState
    private val _meterLockState = MutableStateFlow<MeterLockAction>(MeterLockAction.NoAction)
    val meterLockState = _meterLockState
    private val uiUpdateMutex = Mutex()
    private var isUnlockRun = false
    private val _showSnackBarMessage = MutableStateFlow<Pair<String, SnackbarState>?>(null)
    val showSnackBarMessage: StateFlow<Pair<String, SnackbarState>?> = _showSnackBarMessage

    // Track whether an upload is currently in progress
    private val _isUploading = MutableLiveData(false)
    val isUploading: LiveData<Boolean> get() = _isUploading

    init {
        viewModelScope.launch {
            withContext(ioDispatcher) {
                launch {
                    combine(
                        TripDataStore.ongoingTripData,
                        TripDataStore.isAbnormalPulseTriggered,
                        tripRepository.remoteUnlockMeter,
                        DeviceDataStore.deviceGodCodeUnlockState,
                    ) { tripData, isAbnormalPulseTriggered, remoteUnlockMeter, deviceGodUnlockState -> Quadruple(tripData, isAbnormalPulseTriggered, remoteUnlockMeter, deviceGodUnlockState) }
                    .collectLatest { (tripData, isAbnormalPulseTriggered, remoteUnlockMeter, deviceGodUnlockState) ->
                        _ongoingTrip.value = tripData

                        val trip = tripData ?: return@collectLatest

                        val status: TripStateInMeterOpsUI = when (trip.tripStatus) {
                            TripStatus.HIRED -> Hired
                            TripStatus.STOP -> Paused
                            TripStatus.ENDED, null -> ForHire
                        }

                        if ((remoteUnlockMeter || deviceGodUnlockState == DeviceGodCodeUnlockState.Unlocked) && (status is Hired || status is Paused) && trip.shouldLockMeter()) {
                            unlockMeterInMCU(isAbnormalPulseTriggered)
                            tripRepository.resetUnlockMeterStatusInRemote()
                            return@collectLatest
                        }
                        else if (status == ForHire) {
                            updateUIStateForHire()
                            return@collectLatest
                        }
                        else if (trip.shouldLockMeter()) {
                            handleOverSpeed(trip, isAbnormalPulseTriggered)
                            return@collectLatest
                        }
                        else {
                            updateUIStateForTrip(trip, status)
                        }
                    }
                }
                launch {
                    tripRepository.currentTripPaidStatus.collectLatest {
                        uiUpdateMutex.withLock {
                            _uiState.value = _uiState.value.copy(
                                totalColor = when (it) {
                                    TripPaidStatus.NOT_PAID -> red
                                    TripPaidStatus.COMPLETELY_PAID -> pastelGreen600
                                    TripPaidStatus.PARTIALLY_PAID -> gold600
                                }
                            )
                        }
                    }
                }
                launch {
                    meterLockState.collectLatest {
                        when (it) {
                            is MeterLockAction.Lock -> {
                                tripRepository.lockMeter(10, 80, TOTAL_LOCK_BEEP_COUNTER, isAbnormalPulse = it.isAbnormalPulse)
                            }
                            MeterLockAction.Unlock -> {
                                tripRepository.unlockMeter()
                                delay(1000)
                                tripRepository.endTrip()
                            }

                            MeterLockAction.NoAction -> {
                                // do nothing
                            }
                        }
                    }
                }
                launch {
                    TripDataStore.mostRecentTripData.collectLatest {
                        it?.let {
                            updateUIStateForTrip(it, PastTrip)
                        }
                    }
                }
                launch {
                    // set current state of language preference
                    val currentLocale = meterPreferenceRepository.getSelectedLocale().first() ?: ""
                    localeHelper.setLocale(if(currentLocale.isNotEmpty()) currentLocale else KEY_EN)
                    val languagePref = when (currentLocale) {
                        KEY_EN -> TtsLanguagePref.EN
                        KEY_ZH_CN -> TtsLanguagePref.ZH_CN
                        KEY_ZH_HK -> TtsLanguagePref.ZH_HK
                        else -> TtsLanguagePref.OFF
                    }
                    uiUpdateMutex.withLock {
                        _uiState.value = _uiState.value.copy(languagePref = languagePref)
                        ttsUtil.setLanguagePref(languagePref)
                    }
                }
            }
        }
    }

    fun isTTSPlaying(): Boolean {
        return ttsUtil.isPlaying()
    }

    private suspend fun handleOverSpeed(trip: TripData, isAbnormalPulseTriggered: Boolean) {
        val isAbnormalPulse = isAbnormalPulseTriggered || trip.isAbnormalPulseStatus()
        if (_meterLockState.value == MeterLockAction.NoAction) {
            _meterLockState.value = MeterLockAction.Lock(isAbnormalPulse)
        }
        if (trip.getRemainingLockTimeInSeconds(TOTAL_COUNTDOWN_DURATION.toLong()) == null && trip.overSpeedDurationInSeconds > TOTAL_LOCK_BEEP_COUNTER && !isUnlockRun) {
            unlockMeterInMCU(isAbnormalPulseTriggered)
        }
        updateUIStateForTrip(trip, Hired)
    }

    private suspend fun unlockMeterInMCU(isAbnormalPulse: Boolean) {
        isUnlockRun = true
        if (isAbnormalPulse) {
            TripDataStore.setAbnormalPulseTriggered(false)
        }
        _meterLockState.value = MeterLockAction.Unlock
    }

    private suspend fun updateUIStateForHire() {
        uiUpdateMutex.withLock {
            _uiState.value = MeterOpsUiData(
                status = ForHire,
                fare = "",
                extras = "",
                distanceInKM = "",
                duration = "",
                totalFare = "",
                languagePref = _uiState.value.languagePref,
                totalColor = red,
                remainingOverSpeedTimeInSeconds = null
            )
        }
        _meterLockState.value = MeterLockAction.NoAction
        isUnlockRun = false
    }

    private suspend fun updateUIStateForTrip(trip: TripData, status: TripStateInMeterOpsUI) {
//        val savedStartPrice = meterPreferenceRepository.getMcuStartPrice().first()?.replace("$", "") ?: "0.0"
//        val startPrice = MeasureBoardUtils.formatStartingPrice(savedStartPrice).toDouble()
        val fareIfZero = trip.fare // if (trip.fare == 0.0) startPrice else trip.fare
        val totalFareIfZero = trip.totalFare // if (trip.totalFare == 0.0) startPrice else trip.totalFare

        uiUpdateMutex.withLock {
            val isLocked = trip.shouldLockMeter()
            val remainingLockTimeInSeconds = trip.getRemainingLockTimeInSeconds(TOTAL_COUNTDOWN_DURATION.toLong())
            _uiState.value = _uiState.value.copy(
                status = status,
                extras = MeterOpsUtil.formatToNDecimalPlace(trip.extra, 1),
                fare = MeterOpsUtil.formatToNDecimalPlace(fareIfZero, 2),
                distanceInKM = MeterOpsUtil.getDistanceInKm(trip.paidDistanceInMeters),
                duration = MeterOpsUtil.getFormattedDurationFromSeconds(trip.waitDurationInSeconds),
                totalFare = MeterOpsUtil.formatToNDecimalPlace(totalFareIfZero, 2),
                languagePref = _uiState.value.languagePref,
                overSpeedDurationInSeconds = if (isLocked) trip.overSpeedDurationInSeconds else 0,
                remainingOverSpeedTimeInSeconds = if(remainingLockTimeInSeconds != null) MeterOpsUtil.getFormattedDurationFromSeconds(remainingLockTimeInSeconds) else null,
                mcuStartingPrice = if (isLocked) {
                    val savedStartPrice = meterPreferenceRepository.getMcuStartPrice().first()?.replace("$", "") ?: DEFAULT_STARTING_PRICE
                    val startPrice = MeasureBoardUtils.formatStartingPrice(savedStartPrice)
                    if (startPrice.isNotEmpty()) startPrice else DEFAULT_STARTING_PRICE
                } else "",
            )
        }
    }

    fun toggleLanguagePref() {
        viewModelScope.launch {
            withContext(ioDispatcher) {
                if (ttsUtil.isPlaying()) {
                    return@withContext
                }
                val newLanguagePref = when (_uiState.value.languagePref) {
                    TtsLanguagePref.EN -> TtsLanguagePref.ZH_CN
                    TtsLanguagePref.ZH_CN -> TtsLanguagePref.ZH_HK
                    TtsLanguagePref.ZH_HK -> TtsLanguagePref.OFF
                    TtsLanguagePref.OFF -> TtsLanguagePref.EN
                }
                when (newLanguagePref) {
                    TtsLanguagePref.EN -> localeHelper.setLocale(KEY_EN)
                    TtsLanguagePref.ZH_CN -> localeHelper.setLocale(KEY_ZH_CN)
                    TtsLanguagePref.ZH_HK -> localeHelper.setLocale(KEY_ZH_HK)
                    TtsLanguagePref.OFF -> localeHelper.setLocale(null)
                }
                uiUpdateMutex.withLock {
                    _uiState.value = _uiState.value.copy(languagePref = newLanguagePref)
                }
                ttsUtil.setLanguagePref(newLanguagePref)
                meterPreferenceRepository.saveSelectedLocale(newLanguagePref.toLanguageCode())
            }
        }
    }

    fun triggerLogShipping(){
        if (_isUploading.value == true) {
            // Already uploading, show a message and return
            _showSnackBarMessage.value = Pair(
                "日誌發送中，請稍後！⌛ Log upload already in progress, please wait...",
                SnackbarState.WARNING
            )
            return
        }
        _isUploading.value = true
        viewModelScope.launch {
            Log.d(TAG, "triggerLogShipping")
            val result = withContext(ioDispatcher) {
                logShippingRepository.handleLogUploadFlow()
            }

            when {
                result.isSuccess -> {
                    val uploadedCount = result.getOrNull() ?: 0
                    _showSnackBarMessage.value = Pair(
                        "已發送日誌 $uploadedCount 條 ✨ $uploadedCount log(s) uploaded successfully.",
                        SnackbarState.SUCCESS
                    )
                }
                result.isFailure -> {
                    val exception = result.exceptionOrNull()
                    _showSnackBarMessage.value = Pair(
                        "Failed to upload logs: ${exception?.message ?: "Unknown error"}",
                        SnackbarState.ERROR
                    )
                }
            }
            _isUploading.value = false
        }
    }

    fun handleKeyEvent(
        code: Int,
        repeatCount: Int,
        isLongPress: Boolean
    ) {
        // Log key press if enabled via configuration
        if (remoteMeterControlRepository.meterSdkConfiguration.value.isKeyLogEnabled) {
            Log.d(TAG, "handleKeyEvent: code=$code, repeatCount=$repeatCount, isLongPress=$isLongPress")
        }

        if (isLongPress || repeatCount > 1) {
            // handle long press
            handleLongPress(code, repeatCount)

        } else {
            // handle single press
            handleSinglePress(code)
        }
    }

    private fun handleLongPress(code: Int, repeatCount: Int) {
        when (code) {
            252 -> {
                if (repeatCount.mod(25) == 0) {
                    toggleFlagWithoutTrip()
                }
            }

            253 -> {
                if (repeatCount.mod(10) == 0) {
                    // subtract extras - $10
                    subtractExtras(10)
                }
            }

            254 -> {
                if (repeatCount.mod(10) == 0) {
                    // subtract extras - $1
                    subtractExtras(1)
                }
            }
            251 -> {
                if (BuildConfig.FLAVOR != "prd") {
                    if (repeatCount.mod(25) == 0) {
                        DeviceDataStore.setToggleCommunicationWithMCU()
                    }
                }
            }
        }
    }

    private fun handleSinglePress(code: Int) {
        when (code) {
            248 -> {
                // ready for hire or remove most recent trip
                handleOn248Pressed()
            }

            249 -> {
                // start/resume trip
                startOrResumeTrip()
            }

            250 -> {
                // pause/start-and-pause trip
                pauseTrip()
            }

            253 -> {
                // add extras - $10
                addExtras(10)
            }

            254 -> {
                // add extras - $1
                addExtras(1)
            }

            255 -> {
                // print receipt or show recent trip details
                handleRecentTripDetailsOrPrintTrip()
            }
        }
    }

    private fun toggleFlagWithoutTrip() {
        viewModelScope.launch {
            if (_ongoingTrip.value == null && peripheralControlRepository.isFlagDown()) {
                peripheralControlRepository.toggleForHireFlag(goDown = false)
            } else {
                peripheralControlRepository.toggleForHireFlag(goDown = true)
            }
            tripRepository.emitBeepSound()
        }
    }

    private fun handleRecentTripDetailsOrPrintTrip() {
        viewModelScope.launch(ioDispatcher) {
            if (_ongoingTrip.value == null) { // only if there is no ongoing trip
                val mostRecentTrip = TripDataStore.mostRecentTripData.first()
                if (mostRecentTrip == null) {
                    try {
                        tripRepository.getMostRecentTrip()
                    } catch (e: Exception) {
                        // show error message if there is no recent trip
                        _showSnackBarMessage.value = Pair("没有行程資料", SnackbarState.ERROR)
                    }
                } else {
                    // print receipt for recent trip
                    peripheralControlRepository.printTripReceiptCommand(mostRecentTrip)
                }
            } else {
                printReceiptForOngoingTrip()
            }
        }
    }

    fun clearSnackBarMessage() {
        _showSnackBarMessage.value = null
    }

    private fun printReceiptForOngoingTrip() {
        viewModelScope.launch(ioDispatcher) {
            _ongoingTrip.value?.let { trip ->
                if (trip.tripStatus == TripStatus.STOP) {
                    peripheralControlRepository.printTripReceiptCommand(trip)
                }
            }
        }
    }

    private fun addExtras(extrasAmount: Int) {
        viewModelScope.launch(ioDispatcher) {
            tripRepository.addExtras(extrasAmount)
        }
    }

    private fun subtractExtras(extrasAmount: Int) {
        viewModelScope.launch(ioDispatcher) {
            tripRepository.subtractExtras(extrasAmount)
        }
    }

    private fun startOrResumeTrip() {
        viewModelScope.launch(ioDispatcher) {
            if(TripDataStore.mostRecentTripData.first() != null) {
                return@launch
            }
            if (_ongoingTrip.value == null) {
                TripDataStore.setIsTripInProgress(true)
                tripRepository.startTrip()
                ttsUtil.setWasTripJustStarted(true)
                return@launch
            } else {
                tripRepository.resumeTrip()
            }
        }
    }

    private fun pauseTrip() {
        viewModelScope.launch(ioDispatcher) {
            if(TripDataStore.mostRecentTripData.first() != null) {
                return@launch
            }
            if (_ongoingTrip.value == null) {
                TripDataStore.setIsTripInProgress(true)
                tripRepository.startAndPauseTrip()
            } else {
                tripRepository.pauseTrip()
            }
            ttsUtil.setWasTripJustPaused(true)
        }
    }

    private fun handleOn248Pressed() {
        viewModelScope.launch(ioDispatcher) {
            if (_ongoingTrip.value != null) {
                endTripAndReadyForHire()
            } else if (TripDataStore.mostRecentTripData.first() != null) {
                TripDataStore.clearMostRecentTripData()
                updateUIStateForHire()
            } else if (TripDataStore.mostRecentTripData.first() == null && _ongoingTrip.value == null) {
                // press end to raise flag if there is no ongoing trip and no recent trip - try to put flag up
                peripheralControlRepository.toggleForHireFlag(goDown = false)
            }
        }
    }

    private suspend fun endTripAndReadyForHire() {
        if (_ongoingTrip.value?.tripStatus == TripStatus.STOP) {
            tripRepository.endTrip()
            Log.d(TAG, "endTripAndReadyForHire: end trip")
        }
    }

    companion object {
        private const val LOCK_DIALOG_VISIBILITY_DURATION = 10 // seconds
        private const val TOTAL_COUNTDOWN_DURATION = 60*60 // seconds - max time shown in countdown
        const val TOTAL_LOCK_BEEP_COUNTER = 30 //seconds
        private const val TOTAL_LOCK_DURATION = TOTAL_COUNTDOWN_DURATION + TOTAL_LOCK_BEEP_COUNTER // seconds
        private const val DEFAULT_STARTING_PRICE = "27.00"
        private const val TAG = "MeterOpsViewModel"
    }

}

sealed class MeterLockAction {
    data class Lock(val isAbnormalPulse: Boolean) : MeterLockAction()
    object Unlock : MeterLockAction()
    object NoAction : MeterLockAction()
}