package com.vismo.nextgenmeter.ui.dashboard.trip

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vismo.nextgenmeter.module.IoDispatcher
import com.vismo.nextgenmeter.repository.LocalTripsRepository
import com.vismo.nextgenmeter.repository.MeterPreferenceRepository
import com.vismo.nextgenmeter.repository.PeripheralControlRepository
import com.vismo.nextgenmeter.util.GlobalUtils.formatSecondsToHHMMSS
import com.vismo.nxgnfirebasemodule.util.DashUtil.roundTo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class TripSummaryDashboardViewModel @Inject constructor(
    private val localTripsRepository: LocalTripsRepository,
    private val meterPreferenceRepository: MeterPreferenceRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val peripheralControlRepository: PeripheralControlRepository
) : ViewModel() {

    private val _allTripsSummary: MutableStateFlow<TripSummaryDashboardUiData> = MutableStateFlow(
        TripSummaryDashboardUiData(TripSummaryDashboardType.ALL)
    )
    val allTripSummary: StateFlow<TripSummaryDashboardUiData> = _allTripsSummary

    private val _dashTripsSummary = MutableStateFlow<TripSummaryDashboardUiData>(
        TripSummaryDashboardUiData(TripSummaryDashboardType.DASH)
    )
    val dashTripsSummary: StateFlow<TripSummaryDashboardUiData> = _dashTripsSummary

    private val _cashTripsSummary = MutableStateFlow<TripSummaryDashboardUiData>(
        TripSummaryDashboardUiData(TripSummaryDashboardType.NON_DASH)
    )
    val cashTripsSummary: StateFlow<TripSummaryDashboardUiData> = _cashTripsSummary

    init {
        getTrips()
    }

    private fun getTrips() {
        viewModelScope.launch {
            withContext(ioDispatcher) {
                localTripsRepository.getAllTripsFlow().collect { allTrips ->
                    if (allTrips.isNotEmpty()) {
                        val currentLicensePlate = meterPreferenceRepository.getLicensePlate().first()
                        val tripsWithLicensePlate = allTrips.filter { it.licensePlate == currentLicensePlate }

                        val sumTotalFare = tripsWithLicensePlate.sumOf { it.fare }.roundTo(2).toString()
                        val sumExtras = tripsWithLicensePlate.sumOf { it.extra }.roundTo(2).toString()
                        val sumWaitingTime =
                            formatSecondsToHHMMSS(tripsWithLicensePlate.sumOf { it.waitDurationInSeconds })
                        val sumOfDistanceInKm =
                            (tripsWithLicensePlate.sumOf { it.distanceInMeter } / 1000).toString()

                        _allTripsSummary.value = TripSummaryDashboardUiData(
                            type = TripSummaryDashboardType.ALL,
                            totalTrips = tripsWithLicensePlate.size.toString(),
                            totalWaitTime = sumWaitingTime,
                            totalDistanceInKM = sumOfDistanceInKm,
                            totalFare = "$$sumTotalFare",
                            totalExtras = "$$sumExtras",
                        )

                        val cashOnlyTrips = tripsWithLicensePlate.filter { !it.isDash  }
                        val sumTotalFareCash = cashOnlyTrips.sumOf { it.fare }.roundTo(2).toString()
                        val sumExtrasCash = cashOnlyTrips.sumOf { it.extra }.roundTo(2).toString()
                        val sumWaitingTimeCash =
                            formatSecondsToHHMMSS(cashOnlyTrips.sumOf { it.waitDurationInSeconds })
                        val sumOfDistanceInKmCash = (cashOnlyTrips.sumOf { it.distanceInMeter } / 1000).toString()
                        _cashTripsSummary.value = TripSummaryDashboardUiData(
                            type = TripSummaryDashboardType.NON_DASH,
                            totalTrips = cashOnlyTrips.size.toString(),
                            totalWaitTime = sumWaitingTimeCash,
                            totalDistanceInKM = sumOfDistanceInKmCash,
                            totalFare = "$$sumTotalFareCash",
                            totalExtras = "$$sumExtrasCash",
                        )

                        val dashOnlyTrips = tripsWithLicensePlate.filter { it.isDash }
                        val sumTotalFareDash = dashOnlyTrips.sumOf { it.fare }.roundTo(2).toString()
                        val sumExtrasDash = dashOnlyTrips.sumOf { it.extra }.roundTo(2).toString()
                        val sumWaitingTimeDash =
                            formatSecondsToHHMMSS(dashOnlyTrips.sumOf { it.waitDurationInSeconds })
                        val sumOfDistanceInKmDash = (dashOnlyTrips.sumOf { it.distanceInMeter } / 1000).toString()
                        _dashTripsSummary.value = TripSummaryDashboardUiData(
                            type = TripSummaryDashboardType.DASH,
                            totalTrips = dashOnlyTrips.size.toString(),
                            totalWaitTime = sumWaitingTimeDash,
                            totalDistanceInKM = sumOfDistanceInKmDash,
                            totalFare = "$$sumTotalFareDash",
                            totalExtras = "$$sumExtrasDash",
                        )

                    }
                }
            }
        }
    }

    fun printSummary() {
        viewModelScope.launch {
            withContext(ioDispatcher) {
//                peripheralControlRepository.printSummary()
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