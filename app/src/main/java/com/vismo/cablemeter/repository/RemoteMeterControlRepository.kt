package com.vismo.cablemeter.repository

import com.vismo.cablemeter.model.MeterInfo
import kotlinx.coroutines.flow.StateFlow

interface RemoteMeterControlRepository {
    val meterInfo: StateFlow<MeterInfo?>

    fun observeFlows()
}