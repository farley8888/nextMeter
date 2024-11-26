package com.vismo.nextgenmeter.repository

import com.vismo.nextgenmeter.model.TripData

interface PeripheralControlRepository {
    fun toggleForHireFlag(goDown: Boolean)
    suspend fun writePrintReceiptCommand(tripData: TripData)
    fun close()
}