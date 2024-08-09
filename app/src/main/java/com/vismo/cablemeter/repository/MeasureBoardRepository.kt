package com.vismo.cablemeter.repository

interface MeasureBoardRepository {

    fun writeStartTripCommand(tripId: String)

    fun writeStartAndPauseTripCommand(tripId: String)

    fun writeEndTripCommand()

    fun writePauseTripCommand()

    fun writeResumeTripCommand()

    fun writeAddExtrasCommand(extrasAmount: Int)

    suspend fun writePrintReceiptCommand()

    fun updateKValue(kValue: Int)

}