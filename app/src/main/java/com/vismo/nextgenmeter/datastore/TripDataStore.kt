package com.vismo.nextgenmeter.datastore

import com.vismo.nextgenmeter.model.TripData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object TripDataStore {
    private val _tripData = MutableStateFlow<TripData?>(null)
    val tripData: StateFlow<TripData?> = _tripData

    private val _isAbnormalPulseTriggered = MutableStateFlow(false)
    val isAbnormalPulseTriggered: StateFlow<Boolean> = _isAbnormalPulseTriggered

    private val mutex = Mutex() // Mutex for synchronization

    suspend fun setTripData(tripData: TripData) {
        mutex.withLock {
            this._tripData.value = tripData
        }
    }

    suspend fun clearTripData() {
        mutex.withLock {
            this._tripData.value = null
        }
    }

    suspend fun updateTripDataValue(updatedTripData: TripData) {
        mutex.withLock {
            this._tripData.value = updatedTripData
        }
    }

    suspend fun setAbnormalPulseTriggered(isAbnormalPulseTriggered: Boolean) {
        mutex.withLock {
            this._isAbnormalPulseTriggered.value = isAbnormalPulseTriggered
        }
    }
}