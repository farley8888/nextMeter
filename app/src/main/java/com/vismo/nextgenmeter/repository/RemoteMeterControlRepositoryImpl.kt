package com.vismo.nextgenmeter.repository

import android.util.Log
import com.google.firebase.Timestamp
import com.ilin.util.ShellUtils
import com.vismo.nextgenmeter.BuildConfig
import com.vismo.nextgenmeter.datastore.DeviceDataStore
import com.vismo.nextgenmeter.model.MeterInfo
import com.vismo.nextgenmeter.model.format
import com.vismo.nextgenmeter.module.IoDispatcher
import com.vismo.nxgnfirebasemodule.DashManager
import com.vismo.nxgnfirebasemodule.DashManagerConfig
import com.vismo.nxgnfirebasemodule.model.McuInfo
import com.vismo.nxgnfirebasemodule.model.Update
import com.vismo.nxgnfirebasemodule.model.UpdateMCUParamsRequest
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

class RemoteMeterControlRepositoryImpl @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val dashManager: DashManager,
    private val measureBoardRepository: MeasureBoardRepository,
    private val logShippingRepository: LogShippingRepository,
) : RemoteMeterControlRepository {

    private val TAG = "RemoteMeterControlRepositoryImpl"
    private val _meterInfo = MutableStateFlow<MeterInfo?>(null)
    override val meterInfo: StateFlow<MeterInfo?> = _meterInfo

    private val _heartBeatInterval = MutableStateFlow(DEFAULT_HEARTBEAT_INTERVAL)
    override val heartBeatInterval: StateFlow<Int> = _heartBeatInterval

    override val meterDeviceProperties = dashManager.meterDeviceProperties
    override val meterIdentifier = measureBoardRepository.meterIdentifierInRemote

    override val remoteUpdateRequest = dashManager.mostRelevantUpdate

    private var externalScope: CoroutineScope? = null

    override fun initDashManager(scope: CoroutineScope) {
        DashManagerConfig.simIccId = getICCID() ?: ""
        DashManagerConfig.meterSoftwareVersion = BuildConfig.VERSION_NAME + "." + BuildConfig.VERSION_CODE
        dashManager.init(scope)
    }

    private fun getICCID(): String? {
        val iccid = ShellUtils.execShellCmd("getprop ril.yuga.iccid")
        return iccid
    }

    override fun observeFlows(scope: CoroutineScope) {
        externalScope = scope
        externalScope?.launch {
            launch {
                DeviceDataStore.mcuPriceParams.collectLatest { mcuParams ->
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
                    }
                }
            }
        }
    }

    private suspend fun triggerLogUpload() {
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
            resetMeterDevicesFlow()
        }
    }

    override fun writeUpdateResultToFireStore(update: Update) {
        dashManager.writeUpdateResult(update)
    }


    private fun resetMeterDevicesFlow() {
        dashManager.resetMeterDeviceProperties()
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


    companion object {
        private const val DEFAULT_HEARTBEAT_INTERVAL = 5
    }
}