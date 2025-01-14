package com.vismo.nxgnfirebasemodule.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ServerTimestamp
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.google.gson.annotations.SerializedName
import com.vismo.nxgnfirebasemodule.util.Constant.NANOSECONDS
import com.vismo.nxgnfirebasemodule.util.Constant.SECONDS
import com.vismo.nxgnfirebasemodule.util.Constant.SERVER_TIME
import java.lang.reflect.Type

data class Heartbeat(
    @SerializedName("id") val id: String,
    @SerializedName("location") val location: GeoPoint,
    @SerializedName("gps_type") val gpsType: String,
    @SerializedName("device_time") val deviceTime: Timestamp,
    @SerializedName(SERVER_TIME) @ServerTimestamp val serverTime: Timestamp? = null,
    @SerializedName("bearing") val bearing: Number?,
    @SerializedName("speed") val speed: Number?,
    @SerializedName("meter_software_version") val meterSoftwareVersion: String? = null,
    @SerializedName("acc_status") val deviceAccStatus: String? = null
)

class HeartbeatSerializer : JsonSerializer<Heartbeat> {
    override fun serialize(src: Heartbeat, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        val jsonObject = JsonObject()

        jsonObject.addProperty("id", src.id)
        jsonObject.add("location", context.serialize(src.location))
        jsonObject.addProperty("gps_type", src.gpsType)
        jsonObject.add("device_time", context.serialize(src.deviceTime))
        jsonObject.add(SERVER_TIME, context.serialize(src.serverTime))
        jsonObject.addProperty("meter_software_version", src.meterSoftwareVersion)
        jsonObject.addProperty("acc_status", src.deviceAccStatus)

        src.bearing?.let { jsonObject.addProperty("bearing", it) }
        src.speed?.let { jsonObject.addProperty("speed", it) }

        return jsonObject
    }
}

class TimestampSerializer : JsonSerializer<Timestamp> {
    override fun serialize(src: Timestamp?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
        return if (src == null) {
            JsonNull.INSTANCE
        } else {
            JsonObject().apply {
                addProperty(SECONDS, src.seconds)
                addProperty(NANOSECONDS, src.nanoseconds)
            }
        }
    }
}

class TimestampDeserializer : JsonDeserializer<Timestamp> {
    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): Timestamp {
        return if (json == null || !json.isJsonObject) {
            Timestamp(0, 0)
        } else {
            val jsonObject = json.asJsonObject
            val seconds = jsonObject.get(SECONDS).asLong
            val nanoseconds = jsonObject.get(NANOSECONDS).asInt
            Timestamp(seconds, nanoseconds)
        }
    }
}