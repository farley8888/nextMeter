package com.vismo.nextgenmeter.datastore

import com.vismo.nextgenmeter.model.DeviceIdData
import com.vismo.nextgenmeter.model.MCUFareParams
import com.vismo.nextgenmeter.service.StorageReceiverStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object DeviceDataStore {
    private val _mcuFareParams = MutableStateFlow<MCUFareParams?>(null)
    val mcuPriceParams: StateFlow<MCUFareParams?> = _mcuFareParams

    private val _deviceIdData = MutableStateFlow<DeviceIdData?>(null)
    val deviceIdData = _deviceIdData

    private val _mcuTime = MutableStateFlow<String?>(null)
    val mcuTime: StateFlow<String?> = _mcuTime

    private val _storageReceiverStatus = MutableStateFlow<StorageReceiverStatus>(StorageReceiverStatus.Unknown)
    val storageReceiverStatus: StateFlow<StorageReceiverStatus> = _storageReceiverStatus

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

    fun setStorageReceiverStatus(status: StorageReceiverStatus) {
        this._storageReceiverStatus.value = status
    }
}
