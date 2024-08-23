package com.vismo.cablemeter.model

import androidx.datastore.core.Serializer
import com.vismo.cablemeter.util.CryptoManager
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.serialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

@Serializable
data class DriverPinPrefs (
    @Serializable(with = DriverPinMapListSerializer::class)
    val driverPinMap: PersistentList<DriverPinMap> = persistentListOf()
)

@Serializable
data class DriverPinMap(
    val driverId: String,
    val pin: String
)

object DriverPinPrefsSerializer : Serializer<DriverPinPrefs> {
    override val defaultValue: DriverPinPrefs
        get() = DriverPinPrefs()

    override suspend fun readFrom(input: InputStream): DriverPinPrefs {
        return try {
            val decryptedBytes = CryptoManager.decrypt(input)
            Json.decodeFromString(
                deserializer = DriverPinPrefs.serializer(),
                string = decryptedBytes.decodeToString())
        } catch (e: SerializationException) {
            e.printStackTrace()
            defaultValue
        } catch (e: Exception) {
            e.printStackTrace()
            defaultValue
        }
    }

    override suspend fun writeTo(t: DriverPinPrefs, output: OutputStream) {
        CryptoManager.encrypt(
            bytes = Json.encodeToString(
                serializer = DriverPinPrefs.serializer(),
                value = t
            ).toByteArray(),
            outputStream = output
        )
    }

}

class DriverPinMapListSerializer(
    private val serializer: KSerializer<DriverPinMap>,
) : KSerializer<PersistentList<DriverPinMap>> {

    private class PersistentListDescriptor :
        SerialDescriptor by serialDescriptor<List<DriverPinMap>>() {
        @ExperimentalSerializationApi
        override val serialName: String = "kotlinx.serialization.immutable.persistentList"
    }

    override val descriptor: SerialDescriptor = PersistentListDescriptor()

    override fun serialize(encoder: Encoder, value: PersistentList<DriverPinMap>) {
        return ListSerializer(serializer).serialize(encoder, value)
    }

    override fun deserialize(decoder: Decoder): PersistentList<DriverPinMap> {
        return ListSerializer(serializer).deserialize(decoder).toPersistentList()
    }

}