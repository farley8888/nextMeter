package com.vismo.nxgnfirebasemodule

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import com.google.gson.Gson
import com.vismo.nxgnfirebasemodule.model.AGPS
import com.vismo.nxgnfirebasemodule.model.Driver
import com.vismo.nxgnfirebasemodule.model.GPS
import com.vismo.nxgnfirebasemodule.model.Heartbeat
import com.vismo.nxgnfirebasemodule.model.McuInfo
import com.vismo.nxgnfirebasemodule.model.MeterFields
import com.vismo.nxgnfirebasemodule.model.MeterSdkConfiguration
import com.vismo.nxgnfirebasemodule.model.MeterTripInFirestore
import com.vismo.nxgnfirebasemodule.model.OperatingArea
import com.vismo.nxgnfirebasemodule.model.Session
import com.vismo.nxgnfirebasemodule.model.Settings
import com.vismo.nxgnfirebasemodule.model.TripStatus
import com.vismo.nxgnfirebasemodule.model.UpdateMCUParamsRequest
import com.vismo.nxgnfirebasemodule.util.Constant.CONFIGURATIONS_COLLECTION
import com.vismo.nxgnfirebasemodule.util.Constant.CREATED_ON
import com.vismo.nxgnfirebasemodule.util.Constant.HEARTBEAT_COLLECTION
import com.vismo.nxgnfirebasemodule.util.Constant.METERS_COLLECTION
import com.vismo.nxgnfirebasemodule.util.Constant.METER_SDK_DOCUMENT
import com.vismo.nxgnfirebasemodule.util.Constant.TRIPS_COLLECTION
import com.vismo.nxgnfirebasemodule.util.Constant.UPDATE_MCU_PARAMS
import com.vismo.nxgnfirebasemodule.util.DashUtil.roundTo
import com.vismo.nxgnfirebasemodule.util.DashUtil.toFirestoreFormat
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

