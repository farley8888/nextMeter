package com.vismo.cablemeter.datastore

import com.vismo.cablemeter.model.TripData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object TripDataStore {
    private val _tripData = MutableStateFlow<TripData?>(null)
    val tripData: StateFlow<TripData?> = _tripData

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
}