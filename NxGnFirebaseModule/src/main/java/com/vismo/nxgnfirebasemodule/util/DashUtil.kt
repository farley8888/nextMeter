package com.vismo.nxgnfirebasemodule.util

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.GeoPoint
import com.vismo.nxgnfirebasemodule.util.Constant.NANOSECONDS
import com.vismo.nxgnfirebasemodule.util.Constant.SECONDS
import com.vismo.nxgnfirebasemodule.util.Constant.SERVER_TIME
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object DashUtil {

    private const val LATITUDE = "latitude"
    private const val LONGITUDE = "longitude"

    fun Map<String, Any?>.toFirestoreFormat(): Map<String, Any?> {
        // Manually convert Timestamp fields to Firestore Timestamp objects
        return this.mapValues { (key, value) ->
            if (value is Map<*, *>) {
                val mapValue = value as Map<String, Number>
                if (mapValue.containsKey(SECONDS) && mapValue.containsKey(NANOSECONDS)) {
                    Timestamp(mapValue[SECONDS]!!.toLong(), mapValue[NANOSECONDS]!!.toInt())
                } else if(mapValue.containsKey(LATITUDE) && mapValue.containsKey(LONGITUDE)) {
                    GeoPoint(mapValue[LATITUDE]!!.toDouble(), mapValue[LONGITUDE]!!.toDouble())
                } else {
                    value
                }
            } else if (key == SERVER_TIME) {
                FieldValue.serverTimestamp()
            }  else {
                value
            }
        }
    }

    /**
     * Helper functions and var for AMap
     */
    private const val PI = 3.14159265358979324
    private const val A = 6378245.0
    private const val EE = 0.00669342162296594323

    private fun transformLat(x: Double, y: Double): Double {
        var ret = -100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y + 0.1 * x * y + 0.2 * sqrt(abs(x))
        ret += (20.0 * sin(6.0 * x * PI) + 20.0 * sin(2.0 * x * PI)) * 2.0 / 3.0
        ret += (20.0 * sin(y * PI) + 40.0 * sin(y / 3.0 * PI)) * 2.0 / 3.0
        ret += (160.0 * sin(y / 12.0 * PI) + 320.0 * sin(y * PI / 30.0)) * 2.0 / 3.0
        return ret
    }

    private fun transformLon(x: Double, y: Double): Double {
        var ret = 300.0 + x + 2.0 * y + 0.1 * x * x + 0.1 * x * y + 0.1 * sqrt(abs(x))
        ret += (20.0 * sin(6.0 * x * PI) + 20.0 * sin(2.0 * x * PI)) * 2.0 / 3.0
        ret += (20.0 * sin(x * PI) + 40.0 * sin(x / 3.0 * PI)) * 2.0 / 3.0
        ret += (150.0 * sin(x / 12.0 * PI) + 300.0 * sin(x / 30.0 * PI)) * 2.0 / 3.0
        return ret
    }

    private fun outOfChina(lat: Double, lon: Double): Boolean {
        if (lon < 72.004 || lon > 137.8347) return true
        if (lat < 0.8293 || lat > 55.8271) return true
        return false
    }

    /**
     * Coordinate converter gcj2Wgs84
     * @param lat latitude from gcj02
     * @param lon longitude from gcj02
     * @return converted coordinate Pair in wgs84
     */
    fun gcj2Wgs(gcjLat: Double, gcjLon: Double): Pair<Double, Double> {
        if (outOfChina(gcjLat, gcjLon)) {
            return Pair(gcjLat, gcjLon)
        }
        var dLat = transformLat(gcjLon - 105.0, gcjLat - 35.0)
        var dLon = transformLon(gcjLon - 105.0, gcjLat - 35.0)
        val radLat = gcjLat / 180.0 * PI
        var magic = sin(radLat)
        magic = 1 - EE * magic * magic
        val sqrtMagic = sqrt(magic)
        dLat = (dLat * 180.0) / ((A * (1 - EE)) / (magic * sqrtMagic) * PI)
        dLon = (dLon * 180.0) / (A / sqrtMagic * cos(radLat) * PI)
        val mgLat = gcjLat + dLat
        val mgLon = gcjLon + dLon
        return Pair(gcjLat * 2 - mgLat, gcjLon * 2 - mgLon)
    }
}