package com.vismo.cablemeter.repository

import com.vismo.cablemeter.model.MeterInfo
import kotlinx.coroutines.flow.StateFlow

interface RemoteMeterControlRepository {
    val meterInfo: StateFlow<MeterInfo?>
    val heartBeatInterval: StateFlow<Int>

    fun observeFlows()

    fun sendHeartBeat()

    fun onCleared()

    fun clearDriverSession()
}