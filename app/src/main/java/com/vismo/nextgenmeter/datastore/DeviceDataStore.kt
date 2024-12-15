package com.vismo.nextgenmeter.datastore

import com.vismo.nextgenmeter.model.DeviceIdData
import com.vismo.nextgenmeter.model.MCUFareParams
import com.vismo.nextgenmeter.service.DeviceGodCodeUnlockState
import com.vismo.nextgenmeter.service.StorageReceiverStatus
import com.vismo.nextgenmeter.service.USBReceiverStatus
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

    private val _usbReceiverStatus = MutableStateFlow<USBReceiverStatus>(USBReceiverStatus.Unknown)
    val usbReceiverStatus: StateFlow<USBReceiverStatus> = _usbReceiverStatus

    private val _deviceGodCodeUnlockState = MutableStateFlow<DeviceGodCodeUnlockState>(DeviceGodCodeUnlockState.Locked)
    val deviceGodCodeUnlockState: StateFlow<DeviceGodCodeUnlockState> = _deviceGodCodeUnlockState

    private val _clearCacheOfApplication = MutableStateFlow(false)
    val clearCacheOfApplication: StateFlow<Boolean> = _clearCacheOfApplication

    private val _reinitMCURepository = MutableStateFlow(false)
    val reinitMCURepository: StateFlow<Boolean> = _reinitMCURepository

    private val _isMCUHeartbeatActive = MutableStateFlow(false)
    val isMCUHeartbeatActive: StateFlow<Boolean> = _isMCUHeartbeatActive

    private val _isBusModelListenerDataReceived = MutableStateFlow(false)
    val isBusModelListenerDataReceived: StateFlow<Boolean> = _isBusModelListenerDataReceived

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

    fun setUSBReceiverStatus(status: USBReceiverStatus) {
        this._usbReceiverStatus.value = status
    }

    fun setDeviceGodCodeUnlockState(state: DeviceGodCodeUnlockState) {
        this._deviceGodCodeUnlockState.value = state
    }

    fun setClearCacheOfApplication(clearCache: Boolean) {
        this._clearCacheOfApplication.value = clearCache
    }

    fun setReinitMCURepository(reinit: Boolean) {
        this._reinitMCURepository.value = reinit
    }

    suspend fun setMCUHeartbeatActive(isActive: Boolean) {
        mutex.withLock {
            this._isMCUHeartbeatActive.value = isActive
        }
    }

    fun setBusModelListenerDataReceived(isActive: Boolean) {
        this._isBusModelListenerDataReceived.value = isActive
    }
}
