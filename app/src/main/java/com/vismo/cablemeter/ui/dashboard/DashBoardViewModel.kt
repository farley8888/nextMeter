package com.vismo.cablemeter.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vismo.cablemeter.module.IoDispatcher
import com.vismo.cablemeter.repository.LocalTripsRepository
import com.vismo.cablemeter.util.GlobalUtils.formatSecondsToCompactFormat
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class DashBoardViewModel @Inject constructor(
    private val localTripsRepository: LocalTripsRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _allTripsSummary: MutableStateFlow<DashBoardScreenUiData> = MutableStateFlow(
        DashBoardScreenUiData(DashBoardDataType.ALL)
    )
    val allTripSummary: StateFlow<DashBoardScreenUiData> = _allTripsSummary

    init {
        viewModelScope.launch {
            withContext(ioDispatcher) {
                localTripsRepository.getAllTripsFlow().collect { allTrips ->
                    val numberOfTrips = allTrips.size.toString()
                    val sumTotalFare = allTrips.sumOf { it.totalFare }.toString()
                    val sumWaitingTime = formatSecondsToCompactFormat(allTrips.sumOf { it.waitDurationInSeconds })
                    val sumOfDistanceInKm = (allTrips.sumOf { it.distanceInMeter } / 1000).toString()
                    _allTripsSummary.value = DashBoardScreenUiData(
                        DashBoardDataType.ALL,
                        numberOfTrips,
                        sumWaitingTime,
                        "$sumOfDistanceInKm",
                        "$$sumTotalFare"
                    )
                }
            }
        }
    }

    fun clearAllLocalTrips() {
        viewModelScope.launch {
            withContext(ioDispatcher) {
                localTripsRepository.clearAllTrips()
            }
        }
    }
}