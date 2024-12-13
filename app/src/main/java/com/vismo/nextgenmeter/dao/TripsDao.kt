package com.vismo.nextgenmeter.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Transaction
import androidx.room.Upsert
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.vismo.nextgenmeter.model.TripData
import kotlinx.coroutines.flow.Flow

@Dao
interface TripsDao {

    @Upsert
    suspend fun upsertTrip(trip: TripData)

    @Query("SELECT * FROM trips ORDER BY start_time DESC LIMIT 1")
    suspend fun getMostRecentTrip(): TripData?

    @Query("SELECT * FROM trips ORDER BY start_time DESC")
    suspend fun getDescendingSortedTrips(): List<TripData>

    @Query("SELECT * FROM trips ORDER BY start_time DESC")
    fun getDescendingSortedTripsFlow(): Flow<List<TripData>>

    @Query("SELECT * FROM trips WHERE tripId = :tripId")
    suspend fun getTrip(tripId: String): TripData?

    @Query("SELECT * FROM trips WHERE end_time IS NULL ORDER BY start_time DESC LIMIT 1")
    fun getLatestOnGoingTripFlow(): Flow<TripData?>

    @Query("DELETE FROM trips")
    suspend fun clearAllTrips()

    @Query("DELETE FROM sqlite_sequence WHERE name='trips'")
    suspend fun deletePrimaryKeyIndex()

    @RawQuery
    fun checkpoint(supportSQLiteQuery: SupportSQLiteQuery = SimpleSQLiteQuery("PRAGMA wal_checkpoint(FULL)")): Int

}