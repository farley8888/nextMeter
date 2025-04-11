package com.vismo.nxgnfirebasemodule

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
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
import com.vismo.nxgnfirebasemodule.model.HealthCheckStatus
import com.vismo.nxgnfirebasemodule.model.Heartbeat
import com.vismo.nxgnfirebasemodule.model.McuInfo
import com.vismo.nxgnfirebasemodule.model.McuInfoStatus
import com.vismo.nxgnfirebasemodule.model.MeterDeviceProperties
import com.vismo.nxgnfirebasemodule.model.MeterFields
import com.vismo.nxgnfirebasemodule.model.MeterSdkConfiguration
import com.vismo.nxgnfirebasemodule.model.MeterTripInFirestore
import com.vismo.nxgnfirebasemodule.model.OperatingArea
import com.vismo.nxgnfirebasemodule.model.Session
import com.vismo.nxgnfirebasemodule.model.Settings
import com.vismo.nxgnfirebasemodule.model.TripSession
import com.vismo.nxgnfirebasemodule.model.Update
import com.vismo.nxgnfirebasemodule.model.UpdateMCUParamsRequest
import com.vismo.nxgnfirebasemodule.model.UpdateStatus
import com.vismo.nxgnfirebasemodule.model.isCompleted
import com.vismo.nxgnfirebasemodule.model.shouldPrompt
import com.vismo.nxgnfirebasemodule.util.Constant.AUDIT_COLLECTION
import com.vismo.nxgnfirebasemodule.util.Constant.CONFIGURATIONS_COLLECTION
import com.vismo.nxgnfirebasemodule.util.Constant.CREATED_ON
import com.vismo.nxgnfirebasemodule.util.Constant.HEARTBEAT_COLLECTION
import com.vismo.nxgnfirebasemodule.util.Constant.LOGGING_COLLECTION
import com.vismo.nxgnfirebasemodule.util.Constant.METERS_COLLECTION
import com.vismo.nxgnfirebasemodule.util.Constant.METER_DEVICES_COLLECTION
import com.vismo.nxgnfirebasemodule.util.Constant.METER_SDK_DOCUMENT
import com.vismo.nxgnfirebasemodule.util.Constant.OTA_FIRMWARE_TYPE
import com.vismo.nxgnfirebasemodule.util.Constant.TRIPS_COLLECTION
import com.vismo.nxgnfirebasemodule.util.Constant.UPDATES_COLLECTION
import com.vismo.nxgnfirebasemodule.util.Constant.UPDATE_MCU_PARAMS
import com.vismo.nxgnfirebasemodule.util.DashUtil.toFirestoreFormat
import com.vismo.nxgnfirebasemodule.util.LogConstant
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

