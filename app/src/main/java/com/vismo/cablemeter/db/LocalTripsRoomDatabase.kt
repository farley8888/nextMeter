package com.vismo.cablemeter.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.vismo.cablemeter.dao.LocalTripsDao
import com.vismo.cablemeter.model.TripData

@Database(entities = [TripData::class], version = 1, exportSchema = false)
abstract class LocalTripsRoomDatabase: RoomDatabase() {
    abstract fun localTripsDao(): LocalTripsDao

    companion object {
        private const val DATABASE_NAME = "local_trips_db"

        @Volatile
        private var INSTANCE: LocalTripsRoomDatabase? = null

        fun getInstance(context: Context): LocalTripsRoomDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context = context.applicationContext,
                    LocalTripsRoomDatabase::class.java,
                    DATABASE_NAME
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}