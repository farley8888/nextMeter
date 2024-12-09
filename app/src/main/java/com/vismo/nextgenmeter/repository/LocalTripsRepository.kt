package com.vismo.nextgenmeter.repository

import com.vismo.nextgenmeter.model.TripData
import kotlinx.coroutines.flow.Flow

interface LocalTripsRepository {

    fun addTrip(tripData: TripData)

    fun updateTrip(tripData: TripData)

    fun setDashPaymentStatus(tripId: String, isDashPayment: Boolean)

    fun deleteTrip(tripData: TripData)

    suspend fun getMostRecentTrip() : TripData

    suspend fun getAllTrips() : List<TripData>

    fun getAllTripsFlow() : Flow<List<TripData>>

    suspend fun getTrip(tripId: String) : TripData?

    suspend fun getLatestOnGoingTripFlow() : Flow<TripData?>

    suspend fun clearAllTrips()
}