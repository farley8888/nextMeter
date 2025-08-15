package com.vismo.nextgenmeter.repository

import com.google.android.gms.tasks.Task
import com.google.firebase.Timestamp
import com.vismo.nextgenmeter.model.MeterInfo
import com.vismo.nxgnfirebasemodule.model.MeterDeviceProperties
import com.vismo.nxgnfirebasemodule.model.MeterSdkConfiguration
import com.vismo.nxgnfirebasemodule.model.Update
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

interface RemoteMeterControlRepository {
    val meterInfo: StateFlow<MeterInfo?>
    val heartBeatInterval: StateFlow<Int>
    val meterDeviceProperties: StateFlow<MeterDeviceProperties?>
    val meterIdentifier: StateFlow<String>
    val meterSdkConfiguration: StateFlow<MeterSdkConfiguration?>
    val remoteUpdateRequest: StateFlow<Update?>

    suspend fun initDashManager(scope: CoroutineScope)

    fun observeFlows(scope: CoroutineScope)

    fun sendHeartBeat()

    fun onCleared()

    fun clearDriverSession()

    fun performHealthCheck()

    fun updateLicensePlateAndKValue(licensePlate: String, kValue: String)

    fun writeUpdateResultToFireStore(update: Update): Task<Void>

    fun remoteUpdateKValue()

    fun requestPatchFirmwareToMCU(fileName: String)

    suspend fun saveRecentlyCompletedUpdateId(id: String)

    fun write4GModuleRestarting(timestamp: Long, reason: String)

    fun writeToLoggingCollection(map: Map<String, Any>)
}