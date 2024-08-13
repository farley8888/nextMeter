package com.vismo.nxgnfirebasemodule

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import com.google.gson.Gson
import com.vismo.nxgnfirebasemodule.model.AGPS
import com.vismo.nxgnfirebasemodule.model.GPS
import com.vismo.nxgnfirebasemodule.model.Heartbeat
import com.vismo.nxgnfirebasemodule.model.McuInfo
import com.vismo.nxgnfirebasemodule.model.MeterFields
import com.vismo.nxgnfirebasemodule.model.MeterLocation
import com.vismo.nxgnfirebasemodule.model.MeterSdkConfiguration
import com.vismo.nxgnfirebasemodule.model.MeterTripInFirestore
import com.vismo.nxgnfirebasemodule.model.UpdateMCUParamsRequest
import com.vismo.nxgnfirebasemodule.util.Constant.CONFIGURATIONS_COLLECTION
import com.vismo.nxgnfirebasemodule.util.Constant.CREATED_ON
import com.vismo.nxgnfirebasemodule.util.Constant.HEARTBEAT_COLLECTION
import com.vismo.nxgnfirebasemodule.util.Constant.MCU_INFO
import com.vismo.nxgnfirebasemodule.util.Constant.METERS_COLLECTION
import com.vismo.nxgnfirebasemodule.util.Constant.METER_SDK_DOCUMENT
import com.vismo.nxgnfirebasemodule.util.Constant.TRIPS_COLLECTION
import com.vismo.nxgnfirebasemodule.util.Constant.UPDATE_MCU_PARAMS
import com.vismo.nxgnfirebasemodule.util.DashUtil.toFirestoreFormat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID
import javax.inject.Inject

class DashManager @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val gson: Gson,
    private val dashManagerConfig: DashManagerConfig,
) {
    private val metersCollection = firestore.collection(METERS_COLLECTION)
    private val meterDocument = metersCollection.document(dashManagerConfig.meterIdentifier)
    private val mcuParamsUpdateCollection = meterDocument.collection(UPDATE_MCU_PARAMS)
    private val tripsCollection = meterDocument.collection(TRIPS_COLLECTION)
    private val heartBeatCollection = meterDocument.collection(HEARTBEAT_COLLECTION)

    init {
        meterDocumentListener()
        meterSdkConfigurationListener()
        //TODO: needs to be called after code 682682 is entered - not like this
        isMCUParamsUpdateRequired()
    }

    private val _meterFields: MutableStateFlow<MeterFields?> = MutableStateFlow(null)
    val meterFields: StateFlow<MeterFields?> = _meterFields

    private val _meterSdkConfig: MutableStateFlow<MeterSdkConfiguration?> = MutableStateFlow(null)
    val meterSdkConfig: StateFlow<MeterSdkConfiguration?> = _meterSdkConfig

    private val _mcuParamsUpdateRequired: MutableStateFlow<UpdateMCUParamsRequest?> = MutableStateFlow(null)
    val mcuParamsUpdateRequired: StateFlow<UpdateMCUParamsRequest?> = _mcuParamsUpdateRequired

    private fun isMCUParamsUpdateRequired() {
        mcuParamsUpdateCollection
            .orderBy(CREATED_ON, Query.Direction.DESCENDING)
            .limit(1)
            .get(Source.SERVER)
            .addOnSuccessListener { snapshot ->
                if (snapshot != null && !snapshot.isEmpty) {
                    val latestDocument = snapshot.documents[0]
                    var json = gson.toJson(latestDocument.data)
                    // Manually add the document ID to the JSON string
                    json = json.substring(0, json.length - 1) + ",\"id\":\"${latestDocument.id}\"}"
                    _mcuParamsUpdateRequired.value = gson.fromJson(json, UpdateMCUParamsRequest::class.java)
                }
            }
    }

    fun setMCUParamsUpdateComplete(updateRequest: UpdateMCUParamsRequest) {
        val json = gson.toJson(updateRequest)
        val map = (gson.fromJson(json, Map::class.java) as Map<String, Any?>).toFirestoreFormat()

        mcuParamsUpdateCollection
            .document(updateRequest.id)
            .set(map, SetOptions.merge())
    }

    private fun meterSdkConfigurationListener() {
        firestore.collection(CONFIGURATIONS_COLLECTION)
            .document(METER_SDK_DOCUMENT)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val meterSdkConfigJson = gson.toJson(snapshot.data)
                    _meterSdkConfig.value = gson.fromJson(meterSdkConfigJson, MeterSdkConfiguration::class.java)
                }
            }
    }

    private fun meterDocumentListener() {
        meterDocument
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val meterFieldsJson = gson.toJson(snapshot.data)
                    _meterFields.value = gson.fromJson(meterFieldsJson, MeterFields::class.java)
                }
            }
    }

    fun setMCUInfoOnFirestore(mcuInfo: McuInfo) {
        val json = gson.toJson(mcuInfo)
        val map = (gson.fromJson(json, Map::class.java) as Map<String, Any?>)

        meterDocument
            .set(mapOf(MCU_INFO to map), SetOptions.merge())
    }


    fun updateTripOnFirestore(trip: MeterTripInFirestore) {
        val json = gson.toJson(trip)
        val map = (gson.fromJson(json, Map::class.java) as Map<String, Any?>).toFirestoreFormat()

        tripsCollection
            .document(trip.tripId)
            .set(map, SetOptions.merge())
    }

    fun sendHeartbeat(meterLocation: MeterLocation) {
        val speed = when (meterLocation.gpsType) {
            is AGPS -> {
                meterLocation.gpsType.speed
            }

            is GPS -> {
                meterLocation.gpsType.speed
            }

            else -> null
        }

        val bearing = when (meterLocation.gpsType) {
            is AGPS -> {
                meterLocation.gpsType.bearing
            }

            is GPS -> {
                meterLocation.gpsType.bearing
            }

            else -> null
        }

        val id = UUID.randomUUID().toString()
        val heartbeat = Heartbeat(
            id = id,
            location = meterLocation.geoPoint,
            gpsType = meterLocation.gpsType.toString(),
            deviceTime = Timestamp.now(),
            bearing = bearing,
            speed = speed
        )
        val json = gson.toJson(heartbeat)
        val map = (gson.fromJson(json, Map::class.java) as Map<String, Any?>).toFirestoreFormat()

        heartBeatCollection
            .document(id)
            .set(map, SetOptions.merge())
    }

    // Generic conversion to convert to classes supported by DashManager from external classes
    inline fun <reified T, reified R> convertToType(externalObject: T): R {
        val gson = Gson()
        val json = gson.toJson(externalObject)
        return gson.fromJson(json, R::class.java)
    }
}