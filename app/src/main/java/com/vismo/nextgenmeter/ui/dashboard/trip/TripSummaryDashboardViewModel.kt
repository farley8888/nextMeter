package com.vismo.nextgenmeter.ui.dashboard.trip

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.vismo.nextgenmeter.model.TripData
import com.vismo.nextgenmeter.model.TripSummary
import com.vismo.nextgenmeter.module.IoDispatcher
import com.vismo.nextgenmeter.repository.MeterPreferenceRepository
import com.vismo.nextgenmeter.repository.PeripheralControlRepository
import com.vismo.nextgenmeter.repository.TripFileManager
import com.vismo.nextgenmeter.ui.meter.MeterOpsUtil.formatToNDecimalPlace
import com.vismo.nextgenmeter.util.GlobalUtils.formatSecondsToHHMMSS
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class TripSummaryDashboardViewModel @Inject constructor(
    private val tripFileManager: TripFileManager,
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

    private val _showTripsClearedToast = MutableSharedFlow<Boolean>()
    val showTripsClearedToast: SharedFlow<Boolean> = _showTripsClearedToast

    init {
        getTrips()
    }

    private fun getTrips() {
        viewModelScope.launch {
            withContext(ioDispatcher) {
                tripFileManager.descendingSortedTrip.collectLatest { allTrips ->
                    if (allTrips.isNotEmpty()) {
                        val sumTotalFare = formatToNDecimalPlace(allTrips.sumOf { it.fare }, 2)
                        val sumExtras = formatToNDecimalPlace(allTrips.sumOf { it.extra }, 2)
                        val sumWaitingTime =
                            formatSecondsToHHMMSS(allTrips.sumOf { it.waitDurationInSeconds })
                        val sumOfDistanceInKm =
                            (allTrips.sumOf { it.paidDistanceInMeters } / 1000).toString()

                        _allTripsSummary.value = TripSummaryDashboardUiData(
                            type = TripSummaryDashboardType.ALL,
                            totalTrips = allTrips.size.toString(),
                            totalWaitTime = sumWaitingTime,
                            totalDistanceInKM = sumOfDistanceInKm,
                            totalFare = "$$sumTotalFare",
                            totalExtras = "$$sumExtras",
                        )

                        val cashOnlyTrips = allTrips.filter { !it.isDash }
                        val sumTotalFareCash =
                            formatToNDecimalPlace(cashOnlyTrips.sumOf { it.fare }, 2)
                        val sumExtrasCash =
                            formatToNDecimalPlace(cashOnlyTrips.sumOf { it.extra }, 2)
                        val sumWaitingTimeCash =
                            formatSecondsToHHMMSS(cashOnlyTrips.sumOf { it.waitDurationInSeconds })
                        val sumOfDistanceInKmCash =
                            (cashOnlyTrips.sumOf { it.paidDistanceInMeters } / 1000).toString()
                        _cashTripsSummary.value = TripSummaryDashboardUiData(
                            type = TripSummaryDashboardType.NON_DASH,
                            totalTrips = cashOnlyTrips.size.toString(),
                            totalWaitTime = sumWaitingTimeCash,
                            totalDistanceInKM = sumOfDistanceInKmCash,
                            totalFare = "$$sumTotalFareCash",
                            totalExtras = "$$sumExtrasCash",
                        )

                        val dashOnlyTrips = allTrips.filter { it.isDash }
                        val sumTotalFareDash =
                            formatToNDecimalPlace(dashOnlyTrips.sumOf { it.fare }, 2)
                        val sumExtrasDash =
                            formatToNDecimalPlace(dashOnlyTrips.sumOf { it.extra }, 2)
                        val sumWaitingTimeDash =
                            formatSecondsToHHMMSS(dashOnlyTrips.sumOf { it.waitDurationInSeconds })
                        val sumOfDistanceInKmDash =
                            (dashOnlyTrips.sumOf { it.paidDistanceInMeters } / 1000).toString()
                        _dashTripsSummary.value = TripSummaryDashboardUiData(
                            type = TripSummaryDashboardType.DASH,
                            totalTrips = dashOnlyTrips.size.toString(),
                            totalWaitTime = sumWaitingTimeDash,
                            totalDistanceInKM = sumOfDistanceInKmDash,
                            totalFare = "$$sumTotalFareDash",
                            totalExtras = "$$sumExtrasDash",
                        )

                    } else {
                        _allTripsSummary.value = TripSummaryDashboardUiData(
                            type = TripSummaryDashboardType.ALL,
                        )
                        _cashTripsSummary.value = TripSummaryDashboardUiData(
                            type = TripSummaryDashboardType.NON_DASH,
                        )
                        _dashTripsSummary.value = TripSummaryDashboardUiData(
                            type = TripSummaryDashboardType.DASH,
                        )
                    }
                }
            }
        }
    }

    fun printSummary() {
        viewModelScope.launch {
            withContext(ioDispatcher) {
                val sortedTrips = tripFileManager.descendingSortedTrip.firstOrNull()
                if (sortedTrips.isNullOrEmpty()) return@withContext
                val tripSummary = TripSummary(
                    licensePlate = meterPreferenceRepository.getLicensePlate().first() ?: "",
                    firstStartTime = sortedTrips.last().startTime,
                    lastEndTime = sortedTrips.first().endTime ?: Timestamp.now(),
                    allTripsCount = sortedTrips.size,
                    cashTripsCount = sortedTrips.count { !it.isDash },
                    dashTripsCount = sortedTrips.count { it.isDash },
                    allTripsDistanceInKm = sortedTrips.sumOf { it.paidDistanceInMeters } / 1000,
                    cashTripsDistanceInKm = sortedTrips.filter { !it.isDash }.sumOf { it.paidDistanceInMeters } / 1000,
                    dashTripsDistanceInKm = sortedTrips.filter { it.isDash }.sumOf { it.paidDistanceInMeters } / 1000,
                    allTripsWaitTime = sortedTrips.sumOf { it.waitDurationInSeconds },
                    cashTripsWaitTime = sortedTrips.filter { !it.isDash }.sumOf { it.waitDurationInSeconds },
                    dashTripsWaitTime = sortedTrips.filter { it.isDash }.sumOf { it.waitDurationInSeconds },
                    allTripsFare = sortedTrips.sumOf { it.fare },
                    cashTripsFare = sortedTrips.filter { !it.isDash }.sumOf { it.fare },
                    dashTripsFare = sortedTrips.filter { it.isDash }.sumOf { it.fare },
                    allTripsExtras = sortedTrips.sumOf { it.extra },
                    cashTripsExtras = sortedTrips.filter { !it.isDash }.sumOf { it.extra },
                    dashTripsExtras = sortedTrips.filter { it.isDash }.sumOf { it.extra }
                )
                peripheralControlRepository.printSummaryReceiptCommand(tripSummary)
            }
        }
    }

    fun clearAllLocalTrips() {
        viewModelScope.launch {
            withContext(ioDispatcher) {
                tripFileManager.deleteAllTrips()
                _showTripsClearedToast.emit(true)
                delay(2000)
                _showTripsClearedToast.emit(false)
            }
        }
    }

}