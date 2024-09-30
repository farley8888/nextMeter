package com.vismo.cablemeter.repository


interface MeasureBoardRepository {

    fun writeStartTripCommand(tripId: String)

    fun writeStartAndPauseTripCommand(tripId: String)

    fun writeEndTripCommand()

    fun writePauseTripCommand()

    fun writeResumeTripCommand()

    fun writeAddExtrasCommand(extrasAmount: Int)

    fun emitBeepSound(
        duration: Int,
        interval: Int,
        repeatCount: Int,
    )

    fun updateKValue(kValue: Int)

    fun updateLicensePlate(licensePlate: String)

    fun updatePriceParams(startPrice: Int, stepPrice: Int, stepPrice2nd:Int, threshold:Int)

    fun stopCommunication()

    fun enquireParameters()
}