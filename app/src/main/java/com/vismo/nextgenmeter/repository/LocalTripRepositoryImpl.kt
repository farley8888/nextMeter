package com.vismo.nextgenmeter.repository

import com.vismo.nextgenmeter.dao.TripsDao
import com.vismo.nextgenmeter.model.TripData
import com.vismo.nextgenmeter.module.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

class LocalTripsRepositoryImpl @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val tripsDao: TripsDao
) : LocalTripsRepository {
    override fun addTrip(tripData: TripData) {
        CoroutineScope(ioDispatcher).launch {
            tripsDao.addTrip(tripData)
        }
    }

    override fun updateTrip(tripData: TripData) {
        CoroutineScope(ioDispatcher).launch {
            tripsDao.updateTrip(tripData)
        }
    }

    override fun setDashPaymentStatus(tripId: String, isDashPayment: Boolean) {
        CoroutineScope(ioDispatcher).launch {
            tripsDao.setDashPaymentStatus(tripId, isDashPayment)
        }
    }

    override fun deleteTrip(tripData: TripData) {
        CoroutineScope(ioDispatcher).launch {
            tripsDao.deleteTrip(tripData)
        }
    }

    override suspend fun getMostRecentCompletedTrip(): TripData {
        return tripsDao.getMostRecentTrip() ?: throw Exception("No trips found")
    }

    override suspend fun getAllTrips(): List<TripData> {
        return tripsDao.getAllTrips()
    }

    override fun getAllTripsFlow(): Flow<List<TripData>> {
        return tripsDao.getAllTripsFlow()
    }

    override suspend fun getTrip(tripId: String): TripData? {
        return tripsDao.getTrip(tripId)
    }

    override suspend fun getLatestOnGoingTripFlow(): Flow<TripData?> {
        return tripsDao.getLatestOnGoingTripFlow()
    }

    override suspend fun clearAllTrips() {
        CoroutineScope(ioDispatcher).launch {
            tripsDao.clearAllTrips()
        }
    }

}