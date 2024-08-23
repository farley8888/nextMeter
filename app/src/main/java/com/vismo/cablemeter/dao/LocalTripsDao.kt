package com.vismo.cablemeter.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.vismo.cablemeter.model.TripData
import kotlinx.coroutines.flow.Flow

@Dao
interface LocalTripsDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addTrip(trip: TripData)

    @Update
    suspend fun updateTrip(trip: TripData)

    @Delete
    suspend fun deleteTrip(trip: TripData)

    @Query("SELECT * FROM trips")
    suspend fun getAllTrips(): List<TripData>

    @Query("SELECT * FROM trips")
    fun getAllTripsFlow(): Flow<List<TripData>>

    @Query("SELECT * FROM trips WHERE tripId = :tripId")
    suspend fun getTrip(tripId: String): TripData?

    @Query("SELECT * FROM trips WHERE end_time IS NULL ORDER BY start_time DESC LIMIT 1")
    suspend fun getLatestOnGoingTrip(): TripData?

    @Query("DELETE FROM trips")
    suspend fun clearAllTrips()

}