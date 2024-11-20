package com.vismo.nextgenmeter.ui.dashboard.trip

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vismo.nextgenmeter.module.IoDispatcher
import com.vismo.nextgenmeter.repository.LocalTripsRepository
import com.vismo.nextgenmeter.util.GlobalUtils.formatSecondsToCompactFormat
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class TripSummaryDashboardViewModel @Inject constructor(
    private val localTripsRepository: LocalTripsRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _allTripsSummary: MutableStateFlow<TripSummaryDashboardUiData> = MutableStateFlow(
        TripSummaryDashboardUiData(TripSummaryDashboardType.ALL)
    )
    val allTripSummary: StateFlow<TripSummaryDashboardUiData> = _allTripsSummary

    init {
        viewModelScope.launch {
            withContext(ioDispatcher) {
                localTripsRepository.getAllTripsFlow().collect { allTrips ->
                    val numberOfTrips = allTrips.size.toString()
                    val sumTotalFare = allTrips.sumOf { it.totalFare }.toString()
                    val sumWaitingTime = formatSecondsToCompactFormat(allTrips.sumOf { it.waitDurationInSeconds })
                    val sumOfDistanceInKm = (allTrips.sumOf { it.distanceInMeter } / 1000).toString()
                    _allTripsSummary.value = TripSummaryDashboardUiData(
                        TripSummaryDashboardType.ALL,
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