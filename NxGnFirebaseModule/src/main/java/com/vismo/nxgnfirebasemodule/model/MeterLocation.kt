package com.vismo.nxgnfirebasemodule.model

import com.google.firebase.firestore.GeoPoint

public data class MeterLocation (
    val geoPoint: GeoPoint,
    val gpsType: GPSType
) {

}

public sealed class GPSType
public class AGPS(val speed: Double, val bearing: Double) : GPSType() {
    override fun toString(): String {
        return "AGPS"
    }
}
public class GPS(val speed: Double, val bearing: Double) : GPSType() {
    override fun toString(): String {
        return "GPS"
    }
}
public object NOT_SET: GPSType() {
    override fun toString(): String {
        return "NOT_SET"
    }
}