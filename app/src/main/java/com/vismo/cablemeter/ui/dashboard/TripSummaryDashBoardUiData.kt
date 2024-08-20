package com.vismo.cablemeter.ui.dashboard

data class TripSummaryDashBoardUiData (
    val type: TripSummaryDashBoardType,
    val totalTrips: String = "0",
    val totalWaitTime: String = "0",
    val totalDistanceInKM: String = "0",
    val totalFare: String = "$0.0",
)

enum class TripSummaryDashBoardType {
    ALL,
    NON_DASH,
    DASH,
}