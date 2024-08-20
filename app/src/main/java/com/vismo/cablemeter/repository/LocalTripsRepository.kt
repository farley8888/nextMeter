package com.vismo.cablemeter.repository

import com.vismo.cablemeter.model.TripData
import kotlinx.coroutines.flow.Flow

interface LocalTripsRepository {

    fun addTrip(tripData: TripData)

    fun updateTrip(tripData: TripData)

    fun deleteTrip(tripData: TripData)

    suspend fun getAllTrips() : List<TripData>

    fun getAllTripsFlow() : Flow<List<TripData>>

    suspend fun getTrip(tripId: String) : TripData?

    suspend fun getLatestOnGoingTrip() : TripData?

    suspend fun clearAllTrips()
}