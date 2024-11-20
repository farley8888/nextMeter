package com.vismo.nextgenmeter.repository

import com.vismo.nextgenmeter.model.TripData

interface PeripheralControlRepository {
    suspend fun writePrintReceiptCommand(tripData: TripData)
    fun close()
}