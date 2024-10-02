package com.vismo.cablemeter.ui.meter

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vismo.cablemeter.datastore.MCUParamsDataStore
import com.vismo.cablemeter.datastore.TripDataStore
import com.vismo.cablemeter.repository.TripRepository
import com.vismo.cablemeter.model.TripData
import com.vismo.cablemeter.model.TripStatus
import com.vismo.cablemeter.module.IoDispatcher
import com.vismo.cablemeter.repository.PeripheralControlRepository
import com.vismo.cablemeter.util.LocaleHelper
import com.vismo.cablemeter.util.TtsUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class MeterOpsViewModel @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val tripRepository: TripRepository,
    private val peripheralControlRepository: PeripheralControlRepository,
    private val localeHelper: LocaleHelper,
    private val ttsUtil: TtsUtil
) : ViewModel() {

    private val _currentTrip = MutableStateFlow<TripData?>(null)

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

    init {
        viewModelScope.launch {
            withContext(ioDispatcher) {
                TripDataStore.tripData.collect { trip ->
                    _currentTrip.value = trip
                    if (trip != null) {
                        val status: TripStateInMeterOpsUI = when (trip.tripStatus) {
                            TripStatus.HIRED -> {
                                Hired
                            }

                            TripStatus.PAUSED -> {
                                Paused
                            }

                            TripStatus.ENDED, null -> {
                                ForHire
                            }
                        }
                        if (status == ForHire) {
                            _uiState.value = MeterOpsUiData(
                                status = status,
                                fare = "",
                                extras = "",
                                distanceInKM = "",
                                duration = "",
                                totalFare = "",
                                languagePref = _uiState.value.languagePref
                            )
                            return@collect
                        } else {
                            _uiState.value = MeterOpsUiData(
                                status = status,
                                extras = MeterOpsUtil.formatToNDecimalPlace(trip.extra, 1),
                                fare = MeterOpsUtil.formatToNDecimalPlace(trip.fare, 2),
                                distanceInKM = MeterOpsUtil.getDistanceInKm(trip.distanceInMeter),
                                duration = MeterOpsUtil.getFormattedDurationFromSeconds(trip.waitDurationInSeconds),
                                totalFare = MeterOpsUtil.formatToNDecimalPlace(trip.totalFare, 2),
                                languagePref = _uiState.value.languagePref
                            )
                        }
                    }
                }
            }
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
                    TtsLanguagePref.EN -> localeHelper.setLocale("en")
                    TtsLanguagePref.ZH_CN -> localeHelper.setLocale("zh")
                    TtsLanguagePref.ZH_HK -> localeHelper.setLocale("zh-rHK")
                    TtsLanguagePref.OFF -> localeHelper.setLocale("en") // default to English
                }
                _uiState.value = _uiState.value.copy(languagePref = newLanguagePref)
                ttsUtil.setLanguagePref(newLanguagePref)
            }
        }
    }

    fun handleKeyEvent(
        code: Int,
        repeatCount: Int,
        isLongPress: Boolean
    ) {
        when (code) {
            248 -> {
                // ready for hire
                endTripAndReadyForHire()
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
                // print receipt
                printReceipt()
            }
        }
    }

    private fun printReceipt() {
        viewModelScope.launch(ioDispatcher) {
            _currentTrip.value?.let { trip ->
                if (trip.tripStatus == TripStatus.PAUSED) {
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

    private fun startOrResumeTrip() {
        viewModelScope.launch(ioDispatcher) {
            if (_currentTrip.value == null) {
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
            if (_currentTrip.value == null) {
                tripRepository.startAndPauseTrip()
            } else {
                tripRepository.pauseTrip()
            }
            ttsUtil.setWasTripJustPaused(true)
        }
    }

    private fun endTripAndReadyForHire() {
        viewModelScope.launch(ioDispatcher) {
            if (_currentTrip.value?.tripStatus == TripStatus.PAUSED) {
                tripRepository.endTrip()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        tripRepository.close()
    }

}