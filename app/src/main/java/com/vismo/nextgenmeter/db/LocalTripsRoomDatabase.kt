package com.vismo.nextgenmeter.db

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.vismo.nextgenmeter.BuildConfig
import com.vismo.nextgenmeter.dao.TripsDao
import com.vismo.nextgenmeter.model.TripData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor

@Database(entities = [TripData::class], version = 4, exportSchema = false)
abstract class LocalTripsRoomDatabase: RoomDatabase() {
    abstract fun tripsDao(): TripsDao

    companion object {
        private const val DATABASE_NAME = "local_trips_db_${BuildConfig.FLAVOR}"

        @Volatile
        private var INSTANCE: LocalTripsRoomDatabase? = null

        fun getInstance(context: Context): LocalTripsRoomDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context = context.applicationContext,
                    LocalTripsRoomDatabase::class.java,
                    DATABASE_NAME
                ).setJournalMode(JournalMode.TRUNCATE)
                    .fallbackToDestructiveMigration()
                    .setQueryCallback({ sqlQuery, bindArgs ->
                        Log.d("LocalTripsRoomDatabase", "Query: $sqlQuery - Args: $bindArgs")
                    }, Dispatchers.IO.asExecutor())
                    .build()
                INSTANCE = instance
                Log.d("LocalTripsRoomDatabase", "getInstance - instance created: $instance - context ${context.applicationContext}")
                instance
            }
        }
    }
}