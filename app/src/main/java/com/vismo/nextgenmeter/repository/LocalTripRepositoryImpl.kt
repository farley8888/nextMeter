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

    override suspend fun upsertTrip(tripData: TripData) {
        withContext(ioDispatcher) {
            tripsDao.upsertTrip(tripData)
        }
    }

    override suspend fun getMostRecentCompletedTrip(): TripData {
        return tripsDao.getMostRecentTrip() ?: throw Exception("No trips found")
    }

    override suspend fun getDescendingSortedTrips(): List<TripData> {
        return tripsDao.getDescendingSortedTrips()
    }

    override fun getDescendingSortedTripsFlow(): Flow<List<TripData>> {
        return tripsDao.getDescendingSortedTripsFlow()
    }

    override suspend fun clearAllTrips() {
        CoroutineScope(ioDispatcher).launch {
            tripsDao.clearAllTrips()
        }
    }

}