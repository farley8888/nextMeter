package com.vismo.nextgenmeter.repository

import android.util.Log
import com.google.firebase.Timestamp
import com.ilin.util.ShellUtils
import com.vismo.nextgenmeter.BuildConfig
import com.vismo.nextgenmeter.datastore.DeviceDataStore
import com.vismo.nextgenmeter.model.McuInfoStatus
import com.vismo.nextgenmeter.model.MeterInfo
import com.vismo.nextgenmeter.model.format
import com.vismo.nxgnfirebasemodule.DashManager
import com.vismo.nxgnfirebasemodule.DashManagerConfig
import com.vismo.nxgnfirebasemodule.model.McuInfo
import com.vismo.nxgnfirebasemodule.model.MeterSdkConfiguration
import com.vismo.nxgnfirebasemodule.model.Update
import com.vismo.nxgnfirebasemodule.model.UpdateMCUParamsRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

class RemoteMeterControlRepositoryImpl @Inject constructor(
    private val dashManager: DashManager,
    private val measureBoardRepository: MeasureBoardRepository,
    private val logShippingRepository: LogShippingRepository,
    private val meterPreferenceRepository: MeterPreferenceRepository
) : RemoteMeterControlRepository {

    private val TAG = "RemoteMeterControlRepositoryImpl"
    private val _meterInfo = MutableStateFlow<MeterInfo?>(null)
    override val meterInfo: StateFlow<MeterInfo?> = _meterInfo

    private val _heartBeatInterval = MutableStateFlow(DEFAULT_HEARTBEAT_INTERVAL)
    override val heartBeatInterval: StateFlow<Int> = _heartBeatInterval

    override val meterDeviceProperties = dashManager.meterDeviceProperties
    override val meterIdentifier = measureBoardRepository.meterIdentifierInRemote
    override val meterSdkConfiguration = dashManager.meterSdkConfig

    override val remoteUpdateRequest: StateFlow<Update?> = dashManager.mostRelevantUpdate

    private var externalScope: CoroutineScope? = null

    override suspend fun initDashManager(scope: CoroutineScope) {
        DashManagerConfig.simIccId = getICCID() ?: ""
        DashManagerConfig.meterSoftwareVersion = BuildConfig.VERSION_NAME + "." + BuildConfig.VERSION_CODE
        val mostRecentlyCompletedUpdateId = meterPreferenceRepository.getRecentlyCompletedUpdateId().firstOrNull()
        dashManager.init(scope, mostRecentlyCompletedUpdateId = mostRecentlyCompletedUpdateId)
        if (!mostRecentlyCompletedUpdateId.isNullOrBlank()) {
            // this is to ensure that the most recently completed update is written to firestore
            measureBoardRepository.enquireParameters()
        }
        meterPreferenceRepository.saveRecentlyCompletedUpdateId("") // clear the recently completed update id
    }

    private fun getICCID(): String? {
        val iccid = ShellUtils.execShellCmd("getprop ril.yuga.iccid")
        return iccid
    }

    override fun observeFlows(scope: CoroutineScope) {
        externalScope = scope
        externalScope?.launch {
            launch {
                DeviceDataStore.mcuPriceParams.collect { mcuParams ->
                    mcuParams?.let {
                        val mcuInfo: McuInfo = dashManager.convertToType(it.format())
                        dashManager.setMCUInfoOnFirestore(mcuInfo)
                    }
                }
            }

            launch {
                dashManager.mcuParamsUpdateRequired.collectLatest { updateRequest ->
                    updateRequest?.let {
                        if (it.completedOn == null) {
                            measureBoardRepository.updateKValue(it.kValue)
                            val completedRequest = UpdateMCUParamsRequest(
                                id = it.id,
                                createdOn = it.createdOn,
                                kValue = it.kValue,
                                completedOn = Timestamp.now(),
                            )
                            dashManager.setMCUParamsUpdateComplete(completedRequest)
                        }
                    }
                }
            }

            launch {
                dashManager.meterFields.collectLatest {
                    it?.let { meterFields ->
                        val meterInfo: MeterInfo = dashManager.convertToType(meterFields)
                        _meterInfo.value = meterInfo

                        if (meterInfo.settings?.heartbeatInterval != _heartBeatInterval.value)
                            _heartBeatInterval.value = meterInfo.settings?.heartbeatInterval ?: DEFAULT_HEARTBEAT_INTERVAL

                        // Trigger log shipping if needed
                        if (meterInfo.settings?.triggerLogUpload == true) {
                            triggerLogUpload()
                        }

                        if (meterInfo.mcuInfo?.status == McuInfoStatus.REQUESTED) {
                            measureBoardRepository.enquireParameters()
                        }
                    }
                }
            }
        }
    }

    private fun triggerLogUpload() {
        Log.d(TAG, "triggerLogUpload")
        externalScope?.launch {
            val logShippingResult = logShippingRepository.handleLogUploadFlow()
            if (logShippingResult.isSuccess) {
                val count = logShippingResult.getOrNull() ?: 0
                Log.d(TAG, "Log upload success with $count files.")
            } else {
                Log.d(TAG, "Log upload failed")
            }
            // Set it back to false after log upload regardless of success or failure
            dashManager.setTriggerLogUpload(false)
        }
    }

    override fun updateLicensePlateAndKValue(licensePlate: String, kValue: String) {
        externalScope?.launch {
            measureBoardRepository.updateKValue(kValue.toInt())
            delay(3000L) // seems like this delay is necessary for the measure board to process the kValue update
            measureBoardRepository.updateLicensePlate(licensePlate)
            healthCheckApprovedAndLicensePlateSet()
        }
    }

    override fun writeUpdateResultToFireStore(update: Update) {
        dashManager.writeUpdateResult(update)
    }


    private fun healthCheckApprovedAndLicensePlateSet() {
        dashManager.healthCheckApprovedAndLicensePlateSet()
    }

    override fun performHealthCheck() {
        dashManager.performHealthCheck()
    }

    override fun sendHeartBeat() {
        dashManager.sendHeartbeat()
    }

    override fun clearDriverSession() {
        dashManager.clearDriverSession()
    }

    override fun onCleared() {
        dashManager.onCleared()
    }

    override fun remoteUpdateKValue() {
        dashManager.isMCUParamsUpdateRequired()
    }

    override fun requestPatchFirmwareToMCU(fileName: String) {
        externalScope?.launch {
            measureBoardRepository.requestPatchFirmware(fileName)
        }
    }

    override suspend fun saveRecentlyCompletedUpdateId(id: String) {
        meterPreferenceRepository.saveRecentlyCompletedUpdateId(id = id)
    }

    override fun writeToLoggingCollection(log: Map<String, Any?>) {
        dashManager.writeToLoggingCollection(log)
    }


    companion object {
        private const val DEFAULT_HEARTBEAT_INTERVAL = 5
    }
}