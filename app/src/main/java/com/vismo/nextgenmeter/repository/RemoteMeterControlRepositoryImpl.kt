package com.vismo.nextgenmeter.repository

import com.google.firebase.Timestamp
import com.vismo.nextgenmeter.datastore.DeviceDataStore
import com.vismo.nextgenmeter.model.MeterInfo
import com.vismo.nextgenmeter.module.IoDispatcher
import com.vismo.nxgnfirebasemodule.DashManager
import com.vismo.nxgnfirebasemodule.model.McuInfo
import com.vismo.nxgnfirebasemodule.model.Update
import com.vismo.nxgnfirebasemodule.model.UpdateMCUParamsRequest
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
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
) : RemoteMeterControlRepository {

    private val _meterInfo = MutableStateFlow<MeterInfo?>(null)
    override val meterInfo: StateFlow<MeterInfo?> = _meterInfo

    private val _heartBeatInterval = MutableStateFlow(DEFAULT_HEARTBEAT_INTERVAL)
    override val heartBeatInterval: StateFlow<Int> = _heartBeatInterval

    override val meterDeviceProperties = dashManager.meterDeviceProperties
    override val meterIdentifier = measureBoardRepository.meterIdentifierInRemote

    override val remoteUpdateRequest = dashManager.mostRelevantUpdate

    override fun initDashManager() {
        dashManager.init()
    }

    override fun observeFlows() {
        CoroutineScope(ioDispatcher).launch {
            launch {
                DeviceDataStore.mcuPriceParams.collectLatest { mcuParams ->
                    mcuParams?.let {
                        val mcuInfo: McuInfo = dashManager.convertToType(it)
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
                    }
                }
            }
        }
    }

    override fun updateLicensePlateAndKValue(licensePlate: String, kValue: String) {
        CoroutineScope(ioDispatcher).launch {
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