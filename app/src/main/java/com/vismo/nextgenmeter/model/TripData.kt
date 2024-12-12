package com.vismo.nextgenmeter.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.google.firebase.Timestamp
import com.google.gson.annotations.SerializedName
import com.vismo.nextgenmeter.db.Converters
import javax.annotation.Nonnull

@Entity(tableName = "trips")
@TypeConverters(Converters::class)
data class TripData (

    @PrimaryKey(autoGenerate = false)
    @Nonnull
    @SerializedName("id")
    val tripId: String,

    @ColumnInfo(name = "license_plate")
    @SerializedName("license_plate")
    val licensePlate: String = "",

    @ColumnInfo(name = "device_id")
    @SerializedName("device_id")
    val deviceId: String = "",

    @ColumnInfo(name = "start_time")
    @SerializedName("trip_start")
    val startTime: Timestamp,

    @ColumnInfo(name = "trip_status")
    @SerializedName("trip_status")
    val tripStatus: TripStatus?,

    @ColumnInfo(name = "fare")
    @SerializedName("fare")
    val fare: Double = 0.0,

    @ColumnInfo(name = "extra")
    @SerializedName("extra")
    val extra: Double = 0.0,

    @ColumnInfo(name = "total_fare")
    @SerializedName("trip_total")
    val totalFare: Double = 0.0,

    @ColumnInfo(name = "distance_in_meter")
    @SerializedName("distance")
    val distanceInMeter: Double = 0.0,

    @ColumnInfo(name = "wait_time")
    @SerializedName("wait_time")
    val waitDurationInSeconds: Long = 0,

    @ColumnInfo(name = "pause_time")
    @SerializedName("trip_pause")
    val pauseTime: Timestamp? = null,


    @ColumnInfo(name = "end_time")
    @SerializedName("trip_end")
    val endTime: Timestamp? = null,

    @ColumnInfo(name = "is_dash")
    @SerializedName("is_dash")
    val isDash: Boolean = false,

    val overSpeedDurationInSeconds: Int = 0,
    val requiresUpdateOnDatabase: Boolean = false,
    val abnormalPulseCounter: Int? = null,
    val overSpeedCounter: Int? = null,
    val mcuStatus: Int? = null,
    val isNewTrip: Boolean = false,
)

enum class TripStatus {
    HIRED,
    STOP, // - this means the meter is PAUSED - TODO: rename later so that POS is compatible with PAUSED status
    ENDED
}

fun TripData.shouldLockMeter(): Boolean {
    val currentMCUStatus = OngoingMeasureBoardStatus.fromInt(mcuStatus ?: -1)
    val isStatusLocked =
        currentMCUStatus is OngoingMeasureBoardStatusOverspeed || currentMCUStatus is OngoingMeasureBoardStatusFault || currentMCUStatus is OngoingMeasureBoardStatusFaultAndOverspeed
    val isOverSpeed = overSpeedDurationInSeconds > 0
    return isStatusLocked && isOverSpeed
}

fun TripData.isAbnormalPulseStatus(): Boolean {
    val currentMCUStatus = OngoingMeasureBoardStatus.fromInt(mcuStatus ?: -1)
    val isAbnormalPulse = currentMCUStatus is OngoingMeasureBoardStatusFault || currentMCUStatus is OngoingMeasureBoardStatusFaultAndOverspeed
    return isAbnormalPulse
}

fun TripData.getRemainingLockTimeInSeconds(maxLockDurationInSeconds: Long, minTimeAfter: Long = 30): Long? {
    val shouldLockMeter = shouldLockMeter()
    return if (!shouldLockMeter) {
        null
    } else {
        val remainingLockTime = maxLockDurationInSeconds - overSpeedDurationInSeconds
        if (remainingLockTime <= 0 || overSpeedDurationInSeconds < minTimeAfter) {
            null
        } else {
            remainingLockTime
        }
    }
}
