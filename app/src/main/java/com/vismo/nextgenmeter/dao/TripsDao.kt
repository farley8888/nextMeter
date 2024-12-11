package com.vismo.nextgenmeter.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.vismo.nextgenmeter.model.TripData
import kotlinx.coroutines.flow.Flow

@Dao
interface TripsDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addTrip(trip: TripData)

    @Update
    suspend fun updateTrip(trip: TripData)

    @Query("UPDATE trips SET is_dash = :isDashPayment WHERE tripId = :tripId")
    suspend fun setDashPaymentStatus(tripId: String, isDashPayment: Boolean)

    @Delete
    suspend fun deleteTrip(trip: TripData)

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

}