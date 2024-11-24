package com.vismo.nextgenmeter.ui.meter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vismo.nextgenmeter.datastore.TripDataStore
import com.vismo.nextgenmeter.repository.TripRepository
import com.vismo.nextgenmeter.model.TripData
import com.vismo.nextgenmeter.model.TripStatus
import com.vismo.nextgenmeter.model.shouldLockMeter
import com.vismo.nextgenmeter.module.IoDispatcher
import com.vismo.nextgenmeter.repository.MeterPreferenceRepository
import com.vismo.nextgenmeter.repository.PeripheralControlRepository
import com.vismo.nextgenmeter.ui.meter.TtsLanguagePref.Companion.KEY_EN
import com.vismo.nextgenmeter.ui.meter.TtsLanguagePref.Companion.KEY_ZH_CN
import com.vismo.nextgenmeter.ui.meter.TtsLanguagePref.Companion.KEY_ZH_HK
import com.vismo.nextgenmeter.ui.shared.SnackbarState
import com.vismo.nextgenmeter.ui.theme.gold600
import com.vismo.nextgenmeter.ui.theme.pastelGreen600
import com.vismo.nextgenmeter.ui.theme.red
import com.vismo.nextgenmeter.ui.theme.valencia900
import com.vismo.nextgenmeter.util.LocaleHelper
import com.vismo.nextgenmeter.util.TtsUtil
import com.vismo.nxgnfirebasemodule.model.TripPaidStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
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
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val tripRepository: TripRepository,
    private val peripheralControlRepository: PeripheralControlRepository,
    private val localeHelper: LocaleHelper,
    private val ttsUtil: TtsUtil,
    private val meterPreferenceRepository: MeterPreferenceRepository
) : ViewModel() {
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

    init {
        viewModelScope.launch {
            withContext(ioDispatcher) {
                launch {
                    combine(
                        TripDataStore.ongoingTripData,
                        TripDataStore.isAbnormalPulseTriggered,
                        tripRepository.remoteUnlockMeter,
                    ) { tripData, isAbnormalPulseTriggered, remoteUnlockMeter -> Triple(tripData, isAbnormalPulseTriggered, remoteUnlockMeter) }
                    .collectLatest { (tripData, isAbnormalPulseTriggered, remoteUnlockMeter) ->
                        _ongoingTrip.value = tripData

                        val trip = tripData ?: return@collectLatest

                        val status: TripStateInMeterOpsUI = when (trip.tripStatus) {
                            TripStatus.HIRED -> Hired
                            TripStatus.STOP -> Paused
                            TripStatus.ENDED, null -> ForHire
                        }

                        if (remoteUnlockMeter && status is Hired) {
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
                                    TripPaidStatus.NOT_PAID -> valencia900
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
                                tripRepository.lockMeter(20, 80, TOTAL_LOCK_BEEP_COUNTER)
                            }
                            MeterLockAction.Unlock -> {
                                tripRepository.unlockMeter()
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
        tripRepository.initObservers()
    }

    fun isTTSPlaying(): Boolean {
        return ttsUtil.isPlaying()
    }

    private suspend fun handleOverSpeed(trip: TripData, isAbnormalPulseTriggered: Boolean) {
        if (trip.overSpeedDurationInSeconds > 3 && _meterLockState.value == MeterLockAction.NoAction) {
            _meterLockState.value = MeterLockAction.Lock(isAbnormalPulseTriggered)
        }
        if (trip.overSpeedDurationInSeconds > LOCK_DIALOG_VISIBILITY_DURATION && uiState.value.overSpeedDurationInSeconds < LOCK_DIALOG_VISIBILITY_DURATION) {
            updateUIStateForTrip(trip, Hired)
        }
        if (trip.overSpeedDurationInSeconds > TOTAL_LOCK_BEEP_COUNTER) {
            updateUIStateForTrip(trip, Hired)
        }
        if (trip.overSpeedDurationInSeconds > TOTAL_LOCK_DURATION && !isUnlockRun) {
            unlockMeterInMCU(isAbnormalPulseTriggered)
        }
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
        uiUpdateMutex.withLock {
            val remainingOverDuration = TOTAL_LOCK_DURATION - trip.overSpeedDurationInSeconds
            _uiState.value = _uiState.value.copy(
                status = status,
                extras = MeterOpsUtil.formatToNDecimalPlace(trip.extra, 1),
                fare = MeterOpsUtil.formatToNDecimalPlace(trip.fare, 2),
                distanceInKM = MeterOpsUtil.getDistanceInKm(trip.distanceInMeter),
                duration = MeterOpsUtil.getFormattedDurationFromSeconds(trip.waitDurationInSeconds),
                totalFare = MeterOpsUtil.formatToNDecimalPlace(trip.totalFare, 2),
                languagePref = _uiState.value.languagePref,
                overSpeedDurationInSeconds = trip.overSpeedDurationInSeconds,
                remainingOverSpeedTimeInSeconds = if(trip.shouldLockMeter() && trip.overSpeedDurationInSeconds > 0) {
                    MeterOpsUtil.getFormattedDurationFromSeconds((if (remainingOverDuration > 0)remainingOverDuration else 0).toLong())
                } else null,
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
                    TtsLanguagePref.OFF -> localeHelper.setLocale("en") // default to English
                }
                uiUpdateMutex.withLock {
                    _uiState.value = _uiState.value.copy(languagePref = newLanguagePref)
                }
                ttsUtil.setLanguagePref(newLanguagePref)
                meterPreferenceRepository.saveSelectedLocale(newLanguagePref.toLanguageCode())
            }
        }
    }

    fun handleKeyEvent(
        code: Int,
        repeatCount: Int,
        isLongPress: Boolean
    ) {
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
                // TODO: put flag down
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
                    peripheralControlRepository.writePrintReceiptCommand(mostRecentTrip)
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
                    peripheralControlRepository.writePrintReceiptCommand(trip)
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
            }
        }
    }

    private suspend fun endTripAndReadyForHire() {
        if (_ongoingTrip.value?.tripStatus == TripStatus.STOP) {
            tripRepository.endTrip()
        }
    }

    override fun onCleared() {
        super.onCleared()
        tripRepository.close()
    }

    companion object {
        private const val LOCK_DIALOG_VISIBILITY_DURATION = 10 // seconds
        private const val TOTAL_LOCK_DURATION = 60*2 // seconds
        const val TOTAL_LOCK_BEEP_COUNTER = 30 //seconds
    }

}

sealed class MeterLockAction {
    data class Lock(val isAbnormalPulse: Boolean) : MeterLockAction()
    object Unlock : MeterLockAction()
    object NoAction : MeterLockAction()
}