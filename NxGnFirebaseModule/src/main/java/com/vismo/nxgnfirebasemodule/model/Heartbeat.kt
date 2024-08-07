package com.vismo.nxgnfirebasemodule.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.GeoPoint
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.google.gson.annotations.SerializedName
import java.lang.reflect.Type

data class Heartbeat(
    @SerializedName("id") val id: String,
    @SerializedName("location") val location: GeoPoint,
    @SerializedName("gps_type") val gpsType: String,
    @SerializedName("device_time") val deviceTime: Timestamp,
    @SerializedName("server_time") val serverTime: FieldValue,
    @SerializedName("bearing") val bearing: Number?,
    @SerializedName("speed") val speed: Number?,
)

class HeartbeatSerializer : JsonSerializer<Heartbeat> {
    override fun serialize(src: Heartbeat, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        val jsonObject = JsonObject()

        jsonObject.addProperty("id", src.id)
        jsonObject.add("location", context.serialize(src.location))
        jsonObject.addProperty("gps_type", src.gpsType)
        jsonObject.add("device_time", context.serialize(src.deviceTime))
        jsonObject.add("server_time", context.serialize(src.serverTime))

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
                addProperty("seconds", src.seconds)
                addProperty("nanoseconds", src.nanoseconds)
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
            val seconds = jsonObject.get("seconds").asLong
            val nanoseconds = jsonObject.get("nanoseconds").asInt
            Timestamp(seconds, nanoseconds)
        }
    }
}