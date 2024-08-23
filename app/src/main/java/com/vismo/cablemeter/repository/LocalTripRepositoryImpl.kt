package com.vismo.cablemeter.repository

import com.vismo.cablemeter.dao.LocalTripsDao
import com.vismo.cablemeter.model.TripData
import com.vismo.cablemeter.module.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

class LocalTripsRepositoryImpl @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val localTripsDao: LocalTripsDao
) : LocalTripsRepository {
    override fun addTrip(tripData: TripData) {
        CoroutineScope(ioDispatcher).launch {
            localTripsDao.addTrip(tripData)
        }
    }

    override fun updateTrip(tripData: TripData) {
        CoroutineScope(ioDispatcher).launch {
            localTripsDao.updateTrip(tripData)
        }
    }

    override fun deleteTrip(tripData: TripData) {
        CoroutineScope(ioDispatcher).launch {
            localTripsDao.deleteTrip(tripData)
        }
    }

    override suspend fun getAllTrips(): List<TripData> {
        return localTripsDao.getAllTrips()
    }

    override fun getAllTripsFlow(): Flow<List<TripData>> {
        return localTripsDao.getAllTripsFlow()
    }

    override suspend fun getTrip(tripId: String): TripData? {
        return localTripsDao.getTrip(tripId)
    }

    override suspend fun getLatestOnGoingTrip(): TripData? {
        return localTripsDao.getLatestOnGoingTrip()
    }

    override suspend fun clearAllTrips() {
        CoroutineScope(ioDispatcher).launch {
            localTripsDao.clearAllTrips()
        }
    }

}