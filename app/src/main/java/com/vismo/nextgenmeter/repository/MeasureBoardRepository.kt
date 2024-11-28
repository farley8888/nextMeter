package com.vismo.nextgenmeter.repository

import kotlinx.coroutines.flow.StateFlow


interface MeasureBoardRepository {

    val meterIdentifierInRemote: StateFlow<String>

    fun init()

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

    fun unlockMeter()

    fun updateMeasureBoardTime(formattedDateStr: String)
}