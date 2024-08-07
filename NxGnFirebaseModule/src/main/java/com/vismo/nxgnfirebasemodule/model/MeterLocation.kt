package com.vismo.nxgnfirebasemodule.model

import com.google.firebase.firestore.GeoPoint

data class MeterLocation (
    val geoPoint: GeoPoint,
    val gpsType: GPSType
) {

}

sealed class GPSType
class AGPS(val speed: Double, val bearing: Double) : GPSType() {
    override fun toString(): String {
        return "AGPS"
    }
}
class GPS(val speed: Double, val bearing: Double) : GPSType() {
    override fun toString(): String {
        return "GPS"
    }
}
object NOT_SET: GPSType() {
    override fun toString(): String {
        return "NOT_SET"
    }
}