class DashManager @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val gson: Gson,
    private val dashManagerConfig: DashManagerConfig,
    private val ioDispatcher: CoroutineDispatcher,
    private val env: String
) {
    private val meterDevicesCollection = firestore.collection(METER_DEVICES_COLLECTION)
    private val metersCollection = firestore.collection(METERS_COLLECTION)
    private var meterDocumentListener: ListenerRegistration? = null
    private var tripDocumentListener: ListenerRegistration? = null
    private var meterDevicesDocumentListener: ListenerRegistration? = null

    private val _meterFields: MutableStateFlow<MeterFields?> = MutableStateFlow(null)
    val meterFields: StateFlow<MeterFields?> = _meterFields

    private val _meterSdkConfig: MutableStateFlow<MeterSdkConfiguration?> = MutableStateFlow(null)
    val meterSdkConfig: StateFlow<MeterSdkConfiguration?> = _meterSdkConfig

    private val _meterDeviceProperties: MutableStateFlow<MeterDeviceProperties?> = MutableStateFlow(null)
    val meterDeviceProperties: StateFlow<MeterDeviceProperties?> = _meterDeviceProperties

    private val _mcuParamsUpdateRequired: MutableStateFlow<UpdateMCUParamsRequest?> = MutableStateFlow(null)
    val mcuParamsUpdateRequired: StateFlow<UpdateMCUParamsRequest?> = _mcuParamsUpdateRequired

    private val _tripInFirestore: MutableStateFlow<MeterTripInFirestore?> = MutableStateFlow(null)
    val tripInFirestore: StateFlow<MeterTripInFirestore?> = _tripInFirestore

    private val _remoteUnlockMeter: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val remoteUnlockMeter: StateFlow<Boolean> = _remoteUnlockMeter

    private val _mostRelevantUpdate: MutableStateFlow<Update?> = MutableStateFlow(null)
    val mostRelevantUpdate: StateFlow<Update?> = _mostRelevantUpdate

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e(TAG, "Coroutine exception", throwable)
    }

    private var externalScope: CoroutineScope? = null

    fun init(scope: CoroutineScope, mostRecentlyCompletedUpdateId: String?) {
        externalScope = scope
        setMeterInfoToSettings()
        meterSdkConfigurationListener()
        scope.launch {
            launch { observeMeterLicensePlate() }
            launch { observeMeterDeviceId() }
        }
        checkForMostRelevantOTAUpdate()
        writeUpdateStatus(mostRecentlyCompletedUpdateId)
        isInitialized = true
        Log.d(TAG, "DashManager initialized")
    }

    private fun writeUpdateStatus(mostRecentlyCompletedUpdateId: String?) {
        externalScope?.launch {
            if (!mostRecentlyCompletedUpdateId.isNullOrBlank()) {
                val updatesCollection = getMeterDocument()
                    .collection(UPDATES_COLLECTION)

                updatesCollection
                    .document(mostRecentlyCompletedUpdateId)
                    .update("status", UpdateStatus.COMPLETE.name)
                    .addOnSuccessListener {
                        Log.d(TAG, "writeUpdateStatus COMPLETE successfully")
                    }
                    .addOnFailureListener {
                        Log.e(TAG, "writeUpdateStatus COMPLETE error", it)
                    }
            }
        }
    }

    private suspend fun observeMeterDeviceId() {
        var isFirstFetch = true
        /*
        * // Flag to check if it's the first fetch
        *    - we don't need to update the flow on the first fetch because we only need the flow to set up garage app
        *    - so we will wait for changes from the garage app to the document first and ignore the first fetch
        * */
        dashManagerConfig.deviceID.collectLatest { deviceID ->
            if (deviceID.isEmpty()) return@collectLatest
            meterDevicesDocumentListener?.remove() // Remove any previous listener

            meterDevicesDocumentListener = getMeterDevicesDocument().addSnapshotListener { snapshot, e ->
                if (e != null) {
                    return@addSnapshotListener
                }

                val data = snapshot?.data
                if (snapshot != null && data != null) {
                    if (isFirstFetch) {
                        isFirstFetch = false
                        return@addSnapshotListener
                    }
                    val json = gson.toJson(snapshot.data)
                    val meterDeviceProperties = gson.fromJson(json, MeterDeviceProperties::class.java)
                    _meterDeviceProperties.value = meterDeviceProperties
                    Log.d(TAG, "observeMeterDeviceId successfully")
                }
            }
        }
    }

    fun healthCheckApprovedAndLicensePlateSet() {
        externalScope?.launch(ioDispatcher + exceptionHandler) {
            _meterDeviceProperties.value = null // Reset the meter device properties flow

            getMeterDevicesDocument()
                .set(
                    mapOf("health_check_status" to HealthCheckStatus.LICENSE_PLATE_SET.name),
                    SetOptions.merge()
                )
        }
    }

    fun performHealthCheck() {
        externalScope?.launch(ioDispatcher + exceptionHandler) {
            val env = when(env) {
                "dev" -> "DEV"
                "dev2" -> "DEV2"
                "qa" -> "QA"
                "prd" -> "PRD"
                else -> "INVALID"
            }
            val location = dashManagerConfig.meterLocation
            val gpsType = location.value.gpsType.toString()
            val map = mapOf(
                "health_check_status" to HealthCheckStatus.UPDATED.name,
                "gps_type" to gpsType,
                "location" to location.value.geoPoint,
                "env" to env
            )
            getMeterDevicesDocument()
                .set(map, SetOptions.merge())
                .addOnSuccessListener {
                    Log.d(TAG, "performHealthCheck successfully")
                }
                .addOnFailureListener {
                    Log.e(TAG, "performHealthCheck error", it)
                }
        }
    }

    private fun setMeterInfoToSettings() {
        externalScope?.launch(ioDispatcher + exceptionHandler) {
            getMeterDocument()
                .update(
                    FieldPath.of("settings", "meter_software_version"), DashManagerConfig.meterSoftwareVersion,
                    FieldPath.of("settings", "sim_iccid"), DashManagerConfig.simIccId
                )
                .addOnSuccessListener {
                    Log.d(TAG, "setMeterInfoToSettings successfully")
                }
                .addOnFailureListener {
                    Log.e(TAG, "setMeterInfoToSettings error", it)
                }
        }
    }

    private suspend fun observeMeterLicensePlate() {
        Log.d(TAG, "observeMeterLicensePlate")
        dashManagerConfig.meterIdentifier.collectLatest {
            Log.d(TAG, "observeMeterLicensePlate - meterIdentifier: $it")
            meterDocumentListener?.remove() // Remove the previous listener
            Log.d(TAG, "meter document path ${getMeterDocument().path}")
            meterDocumentListener = getMeterDocument()
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        Log.d(TAG, "observeMeterLicensePlate error", e)
                        return@addSnapshotListener
                    }

                    if (snapshot != null && snapshot.exists()) {
                        val settings = parseSettings(snapshot)
                        val mcuInfo = parseMcuInfo(snapshot)
                        val session = parseSession(snapshot)

                        if (snapshot.contains(LOCKED_AT)) {
                            val lockedAt = snapshot.get(LOCKED_AT)
                            if (lockedAt == null) {
                                // The field exists in the document and is explicitly set to null
                                _remoteUnlockMeter.value = true
                            }
                        }

                        _meterFields.value = MeterFields(
                            settings = settings,
                            session = session,
                            mcuInfo = mcuInfo,
                        )
                        Log.d(TAG, " successfully - showLoginToggle value: ${settings?.showLoginToggle} - showConnectionIconsToggle val ${settings?.showConnectionIconsToggle}")
                    }
                }
        }
    }

    fun clearDriverSession() {
        externalScope?.launch(ioDispatcher + exceptionHandler) {
            val currentSessionId = _meterFields.value?.session?.sessionId
            if (currentSessionId != null) {
                updateSessionEndTime(currentSessionId, Timestamp.now())
                val deleteSessionMap = mapOf(SESSION to FieldValue.delete())
                getMeterDocument().update(deleteSessionMap)
            }
        }
    }

    private fun updateSessionEndTime(sessionId: String, endTime: Timestamp) {
        firestore.collection("sessions")
            .document(sessionId)
            .update(
                "end_time", endTime
            )
    }

    fun isMCUParamsUpdateRequired() {
        externalScope?.launch(ioDispatcher + exceptionHandler) {
            _mcuParamsUpdateRequired.value = null
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
                        json =
                            json.substring(0, json.length - 1) + ",\"id\":\"${latestDocument.id}\"}"
                        val update = gson.fromJson(json, UpdateMCUParamsRequest::class.java)

                        if (!update.isCompleted()) {
                            _mcuParamsUpdateRequired.value = update
                        }
                        Log.d(TAG, "isMCUParamsUpdateRequired $json")
                    }
                }
        }
    }

    fun setMCUParamsUpdateComplete(updateRequest: UpdateMCUParamsRequest) {
        externalScope?.launch(ioDispatcher + exceptionHandler) {
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
        externalScope?.launch(ioDispatcher + exceptionHandler) {
            firestore.collection(CONFIGURATIONS_COLLECTION)
                .document(METER_SDK_DOCUMENT)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        return@addSnapshotListener
                    }

                    if (snapshot != null && snapshot.exists()) {
                        val meterSdkConfigJson = gson.toJson(snapshot.data)
                        _meterSdkConfig.value =
                            gson.fromJson(meterSdkConfigJson, MeterSdkConfiguration::class.java)
                    }
                }
        }
    }

    fun resetUnlockMeterStatusInRemote() {
        externalScope?.launch(ioDispatcher + exceptionHandler) {
            // remove the unlock meter flag
            getMeterDocument()
                .set(mapOf(LOCKED_AT to FieldValue.delete()), SetOptions.merge())
            _remoteUnlockMeter.value = false
        }
    }

    fun setFirestoreTripDocumentListener(tripId: String) {
        externalScope?.launch(ioDispatcher + exceptionHandler) {
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
                        Log.d(TAG, "listening to trip: ${trip.tripId}")
                    }
                }
        }
    }

    fun writeLockMeter(isAbormalPulse: Boolean) {
        externalScope?.launch(ioDispatcher + exceptionHandler) {
            val lockedAt = Timestamp.now()
            getMeterDocument()
                .set(mapOf(LOCKED_AT to lockedAt), SetOptions.merge())

            // write to logging collection
            val logMap = mapOf(
                LogConstant.CREATED_BY to LogConstant.CABLE_METER,
                LogConstant.ACTION to LogConstant.ACTION_METER_LOCKED,
                LogConstant.SERVER_TIME to FieldValue.serverTimestamp(),
                LogConstant.DEVICE_TIME to Timestamp.now(),
                LogConstant.LOCK_TYPE to if (isAbormalPulse) LogConstant.LOCK_TYPE_ABNORMAL_PULSE else LogConstant.LOCK_TYPE_ABNORMAL_OVERSPEED
            )
            writeToLoggingCollection(logMap)

        }
    }

    fun endTripDocumentListener() {
        tripDocumentListener?.remove()
        tripDocumentListener = null
        _tripInFirestore.value = null
        Log.i(TAG, "endTripDocumentListener")
    }

    fun createTripOnFirestore(trip: MeterTripInFirestore) {
        externalScope?.launch(ioDispatcher + exceptionHandler) {
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
            val sessionId = _meterFields.value?.session?.sessionId
            val driver = _meterFields.value?.session?.driver

            val updatedTrip = trip.copy(
                dashFeeRate = applicableFeeRate,
                dashFeeConstant = applicableFeeConstant,
                session = if (!sessionId.isNullOrBlank()) TripSession(sessionId) else null,
                driver = driver,
                creationTime = Timestamp.now(),
                locationStart = dashManagerConfig.meterLocation.value.geoPoint,
                licensePlate = dashManagerConfig.meterIdentifier.first(),
                meterSoftwareVersion = DashManagerConfig.Companion.meterSoftwareVersion,
                meterId = dashManagerConfig.deviceID.first()
            )
            updateTripOnFirestore(updatedTrip)
        }
    }

    fun setMCUInfoOnFirestore(mcuInfo: McuInfo) {
        externalScope?.launch(ioDispatcher + exceptionHandler) {
            val json = gson.toJson(mcuInfo.copy(
                updatedAt = Timestamp.now(),
                status = McuInfoStatus.UPDATED
            ))
            val map = (gson.fromJson(json, Map::class.java) as Map<String, Any?>).toFirestoreFormat()

            getMeterDocument()
                .set(mapOf(MCU_INFO to map), SetOptions.merge())
        }
    }


    fun updateTripOnFirestore(trip: MeterTripInFirestore) {
        externalScope?.launch(ioDispatcher + exceptionHandler) {
            val updatedTrip = trip.copy(
                lastUpdateTime = Timestamp.now(),
                locationEnd = if (trip.endTime != null) dashManagerConfig.meterLocation.value.geoPoint else null
            )
            val json = gson.toJson(updatedTrip)
            val map =
                (gson.fromJson(json, Map::class.java) as Map<String, Any?>).toFirestoreFormat()

            val tripsCollection = getMeterDocument()
                .collection(TRIPS_COLLECTION)

            tripsCollection
                .document(updatedTrip.tripId)
                .set(map, SetOptions.merge())
                .addOnSuccessListener {
                    Log.d(TAG, "updateTripOnFirestore successfully")
                    // Create an audit trail entry for tracking purposes
                    createAuditTrailEntry(tripId = updatedTrip.tripId, updatedTripMap = map)
                }
                .addOnFailureListener {
                    Log.e(TAG, "updateTripOnFirestore error", it)
                }
        }
    }

    private fun createAuditTrailEntry(tripId: String, updatedTripMap: Map<String, Any?>) {
        val auditTrailCollection = getMeterDocument()
            .collection(TRIPS_COLLECTION)
            .document(tripId)
            .collection(AUDIT_COLLECTION)

        val auditTrailEntry = updatedTripMap.toMutableMap().apply {
            put("audit_time", Timestamp.now())
            put("updated_by", "Meter")
        }.toFirestoreFormat()

        // Add an audit trail document
        auditTrailCollection.add(auditTrailEntry)
            .addOnSuccessListener {
                Log.d(TAG, "Audit trail entry created successfully")
            }
            .addOnFailureListener {
                Log.e(TAG, "Error creating audit trail entry", it)
            }
    }

    fun writeToLoggingCollection(log: Map<String, Any?>) {
        val map =  log.toFirestoreFormat()
        externalScope?.launch(ioDispatcher + exceptionHandler) {
            val loggingCollection = getMeterDocument()
                .collection(LOGGING_COLLECTION)

            loggingCollection
                .add(map)
                .addOnSuccessListener {
                    Log.d(TAG, "writeToLoggingsCollection successfully")
                }
                .addOnFailureListener {
                    Log.e(TAG, "writeToLoggingsCollection error", it)
                }
        }
    }

    private fun checkForMostRelevantOTAUpdate() {
        // apk or firmware updates
        externalScope?.launch(ioDispatcher + exceptionHandler) {
            _mostRelevantUpdate.value = null
            val updatesCollection = getMeterDocument()
                .collection(UPDATES_COLLECTION)

            updatesCollection
                .orderBy(CREATED_ON, Query.Direction.DESCENDING)
                .limit(3)
                .get(Source.SERVER)
                .addOnSuccessListener { snapshot ->
                    if (snapshot != null && !snapshot.isEmpty) {
                        val firmwareUpdate = snapshot.documents.firstNotNullOfOrNull { document ->
                            var json = gson.toJson(document.data)
                            // Manually add the document ID to the JSON string
                            json = json.substring(0, json.length - 1) + ",\"id\":\"${document.id}\"}"
                            val update = gson.fromJson(json, Update::class.java)
                            if (update.type == OTA_FIRMWARE_TYPE && update.shouldPrompt()) update else null
                        }

                        val otherUpdate = snapshot.documents.firstNotNullOfOrNull { document ->
                            var json = gson.toJson(document.data)
                            // Manually add the document ID to the JSON string
                            json = json.substring(0, json.length - 1) + ",\"id\":\"${document.id}\"}"
                            val update = gson.fromJson(json, Update::class.java)
                            if (update.shouldPrompt()) update else null
                        }
                        val filteredDocument = firmwareUpdate ?: otherUpdate // firmware update should be first (DASH 2292)

                        if (filteredDocument != null) {
                            _mostRelevantUpdate.value = filteredDocument
                            Log.d(TAG, "checkForUpdates ${gson.toJson(filteredDocument)}")
                        }
                        else {
                            Log.d(TAG, "No matching updates found.")
                        }
                    }
                    Log.d(TAG, "checkForUpdates addOnSuccessListener")
                }
                .addOnFailureListener {
                    Log.e(TAG, "checkForUpdates error", it)
                }
        }
    }

    fun writeUpdateResult(update: Update) {
        externalScope?.launch(ioDispatcher + exceptionHandler) {
            val json = gson.toJson(update)
            val map = (gson.fromJson(json, Map::class.java) as Map<String, Any?>).toFirestoreFormat()

            val updatesCollection = getMeterDocument()
                .collection(UPDATES_COLLECTION)

            updatesCollection
                .document(update.id)
                .set(map, SetOptions.merge())
                .addOnSuccessListener {
                    Log.d(TAG, "updateMostRelevantUpdate successfully")
                }
                .addOnFailureListener {
                    Log.e(TAG, "updateMostRelevantUpdate error", it)
                }
        }
    }

    fun sendHeartbeat() {
        externalScope?.launch(ioDispatcher + exceptionHandler) {
            val meterLocation = dashManagerConfig.meterLocation.value
            val isDeviceAsleep = dashManagerConfig.isDeviceAsleep.value
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
                speed = speed,
                serverTime = Timestamp.now(), // Server time is actually set by the .toFirestoreFormat extension
                meterSoftwareVersion = DashManagerConfig.meterSoftwareVersion,
                deviceAccStatus = if (isDeviceAsleep) ACC_STATUS_ASLEEP else ACC_STATUS_AWAKE
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

    /**
     * update the value of triggerLogUpload in the settings of the meter document
     */
    fun setTriggerLogUpload(booleanValue: Boolean) {
        externalScope?.launch(ioDispatcher + exceptionHandler) {
            val map = mapOf(SETTINGS to mapOf(TRIGGER_LOG_UPLOAD to booleanValue))
            getMeterDocument()
                .set(map, SetOptions.merge())
        }
    }

    private fun getMeterDocument() = metersCollection.document(dashManagerConfig.meterIdentifier.value)

    private fun getMeterDevicesDocument() = meterDevicesCollection.document(dashManagerConfig.deviceID.value)

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
        val licensePlate = sessionMap?.get("license_plate") as? String

        return sessionId?.let {
            driverMap?.let { driverData ->
                val driver = parseDriver(driverData)
                Session(sessionId = it, driver = driver, licensePlate = licensePlate ?: "")
            }
        }
    }

    private fun parseDriver(driverMap: Map<String, *>): Driver {
        return Driver(
            driverPhoneNumber = driverMap[DRIVER_ID] as String,
            driverName = driverMap[DRIVER_NAME] as String,
            driverChineseName = driverMap[DRIVER_NAME_CH] as? String ?: "",
            driverLicense = driverMap[DRIVER_LICENSE] as String
        )
    }

    fun onCleared() {
        meterDocumentListener?.remove()
        tripDocumentListener?.remove()
        meterDevicesDocumentListener?.remove()
        meterDevicesDocumentListener = null
        meterDocumentListener = null
        tripDocumentListener = null
    }

    companion object {
        private const val TAG = "DashManager"
        private const val SETTINGS = "settings"
        private const val MCU_INFO = "mcu_info"
        private const val SESSION = "session"
        private const val SESSION_ID = "id"
        private const val DRIVER = "driver"
        private const val DRIVER_ID = "id"
        private const val DRIVER_NAME = "name"
        private const val DRIVER_NAME_CH = "name_ch"
        private const val DRIVER_LICENSE = "driver_license"
        private const val LOCKED_AT = "locked_at"
        private const val TRIGGER_LOG_UPLOAD = "trigger_log_upload"
        var isInitialized = false
        private const val ACC_STATUS_ASLEEP = "ASLEEP"
        private const val ACC_STATUS_AWAKE = "AWAKE"
    }
}