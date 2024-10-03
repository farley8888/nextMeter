package com.vismo.cablemeter.repository

import com.vismo.nxgnfirebasemodule.model.TripPaidStatus
import kotlinx.coroutines.flow.StateFlow

interface TripRepository {
    val currentTripPaidStatus: StateFlow<TripPaidStatus>

    suspend fun startTrip()

    suspend fun startAndPauseTrip()

    fun endTrip()

    fun pauseTrip()

    fun resumeTrip()

    fun addExtras(extrasAmount: Int)

    fun close()

}