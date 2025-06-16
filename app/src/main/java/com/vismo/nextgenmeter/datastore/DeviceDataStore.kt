package com.vismo.nextgenmeter.datastore

import android.util.Log
import com.vismo.nextgenmeter.model.DeviceIdData
import com.vismo.nextgenmeter.model.MCUFareParams
import com.vismo.nextgenmeter.service.DeviceGodCodeUnlockState
import com.vismo.nextgenmeter.service.StorageReceiverStatus
import com.vismo.nextgenmeter.service.USBReceiverStatus
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object DeviceDataStore {
    private val _mcuFareParams = MutableSharedFlow<MCUFareParams>(replay = 1)
    val mcuPriceParams: SharedFlow<MCUFareParams?> = _mcuFareParams.asSharedFlow()

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

    private val _toggleCommunicationWithMCU = MutableStateFlow(TOGGLE_COMMS_WITH_MCU.NONE)
    val toggleCommunicationWithMCU: StateFlow<TOGGLE_COMMS_WITH_MCU> = _toggleCommunicationWithMCU

    private val _isMCUHeartbeatActive = MutableStateFlow(false)
    val isMCUHeartbeatActive: StateFlow<Boolean> = _isMCUHeartbeatActive

    private val _isBusModelListenerDataReceived = MutableStateFlow(false)
    val isBusModelListenerDataReceived: StateFlow<Boolean> = _isBusModelListenerDataReceived

    private val _isFirmwareUpdateComplete = MutableStateFlow(false)
    val isFirmwareUpdateComplete: StateFlow<Boolean> = _isFirmwareUpdateComplete

    private val _isDeviceAsleep = MutableStateFlow(false)
    val isDeviceAsleep: StateFlow<Boolean> = _isDeviceAsleep

    private val mutex = Mutex() // Mutex for synchronization

    suspend fun setMCUFareData(mcuData: MCUFareParams) {
        mutex.withLock {
            this._mcuFareParams.emit(mcuData)
            Log.d("DeviceDataStore", "MCU Fare Params updated: $mcuData")
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

    fun setToggleCommunicationWithMCU() {
        this._toggleCommunicationWithMCU.value =
        when(this._toggleCommunicationWithMCU.value) {
            TOGGLE_COMMS_WITH_MCU.NONE -> TOGGLE_COMMS_WITH_MCU.TOGGLE_OFF
            TOGGLE_COMMS_WITH_MCU.TOGGLE_OFF -> TOGGLE_COMMS_WITH_MCU.TOGGLE_ON
            else -> TOGGLE_COMMS_WITH_MCU.NONE
        }
    }

    suspend fun setMCUHeartbeatActive(isActive: Boolean) {
        mutex.withLock {
            this._isMCUHeartbeatActive.value = isActive
        }
    }

    fun setBusModelListenerDataReceived(isActive: Boolean) {
        this._isBusModelListenerDataReceived.value = isActive
    }

    fun setFirmwareUpdateComplete(isComplete: Boolean) {
        this._isFirmwareUpdateComplete.value = isComplete
    }

    fun setIsDeviceAsleep(isAsleep: Boolean) {
        this._isDeviceAsleep.value = isAsleep
    }
}

enum class TOGGLE_COMMS_WITH_MCU {
    NONE,
    TOGGLE_OFF,
    TOGGLE_ON
}

