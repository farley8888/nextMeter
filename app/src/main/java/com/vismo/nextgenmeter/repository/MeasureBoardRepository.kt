package com.vismo.nextgenmeter.repository

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow


interface MeasureBoardRepository {

    val meterIdentifierInRemote: StateFlow<String>

    fun startCommunicate()

    fun init(scope: CoroutineScope)

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

    fun updateKValue(kValue: Int?,
                     boardShutdownMinsDelayAfterAcc: Int? = null)

    fun updateLicensePlate(licensePlate: String)

    fun updatePriceParams(startPrice: Int, stepPrice: Int, stepPrice2nd:Int, threshold:Int)

    fun stopCommunication()

    fun enquireParameters()

    /**
     * Sends the 0x10B0 command to get metering board information
     * This should be called after app startup to obtain key information from the metering board
     */
    fun getMeteringBoardInfo()

    fun unlockMeter()

    fun updateMeasureBoardTime(formattedDateStr: String)

    suspend fun requestPatchFirmware(fileName: String)

    fun close()

    fun notifyShutdown()

    fun notifyAndroidFirmwareVersion(androidFirmwareVersion: String)
}