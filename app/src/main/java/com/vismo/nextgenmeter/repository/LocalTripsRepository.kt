package com.vismo.nextgenmeter.repository

import com.vismo.nextgenmeter.model.TripData
import kotlinx.coroutines.flow.Flow

interface LocalTripsRepository {

    suspend fun upsertTrip(tripData: TripData)

    suspend fun getMostRecentCompletedTrip() : TripData

    suspend fun getDescendingSortedTrips() : List<TripData>

    fun getDescendingSortedTripsFlow() : Flow<List<TripData>>

    suspend fun clearAllTrips()
}