package com.vismo.cablemeter.repository

import com.vismo.cablemeter.model.DeviceIdData
import kotlinx.coroutines.flow.StateFlow

interface MeasureBoardRepository {
    val mcuTime: StateFlow<String?>
    val deviceIdData: StateFlow<DeviceIdData?>

    fun writeStartTripCommand(tripId: String)

    fun writeStartAndPauseTripCommand(tripId: String)

    fun writeEndTripCommand()

    fun writePauseTripCommand()

    fun writeResumeTripCommand()

    fun writeAddExtrasCommand(extrasAmount: Int)

    suspend fun writePrintReceiptCommand()

    fun updateKValue(kValue: Int)

    fun stopCommunication()

}