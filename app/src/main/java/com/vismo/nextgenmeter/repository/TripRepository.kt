package com.vismo.nextgenmeter.repository

import com.vismo.nxgnfirebasemodule.model.TripPaidStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

interface TripRepository {
    val currentTripPaidStatus: StateFlow<TripPaidStatus>
    val remoteUnlockMeter: StateFlow<Boolean>

    suspend fun startTrip()

    suspend fun startAndPauseTrip()

    fun endTrip()

    fun pauseTrip()

    fun resumeTrip()

    fun addExtras(extrasAmount: Int)

    fun subtractExtras(extrasAmount: Int)

    fun initObservers(scope: CoroutineScope)

    fun lockMeter(beepDuration: Int, beepInterval: Int, beepRepeatCount: Int)

    fun unlockMeter()

    fun resetUnlockMeterStatusInRemote()

    suspend fun getMostRecentTrip()

    fun emitBeepSound()

}