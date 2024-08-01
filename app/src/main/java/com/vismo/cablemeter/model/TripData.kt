package com.vismo.cablemeter.model

import java.util.Date

data class TripData (
    val tripId: String?,
    val startTime: Date?,
    val tripStatus: TripStatus?,
    val isLocked: Boolean = false,
    val fare: Double = 0.0,
    val extra: Double = 0.0,
    val totalFare: Double = 0.0,
    val distanceInMeter: Double = 0.0,
    val waitDurationInSeconds: Long = 0,
    val overSpeedDurationInSeconds: Int = 0,
    val endTime: Date? = null,
)

enum class TripStatus {
    HIRED,
    PAUSED,
    ENDED
}
