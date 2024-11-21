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

    private val _fallbackTripDataToStartNewTrip = MutableStateFlow<TripData?>(null)
    val fallbackTripDataToStartNewTrip: StateFlow<TripData?> = _fallbackTripDataToStartNewTrip

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

    suspend fun setFallbackTripDataToStartNewTrip(fallbackTripData: TripData) {
        mutex.withLock {
            if (_tripData.value == null) { // only if there is no ongoing trip
                this._fallbackTripDataToStartNewTrip.value = fallbackTripData
            }
        }
    }

    suspend fun clearFallbackTripDataToStartNewTrip() {
        mutex.withLock {
            this._fallbackTripDataToStartNewTrip.value = null
        }
    }
}