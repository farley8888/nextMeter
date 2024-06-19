package com.vismo.cablemeter.model

sealed class MCUTripStatus {
    data object ForHire : MCUTripStatus()

    data class Ongoing(
        val isPaused: Boolean = false,
        val isLocked: Boolean = false,
        val fare: Double,
        val extra: Double,
        val totalFare: Double,
        val distanceInMeter: Double,
        val waitDurationInSeconds: Long,
        val overSpeedDurationInSeconds: Int,
    ) : MCUTripStatus()

    data object Ended : MCUTripStatus()
}
