package com.vismo.cablemeter.model

import java.util.Date

sealed class MCUTripStatus {
    data object ForHire : MCUTripStatus()

    data class Ongoing(
        val tripId: String,
        val startTime: Date,
        var isPaused: Boolean = false,
        var isLocked: Boolean = false,
        var fare: Double = 0.0,
        var extra: Double = 0.0,
        var totalFare: Double = 0.0,
        var distanceInMeter: Double = 0.0,
        var waitDurationInSeconds: Long = 0,
        var overSpeedDurationInSeconds: Int = 0,
    ) : MCUTripStatus()

    data class Ended(
        val tripId: String,
        val startTime: Date,
        val fare: Double,
        val extra: Double,
        val totalFare: Double,
        val distanceInMeter: Double,
        val waitDurationInSeconds: Long,
        val endTime: Date,
    ) : MCUTripStatus()
}
