package com.vismo.cablemeter.datastore

import com.vismo.cablemeter.model.DeviceIdData
import com.vismo.cablemeter.model.MCUFareParams
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object MCUParamsDataStore {
    private val _mcuFareParams = MutableStateFlow<MCUFareParams?>(null)
    val mcuPriceParams: StateFlow<MCUFareParams?> = _mcuFareParams

    private val _deviceIdData = MutableStateFlow<DeviceIdData?>(null)
    val deviceIdData = _deviceIdData

    private val _mcuTime = MutableStateFlow<String?>(null)
    val mcuTime: StateFlow<String?> = _mcuTime

    fun setMCUFareData(mcuData: MCUFareParams) {
        this._mcuFareParams.value = mcuData
    }

    fun setDeviceIdData(deviceIdData: DeviceIdData) {
        this._deviceIdData.value = deviceIdData
    }

    fun setMCUTime(mcuTime: String) {
        this._mcuTime.value = mcuTime
    }
}