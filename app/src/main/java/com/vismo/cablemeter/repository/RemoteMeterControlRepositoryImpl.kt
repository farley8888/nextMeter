package com.vismo.cablemeter.repository

import com.google.firebase.Timestamp
import com.vismo.cablemeter.datastore.MCUParamsDataStore
import com.vismo.cablemeter.model.MeterInfo
import com.vismo.cablemeter.module.IoDispatcher
import com.vismo.nxgnfirebasemodule.DashManager
import com.vismo.nxgnfirebasemodule.model.McuInfo
import com.vismo.nxgnfirebasemodule.model.UpdateMCUParamsRequest
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

class RemoteMeterControlRepositoryImpl @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val dashManager: DashManager,
    private val measureBoardRepository: MeasureBoardRepository
) : RemoteMeterControlRepository {

    val mcuParams = MCUParamsDataStore.mcuParams

    private val _meterInfo = MutableStateFlow<MeterInfo?>(null)
    override val meterInfo: StateFlow<MeterInfo?> = _meterInfo

    private val _heartBeatInterval = MutableStateFlow(DEFAULT_HEARTBEAT_INTERVAL)
    override val heartBeatInterval: StateFlow<Int> = _heartBeatInterval

    override fun observeFlows() {
        CoroutineScope(ioDispatcher).launch {
            launch {
                mcuParams.collectLatest { mcuParams ->
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

                        if (meterInfo.settings.heartbeatInterval != _heartBeatInterval.value)
                            _heartBeatInterval.value = meterInfo.settings.heartbeatInterval
                    }
                }
            }
        }
    }

    override fun sendHeartBeat() {
        dashManager.sendHeartbeat()
    }

    override fun onCleared() {
        dashManager.onCleared()
    }


    companion object {
        private const val DEFAULT_HEARTBEAT_INTERVAL = 5
    }
}