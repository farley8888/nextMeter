package com.vismo.cablemeter.repository

import kotlinx.coroutines.flow.StateFlow

interface MeasureBoardRepository {
    val mcuTime: StateFlow<String?>

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