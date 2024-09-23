package com.vismo.cablemeter.ui.meter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vismo.cablemeter.datastore.TripDataStore
import com.vismo.cablemeter.repository.TripRepository
import com.vismo.cablemeter.model.ForHire
import com.vismo.cablemeter.model.Hired
import com.vismo.cablemeter.model.TripData
import com.vismo.cablemeter.model.MeterOpsUiData
import com.vismo.cablemeter.model.Paused
import com.vismo.cablemeter.model.TripStateInMeterOpsUI
import com.vismo.cablemeter.model.TripStatus
import com.vismo.cablemeter.module.IoDispatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MeterOpsViewModel @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val tripRepository: TripRepository
) : ViewModel() {

    private val _currentTrip = MutableStateFlow<TripData?>(null)

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
                        )
                        return@collect
                    } else {
                        _uiState.value = MeterOpsUiData(
                            status = status,
                            extras = MeterOpsUtil.formatToNDecimalPlace(trip.extra, 1),
                            fare = MeterOpsUtil.formatToNDecimalPlace(trip.fare, 2),
                            distanceInKM = MeterOpsUtil.getDistanceInKm(trip.distanceInMeter),
                            duration = MeterOpsUtil.getFormattedDurationFromSeconds(trip.waitDurationInSeconds),
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
                return@launch
            } else {
                tripRepository.pauseTrip()
            }
        }
    }

    private fun endTripAndReadyForHire() {
        viewModelScope.launch(ioDispatcher) {
            if (_currentTrip.value?.tripStatus == TripStatus.PAUSED) {
                tripRepository.endTrip()
            }
        }
    }

}