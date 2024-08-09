package com.vismo.cablemeter.datastore

import com.vismo.cablemeter.model.MCUParams
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object MCUParamsDataStore {
    private val _mcuParams = MutableStateFlow<MCUParams?>(null)
    val mcuParams: StateFlow<MCUParams?> = _mcuParams

    fun setMCUData(mcuData: MCUParams) {
        this._mcuParams.value = mcuData
    }

    fun clearMCUData() {
        this._mcuParams.value = null
    }
}