package com.vismo.cablemeter.repository

interface TripRepository {
    suspend fun startTrip()

    suspend fun startAndPauseTrip()

    fun endTrip()

    fun pauseTrip()

    fun resumeTrip()

    fun addExtras(extrasAmount: Int)

    fun close()

}