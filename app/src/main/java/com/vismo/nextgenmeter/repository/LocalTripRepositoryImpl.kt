package com.vismo.nextgenmeter.repository

import com.vismo.nextgenmeter.dao.TripsDao
import com.vismo.nextgenmeter.model.TripData
import com.vismo.nextgenmeter.module.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

class LocalTripsRepositoryImpl @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val tripsDao: TripsDao
) : LocalTripsRepository {
    override suspend fun addTrip(tripData: TripData) {
        withContext(ioDispatcher)  {
            tripsDao.addTrip(tripData)
        }
    }

    override suspend fun updateTrip(tripData: TripData) {
        withContext(ioDispatcher) {
            tripsDao.updateTrip(tripData)
        }
    }

    override suspend fun setDashPaymentStatus(tripId: String, isDashPayment: Boolean) {
        withContext(ioDispatcher)  {
            tripsDao.setDashPaymentStatus(tripId, isDashPayment)
        }
    }

    override suspend fun deleteTrip(tripData: TripData) {
        withContext(ioDispatcher)  {
            tripsDao.deleteTrip(tripData)
        }
    }

    override suspend fun getMostRecentCompletedTrip(): TripData {
        return tripsDao.getMostRecentTrip() ?: throw Exception("No trips found")
    }

    override suspend fun getDescendingSortedTrips(): List<TripData> {
        return tripsDao.getDescendingSortedTrips()
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