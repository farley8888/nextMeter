package com.vismo.nextgenmeter.ui.dashboard.trip

data class TripSummaryDashboardUiData (
    val type: TripSummaryDashboardType,
    val totalTrips: String = "0",
    val totalWaitTime: String = "0",
    val totalDistanceInKM: String = "0",
    val totalFare: String = "$0.0",
)

enum class TripSummaryDashboardType {
    ALL,
    NON_DASH,
    DASH,
}