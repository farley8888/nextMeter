package com.vismo.cablemeter.model

import com.google.firebase.Timestamp
import com.google.gson.annotations.SerializedName

data class TripData (
    @SerializedName("id") val tripId: String?,
    @SerializedName("trip_start") val startTime: Timestamp?,
    @SerializedName("trip_status") val tripStatus: TripStatus?,
    val isLocked: Boolean = false,
    @SerializedName("fare") val fare: Double = 0.0,
    @SerializedName("extra") val extra: Double = 0.0,
    @SerializedName("total_fare") val totalFare: Double = 0.0,
    @SerializedName("distance") val distanceInMeter: Double = 0.0,
    @SerializedName("wait_time") val waitDurationInSeconds: Long = 0,
    val overSpeedDurationInSeconds: Int = 0,
    val requiresUpdateOnFirestore: Boolean = false,
    @SerializedName("trip_end") val endTime: Timestamp? = null,
)

enum class TripStatus {
    HIRED,
    PAUSED,
    ENDED
}
