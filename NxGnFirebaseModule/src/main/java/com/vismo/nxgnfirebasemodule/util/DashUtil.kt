package com.vismo.nxgnfirebasemodule.util

import com.google.firebase.Timestamp

object DashUtil {
    fun Map<String, Any?>.toFirestoreFormat(): Map<String, Any?> {
        // Manually convert Timestamp fields to Firestore Timestamp objects
        return this.mapValues { (key, value) ->
            if (value is Map<*, *>) {
                val mapValue = value as Map<String, Number>
                if (mapValue.containsKey("seconds") && mapValue.containsKey("nanoseconds")) {
                    Timestamp(mapValue["seconds"]!!.toLong(), mapValue["nanoseconds"]!!.toInt())
                } else {
                    value
                }
            } else {
                value
            }
        }
    }
}