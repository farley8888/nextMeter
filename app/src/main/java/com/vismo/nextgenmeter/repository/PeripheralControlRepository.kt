package com.vismo.nextgenmeter.repository

import com.vismo.nextgenmeter.model.TripData
import com.vismo.nextgenmeter.model.TripSummary

interface PeripheralControlRepository {
    fun isFlagDown(): Boolean
    fun toggleForHireFlag(goDown: Boolean)
    suspend fun printTripReceiptCommand(tripData: TripData)
    suspend fun printSummaryReceiptCommand(tripSummary: TripSummary)
    fun close()
}