package com.vismo.nextgenmeter.repository

import com.vismo.nextgenmeter.model.TripData
import kotlinx.coroutines.flow.Flow

interface LocalTripsRepository {

    suspend fun addTrip(tripData: TripData)

    suspend fun updateTrip(tripData: TripData)

    suspend fun setDashPaymentStatus(tripId: String, isDashPayment: Boolean)

    suspend fun deleteTrip(tripData: TripData)

    suspend fun getMostRecentCompletedTrip() : TripData

    suspend fun getDescendingSortedTrips() : List<TripData>

    fun getDescendingSortedTripsFlow() : Flow<List<TripData>>

    suspend fun getTrip(tripId: String) : TripData?

    suspend fun getLatestOnGoingTripFlow() : Flow<TripData?>

    suspend fun clearAllTrips()
}