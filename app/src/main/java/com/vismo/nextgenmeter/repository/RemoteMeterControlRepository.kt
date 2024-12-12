package com.vismo.nextgenmeter.repository

import com.vismo.nextgenmeter.model.MeterInfo
import com.vismo.nxgnfirebasemodule.model.MeterDeviceProperties
import com.vismo.nxgnfirebasemodule.model.Update
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

interface RemoteMeterControlRepository {
    val meterInfo: StateFlow<MeterInfo?>
    val heartBeatInterval: StateFlow<Int>
    val meterDeviceProperties: StateFlow<MeterDeviceProperties?>
    val meterIdentifier: StateFlow<String>
    val remoteUpdateRequest: StateFlow<Update?>

    fun initDashManager(scope: CoroutineScope)

    fun observeFlows(scope: CoroutineScope)

    fun sendHeartBeat()

    fun onCleared()

    fun clearDriverSession()

    fun performHealthCheck()

    fun updateLicensePlateAndKValue(licensePlate: String, kValue: String)

    fun writeUpdateResultToFireStore(update: Update)

    fun remoteUpdateKValue()

}