package com.vismo.cablemeter.repository

import com.vismo.cablemeter.model.MeterInfo
import com.vismo.nxgnfirebasemodule.model.MeterDeviceProperties
import kotlinx.coroutines.flow.StateFlow

interface RemoteMeterControlRepository {
    val meterInfo: StateFlow<MeterInfo?>
    val heartBeatInterval: StateFlow<Int>
    val meterDeviceProperties: StateFlow<MeterDeviceProperties?>
    val meterIdentifier: StateFlow<String>

    fun observeFlows()

    fun sendHeartBeat()

    fun onCleared()

    fun clearDriverSession()

    fun performHealthCheck()

    fun updateLicensePlateAndKValue(licensePlate: String, kValue: String)
}