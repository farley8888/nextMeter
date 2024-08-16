package com.vismo.cablemeter.datastore

import com.vismo.cablemeter.model.TripData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object TripDataStore {
    private val _tripData = MutableStateFlow<TripData?>(null)
    val tripData: StateFlow<TripData?> = _tripData

    fun setTripData(tripData: TripData) {
        this._tripData.value = tripData
    }

    fun clearTripData() {
        this._tripData.value = null
    }

    fun updateTripDataValue(updatedTripData: TripData) {
        this._tripData.value = updatedTripData
    }
}