class DashManager @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val gson: Gson,
    private val dashManagerConfig: DashManagerConfig,
    private val ioDispatcher: CoroutineDispatcher,
) {
    private val metersCollection = firestore.collection(METERS_COLLECTION)
    private var meterDocumentListener: ListenerRegistration? = null
    private var tripDocumentListener: ListenerRegistration? = null

    private val _meterFields: MutableStateFlow<MeterFields?> = MutableStateFlow(null)
    val meterFields: StateFlow<MeterFields?> = _meterFields

    private val _meterSdkConfig: MutableStateFlow<MeterSdkConfiguration?> = MutableStateFlow(null)
    val meterSdkConfig: StateFlow<MeterSdkConfiguration?> = _meterSdkConfig

    private val _mcuParamsUpdateRequired: MutableStateFlow<UpdateMCUParamsRequest?> = MutableStateFlow(null)
    val mcuParamsUpdateRequired: StateFlow<UpdateMCUParamsRequest?> = _mcuParamsUpdateRequired

    private val _tripInFirestore: MutableStateFlow<MeterTripInFirestore?> = MutableStateFlow(null)
    val tripInFirestore: StateFlow<MeterTripInFirestore?> = _tripInFirestore

    init {
        meterSdkConfigurationListener()
        //TODO: needs to be called after code 682682 is entered - not like this
        isMCUParamsUpdateRequired()
        CoroutineScope(ioDispatcher).launch {
            dashManagerConfig.meterIdentifier.collectLatest {
                meterDocumentListener()
            }
        }
    }

    fun clearDriverSession() {
        CoroutineScope(ioDispatcher).launch {
            val currentSessionId = _meterFields.value?.session?.sessionId
            if (currentSessionId != null) {
                val deleteSessionMap = mapOf(SESSION to FieldValue.delete())
                getMeterDocument().update(deleteSessionMap)
            }
        }
    }

    private fun isMCUParamsUpdateRequired() {
        CoroutineScope(ioDispatcher).launch {
            val mcuParamsUpdateCollection = getMeterDocument()
                .collection(UPDATE_MCU_PARAMS)

            mcuParamsUpdateCollection
                .orderBy(CREATED_ON, Query.Direction.DESCENDING)
                .limit(1)
                .get(Source.SERVER)
                .addOnSuccessListener { snapshot ->
                    if (snapshot != null && !snapshot.isEmpty) {
                        val latestDocument = snapshot.documents[0]
                        var json = gson.toJson(latestDocument.data)
                        // Manually add the document ID to the JSON string
                        json =
                            json.substring(0, json.length - 1) + ",\"id\":\"${latestDocument.id}\"}"
                        _mcuParamsUpdateRequired.value =
                            gson.fromJson(json, UpdateMCUParamsRequest::class.java)
                    }
                }
        }
    }

    fun setMCUParamsUpdateComplete(updateRequest: UpdateMCUParamsRequest) {
        CoroutineScope(ioDispatcher).launch {
            val json = gson.toJson(updateRequest)
            val map =
                (gson.fromJson(json, Map::class.java) as Map<String, Any?>).toFirestoreFormat()

            val mcuParamsUpdateCollection = getMeterDocument()
                .collection(UPDATE_MCU_PARAMS)

            mcuParamsUpdateCollection
                .document(updateRequest.id)
                .set(map, SetOptions.merge())
        }
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
        CoroutineScope(ioDispatcher).launch {
            meterDocumentListener?.remove() // Remove the previous listener

            meterDocumentListener = getMeterDocument()
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        return@addSnapshotListener
                    }

                    if (snapshot != null && snapshot.exists()) {
                        val settings = parseSettings(snapshot)
                        val mcuInfo = parseMcuInfo(snapshot)
                        val session = parseSession(snapshot)

                         _meterFields.value = MeterFields(
                            settings = settings,
                            session = session,
                            mcuInfo = mcuInfo
                        )
                    }
                }
        }
    }

    fun updateFirestoreTripTotalAndFee(tripId: String, total: Double, fee: Double) {
        CoroutineScope(ioDispatcher).launch {
            updateTripOnFirestore(
                MeterTripInFirestore(
                    tripId = tripId,
                    total = total.roundTo(2),
                    dashFee = fee.roundTo(2)
                )
            )
        }
    }

    fun createTripAndSetDocumentListenerOnFirestore(tripId: String) {
        CoroutineScope(ioDispatcher).launch {
            tripDocumentListener?.remove() // Remove the previous listener

            tripDocumentListener = getMeterDocument()
                .collection(TRIPS_COLLECTION)
                .document(tripId)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        return@addSnapshotListener
                    }

                    if (snapshot != null && snapshot.exists()) {
                        val tripJson = gson.toJson(snapshot.data)
                        val trip = gson.fromJson(tripJson, MeterTripInFirestore::class.java)
                        _tripInFirestore.value = trip

                    }
                }
            // should only run once when trip is created
            writeTripFeeInFirestore(tripId)
        }
    }

    fun endTripDocumentListener() {
        tripDocumentListener?.remove()
        _tripInFirestore.value = null
    }

    private fun writeTripFeeInFirestore(tripId: String) {
        CoroutineScope(ioDispatcher).launch {
            // check in meter settings by default
            val settingsFeeRate = _meterFields.value?.settings?.dashFeeRate
            val settingsFeeConstant = _meterFields.value?.settings?.dashFeeConstant

            // fall back to config if not found in settings
            val operatingArea = _meterFields.value?.settings?.operatingArea
            val transactionFee = when (operatingArea) {
                OperatingArea.LANTAU -> _meterSdkConfig.value?.dashFeesConfig?.lantau
                OperatingArea.NT -> _meterSdkConfig.value?.dashFeesConfig?.nt
                OperatingArea.URBAN -> _meterSdkConfig.value?.dashFeesConfig?.urban
                else -> null
            }

            val applicableFeeRate = settingsFeeRate ?: transactionFee?.dashFeeRate ?: _meterSdkConfig.value?.common?.dashFeeRate ?: 0.0
            val applicableFeeConstant = settingsFeeConstant ?: transactionFee?.dashFeeConstant ?: _meterSdkConfig.value?.common?.dashFeeConstant ?: 0.0

            updateTripOnFirestore(
                MeterTripInFirestore(
                    tripId = tripId,
                    dashFeeRate = applicableFeeRate,
                    dashFeeConstant = applicableFeeConstant
                )
            )
        }
    }

    fun setMCUInfoOnFirestore(mcuInfo: McuInfo) {
        CoroutineScope(ioDispatcher).launch {
            val json = gson.toJson(mcuInfo)
            val map = (gson.fromJson(json, Map::class.java) as Map<String, Any?>)

            getMeterDocument()
                .set(mapOf(MCU_INFO to map), SetOptions.merge())
        }
    }


    fun updateTripOnFirestore(trip: MeterTripInFirestore) {
        CoroutineScope(ioDispatcher).launch {
            val json = gson.toJson(trip)
            val map =
                (gson.fromJson(json, Map::class.java) as Map<String, Any?>).toFirestoreFormat()

            val tripsCollection = getMeterDocument()
                .collection(TRIPS_COLLECTION)

            tripsCollection
                .document(trip.tripId)
                .set(map, SetOptions.merge())
        }
    }

    fun sendHeartbeat() {
        CoroutineScope(ioDispatcher).launch {
            val meterLocation = dashManagerConfig.meterLocation.value
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
            val map =
                (gson.fromJson(json, Map::class.java) as Map<String, Any?>).toFirestoreFormat()

            val heartBeatCollection = getMeterDocument()
                .collection(HEARTBEAT_COLLECTION)

            heartBeatCollection
                .document(id)
                .set(map, SetOptions.merge())
        }
    }

    private fun getMeterDocument() = metersCollection.document(dashManagerConfig.meterIdentifier.value)

    // Generic conversion to convert to classes supported by DashManager from external classes
    inline fun <reified T, reified R> convertToType(externalObject: T): R {
        val gson = Gson()
        val json = gson.toJson(externalObject)
        return gson.fromJson(json, R::class.java)
    }

    private fun parseSettings(snapshot: DocumentSnapshot): Settings? {
        if (snapshot.data?.get(SETTINGS) == null) {
            return null
        }
        val settingsJson = gson.toJson(snapshot.data?.get(SETTINGS))
        return gson.fromJson(settingsJson, Settings::class.java)
    }

    private fun parseMcuInfo(snapshot: DocumentSnapshot): McuInfo? {
        if (snapshot.data?.get(MCU_INFO) == null) {
            return null
        }
        val mcuInfoJson = gson.toJson(snapshot.data?.get(MCU_INFO))
        return gson.fromJson(mcuInfoJson, McuInfo::class.java)
    }

    private fun parseSession(snapshot: DocumentSnapshot): Session? {
        val sessionMap = snapshot.data?.get(SESSION) as? Map<String, *>
        val sessionId = sessionMap?.get(SESSION_ID) as? String
        val driverMap = sessionMap?.get(DRIVER) as? Map<String, *>

        return sessionId?.let {
            driverMap?.let { driverData ->
                val driver = parseDriver(driverData)
                Session(sessionId = it, driver = driver)
            }
        }
    }

    private fun parseDriver(driverMap: Map<String, *>): Driver {
        return Driver(
            driverPhoneNumber = driverMap[DRIVER_ID] as String,
            driverName = driverMap[DRIVER_NAME] as String,
            driverChineseName = driverMap[DRIVER_NAME_CH] as String,
            driverLicense = driverMap[DRIVER_LICENSE] as String
        )
    }

    fun onCleared() {
        meterDocumentListener?.remove()
    }

    companion object {
        private const val SETTINGS = "settings"
        private const val MCU_INFO = "mcu_info"
        private const val SESSION = "session"
        private const val SESSION_ID = "id"
        private const val DRIVER = "driver"
        private const val DRIVER_ID = "id"
        private const val DRIVER_NAME = "name"
        private const val DRIVER_NAME_CH = "name_ch"
        private const val DRIVER_LICENSE = "driver_license"
    }
}