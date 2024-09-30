package com.vismo.cablemeter.repository

import com.vismo.cablemeter.model.TripData

interface PeripheralControlRepository {
    suspend fun writePrintReceiptCommand(tripData: TripData)
    fun close()
}