package com.vismo.cablemeter.ui.meter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vismo.cablemeter.model.ForHire
import com.vismo.cablemeter.model.Hired
import com.vismo.cablemeter.model.MCUTripStatus
import com.vismo.cablemeter.model.MeterOpsUiData
import com.vismo.cablemeter.model.Paused
import com.vismo.cablemeter.module.IoDispatcher
import com.vismo.cablemeter.repository.MeasureBoardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MeterOpsViewModel @Inject constructor(
    private val measureBoardRepository: MeasureBoardRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _currentTripState = MutableStateFlow<MCUTripStatus?>(null)

    private val _uiState = MutableStateFlow<MeterOpsUiData>(
        MeterOpsUiData(
            status = ForHire,
            fare = "",
            extras = "",
            distanceInKM = "",
            duration = "",
        )
    )
    val uiState = _uiState

    init {
        viewModelScope.launch {
            measureBoardRepository.tripStatus.collect { tripStatus ->
                _currentTripState.value = tripStatus
                when(tripStatus) {
                    is MCUTripStatus.Ongoing -> {
                        if (tripStatus.isPaused) {
                            _uiState.value = MeterOpsUiData(
                                status = Paused,
                                extras = MeterOpsUtil.formatToNDecimalPlace(tripStatus.extra, 1),
                                fare = MeterOpsUtil.formatToNDecimalPlace(tripStatus.fare, 2),
                                distanceInKM = MeterOpsUtil.getDistanceInKm(tripStatus.distanceInMeter),
                                duration = MeterOpsUtil.getFormattedDurationFromSeconds(tripStatus.waitDurationInSeconds),
                                )
                        } else {
                            _uiState.value = MeterOpsUiData(
                                status = Hired,
                                extras = MeterOpsUtil.formatToNDecimalPlace(tripStatus.extra, 1),
                                fare = MeterOpsUtil.formatToNDecimalPlace(tripStatus.fare, 2),
                                distanceInKM = MeterOpsUtil.getDistanceInKm(tripStatus.distanceInMeter),
                                duration = MeterOpsUtil.getFormattedDurationFromSeconds(tripStatus.waitDurationInSeconds),

                            )
                        }
                    }
                    else -> {
                        _uiState.value = MeterOpsUiData(
                            status = ForHire,
                            fare = "",
                            extras = "",
                            distanceInKM = "",
                            duration = "",
                        )
                    }
                }
            }
        }
    }

    fun handleKeyEvent(code: Int, repeatCount: Int, isLongPress: Boolean) {
        when (code) {
            248 -> {
                // ready for hire
                endTripAndReadyForHire()
            }
            249 -> {
                // start/resume trip
                startTrip()
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
            }
        }
    }

    private fun addExtras(extrasAmount: Int) {
        viewModelScope.launch(ioDispatcher) {
            _currentTripState.value?.let {
                if (it is MCUTripStatus.Ongoing) {
                    measureBoardRepository.addExtras(extrasAmount)
                    return@launch
                }
            }
        }
    }

    private fun startTrip() {
        viewModelScope.launch(ioDispatcher) {
            _currentTripState.value?.let {
                if (it == MCUTripStatus.ForHire) {
                    measureBoardRepository.startTrip()
                    return@launch
                }
            }
        }
    }

    private fun pauseTrip() {
        viewModelScope.launch(ioDispatcher) {
            _currentTripState.value?.let {
                if (it == MCUTripStatus.ForHire) {
                    measureBoardRepository.startAndPauseTrip()
                    return@launch
                } else if (it is MCUTripStatus.Ongoing && !it.isPaused) {
                    measureBoardRepository.pauseTrip()
                    return@launch
                }
            }
        }
    }

    private fun endTripAndReadyForHire() {
        viewModelScope.launch(ioDispatcher) {
            _currentTripState.value?.let {
                if (it is MCUTripStatus.Ongoing && it.isPaused) {
                    measureBoardRepository.endTrip()
                    return@launch
                }
            }
        }
    }

    override fun onCleared() {
        measureBoardRepository.stopCommunication()
        super.onCleared()
    }

}