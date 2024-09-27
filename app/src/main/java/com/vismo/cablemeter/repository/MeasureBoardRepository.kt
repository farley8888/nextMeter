package com.vismo.cablemeter.repository

import com.vismo.cablemeter.model.TripData


interface MeasureBoardRepository {

    fun writeStartTripCommand(tripId: String)

    fun writeStartAndPauseTripCommand(tripId: String)

    fun writeEndTripCommand()

    fun writePauseTripCommand()

    fun writeResumeTripCommand()

    fun writeAddExtrasCommand(extrasAmount: Int)

    suspend fun writePrintReceiptCommand(tripData: TripData)

    fun updateKValue(kValue: Int)

    fun updateLicensePlate(licensePlate: String)

    fun updatePriceParams(startPrice: Int, stepPrice: Int, stepPrice2nd:Int, threshold:Int)

    fun stopCommunication()

    fun enquireParameters()
}