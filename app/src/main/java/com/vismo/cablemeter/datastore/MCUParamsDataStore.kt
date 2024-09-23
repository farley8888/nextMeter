package com.vismo.cablemeter.datastore

import com.vismo.cablemeter.model.DeviceIdData
import com.vismo.cablemeter.model.MCUFareParams
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object MCUParamsDataStore {
    private val _mcuFareParams = MutableStateFlow<MCUFareParams?>(null)
    val mcuPriceParams: StateFlow<MCUFareParams?> = _mcuFareParams

    private val _deviceIdData = MutableStateFlow<DeviceIdData?>(null)
    val deviceIdData = _deviceIdData

    private val _mcuTime = MutableStateFlow<String?>(null)
    val mcuTime: StateFlow<String?> = _mcuTime

    private val mutex = Mutex() // Mutex for synchronization

    suspend fun setMCUFareData(mcuData: MCUFareParams) {
        mutex.withLock {
            this._mcuFareParams.value = mcuData
        }
    }

    suspend fun setDeviceIdData(deviceIdData: DeviceIdData) {
        mutex.withLock {
            this._deviceIdData.value = deviceIdData
        }
    }

    suspend fun setMCUTime(mcuTime: String) {
        mutex.withLock {
            this._mcuTime.value = mcuTime
        }
    }
}
