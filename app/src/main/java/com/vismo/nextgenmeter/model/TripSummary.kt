package com.vismo.nextgenmeter.model

import com.google.firebase.Timestamp

data class TripSummary(
    val licensePlate : String,
    val firstStartTime: Timestamp,
    val lastEndTime: Timestamp,
    val allTripsCount: Int,
    val cashTripsCount: Int,
    val dashTripsCount: Int,
    val allTripsDistanceInKm: Double,
    val cashTripsDistanceInKm: Double,
    val dashTripsDistanceInKm: Double,
    val allTripsWaitTime: Long,
    val cashTripsWaitTime: Long,
    val dashTripsWaitTime: Long,
    val allTripsFare: Double,
    val cashTripsFare: Double,
    val dashTripsFare: Double,
    val allTripsExtras: Double,
    val cashTripsExtras: Double,
    val dashTripsExtras: Double
)
