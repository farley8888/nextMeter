package com.vismo.nextgenmeter.repository

import com.vismo.nextgenmeter.dao.TripsDao
import com.vismo.nextgenmeter.model.TripData
import com.vismo.nextgenmeter.module.DefaultDispatcher
import com.vismo.nextgenmeter.module.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
@Deprecated("Use LocalTripManager instead - room doesn't seem to work well on abrupt restart conditions")
class LocalTripsRepositoryImpl @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
    private val tripsDao: TripsDao
) : LocalTripsRepository {

    private val dbMutex = Mutex()

    override suspend fun upsertTrip(tripData: TripData) {
        withContext(ioDispatcher) {
            dbMutex.withLock {
                tripsDao.upsertTrip(tripData)
                checkpointDatabase()
                delay(500) // Wait for checkpoint to complete
            }
        }
    }

    override suspend fun getMostRecentCompletedTrip(): TripData {
        dbMutex.withLock {
            return tripsDao.getMostRecentTrip() ?: throw Exception("No trips found")
        }
    }

    override suspend fun getDescendingSortedTrips(): List<TripData> {
        dbMutex.withLock {
            return tripsDao.getDescendingSortedTrips()
        }
    }

    override fun getDescendingSortedTripsFlow(): Flow<List<TripData>> {
        return tripsDao.getDescendingSortedTripsFlow()
    }

    override suspend fun clearAllTrips() {
        withContext(ioDispatcher) {
            dbMutex.withLock {
                tripsDao.clearAllTrips()
                tripsDao.deletePrimaryKeyIndex()
                checkpointDatabase()
                delay(500) // Wait for checkpoint to complete
             }
        }
    }
    private suspend fun checkpointDatabase() {
        withContext(defaultDispatcher) {
            tripsDao.checkpoint()
            delay(500)
            tripsDao.deleteWAL()
        }
    }

}