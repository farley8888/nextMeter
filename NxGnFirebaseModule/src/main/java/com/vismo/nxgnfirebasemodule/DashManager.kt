package com.vismo.nxgnfirebasemodule

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.gson.Gson
import com.vismo.nxgnfirebasemodule.model.AGPS
import com.vismo.nxgnfirebasemodule.model.GPS
import com.vismo.nxgnfirebasemodule.model.Heartbeat
import com.vismo.nxgnfirebasemodule.model.MeterFields
import com.vismo.nxgnfirebasemodule.model.MeterSdkConfiguration
import com.vismo.nxgnfirebasemodule.model.MeterTripInFirestore
import com.vismo.nxgnfirebasemodule.util.Constant.CONFIGURATIONS_COLLECTION
import com.vismo.nxgnfirebasemodule.util.Constant.HEARTBEAT_COLLECTION
import com.vismo.nxgnfirebasemodule.util.Constant.METERS_COLLECTION
import com.vismo.nxgnfirebasemodule.util.Constant.METER_SDK_DOCUMENT
import com.vismo.nxgnfirebasemodule.util.Constant.TRIPS_COLLECTION
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

    init {
        meterDocumentListener()
        meterSdkConfigurationListener()
    }

    private val _meterFields: MutableStateFlow<MeterFields?> = MutableStateFlow(null)
    val meterFields: StateFlow<MeterFields?> = _meterFields

    private val _meterSdkConfig: MutableStateFlow<MeterSdkConfiguration?> = MutableStateFlow(null)
    val meterSdkConfig: StateFlow<MeterSdkConfiguration?> = _meterSdkConfig

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
        firestore.collection(METERS_COLLECTION)
            .document(dashManagerConfig.meterIdentifier)
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


    fun updateTripOnFirestore(trip: MeterTripInFirestore) {
        val json = gson.toJson(trip)
        val map = (gson.fromJson(json, Map::class.java) as Map<String, Any?>).toFirestoreFormat()

        firestore.collection(METERS_COLLECTION)
            .document(dashManagerConfig.meterIdentifier)
            .collection(TRIPS_COLLECTION)
            .document(trip.tripId)
            .set(map, SetOptions.merge())
    }

    fun sendHeartbeat() {
        val meterLocation = dashManagerConfig.meterLocation

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

        firestore.collection(METERS_COLLECTION)
            .document(dashManagerConfig.meterIdentifier)
            .collection(HEARTBEAT_COLLECTION)
            .document(id)
            .set(map, SetOptions.merge())
    }

    // Generic conversion function using Gson
    fun <T> convertToMeterTripInFirestore(externalTrip: T): MeterTripInFirestore {
        val json = Gson().toJson(externalTrip)
        return Gson().fromJson(json, MeterTripInFirestore::class.java)
    }

    fun <T> convertToExternalTrip(meterTripInFirestore: MeterTripInFirestore, externalClass: Class<T>): T {
        val json = Gson().toJson(meterTripInFirestore)
        return Gson().fromJson(json, externalClass)
    }
}