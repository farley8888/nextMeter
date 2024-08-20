package com.vismo.cablemeter.ui.dashboard

data class DashBoardScreenUiData (
    val type: DashBoardDataType,
    val totalTrips: String = "0",
    val totalWaitTime: String = "0",
    val totalDistanceInKM: String = "0",
    val totalFare: String = "$0.0",
)

enum class DashBoardDataType {
    ALL,
    NON_DASH,
    DASH,
}