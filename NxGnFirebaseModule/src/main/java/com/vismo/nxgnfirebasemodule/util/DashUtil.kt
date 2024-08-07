package com.vismo.nxgnfirebasemodule.util

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.vismo.nxgnfirebasemodule.util.Constant.NANOSECONDS
import com.vismo.nxgnfirebasemodule.util.Constant.SECONDS
import com.vismo.nxgnfirebasemodule.util.Constant.SERVER_TIME

object DashUtil {
    fun Map<String, Any?>.toFirestoreFormat(): Map<String, Any?> {
        // Manually convert Timestamp fields to Firestore Timestamp objects
        return this.mapValues { (key, value) ->
            if (value is Map<*, *>) {
                val mapValue = value as Map<String, Number>
                if (mapValue.containsKey(SECONDS) && mapValue.containsKey(NANOSECONDS)) {
                    Timestamp(mapValue[SECONDS]!!.toLong(), mapValue[NANOSECONDS]!!.toInt())
                } else {
                    value
                }
            } else if (key == SERVER_TIME) {
                FieldValue.serverTimestamp()
            } else {
                value
            }
        }
    }
}