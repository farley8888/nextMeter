package com.vismo.nextgenmeter.datastore

import com.vismo.nextgenmeter.model.TripData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object TripDataStore {
    private val _ongoingTripData = MutableStateFlow<TripData?>(null)
    val ongoingTripData: StateFlow<TripData?> = _ongoingTripData

    private val _isAbnormalPulseTriggered = MutableStateFlow(false)
    val isAbnormalPulseTriggered: StateFlow<Boolean> = _isAbnormalPulseTriggered

    private val _mostRecentTripData = MutableStateFlow<TripData?>(null)
    val mostRecentTripData: StateFlow<TripData?> = _mostRecentTripData

    private val mutex = Mutex() // Mutex for synchronization

    suspend fun clearTripData() {
        mutex.withLock {
            this._ongoingTripData.value = null
        }
    }

    suspend fun updateTripDataValue(updatedTripData: TripData) {
        mutex.withLock {
            this._ongoingTripData.value = updatedTripData
        }
    }

    suspend fun setAbnormalPulseTriggered(isAbnormalPulseTriggered: Boolean) {
        mutex.withLock {
            this._isAbnormalPulseTriggered.value = isAbnormalPulseTriggered
        }
    }

    suspend fun setMostRecentTripData(mostRecentTripData: TripData?) {
        mutex.withLock {
            this._mostRecentTripData.value = mostRecentTripData
        }
    }

    suspend fun clearMostRecentTripData() {
        mutex.withLock {
            this._mostRecentTripData.value = null
        }
    }
}