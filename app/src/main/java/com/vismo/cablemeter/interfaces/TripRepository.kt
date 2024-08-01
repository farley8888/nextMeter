package com.vismo.cablemeter.interfaces

interface TripRepository {
    fun startTrip()

    fun startAndPauseTrip()

    fun endTrip()

    fun pauseTrip()

    fun resumeTrip()

    fun addExtras(extrasAmount: Int)

    fun printReceipt()
}