package com.vismo.nextgenmeter.db

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.vismo.nextgenmeter.BuildConfig
import com.vismo.nextgenmeter.dao.TripsDao
import com.vismo.nextgenmeter.model.TripData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor

@Database(entities = [TripData::class], version = 6, exportSchema = false)
abstract class LocalTripsRoomDatabase: RoomDatabase() {
    abstract fun tripsDao(): TripsDao

    companion object {
        private const val DATABASE_NAME = "local_trips_db_${BuildConfig.FLAVOR}"
        private const val TAG = "LocalTripsRoomDatabase"

        @Volatile
        private var INSTANCE: LocalTripsRoomDatabase? = null

        fun getInstance(context: Context): LocalTripsRoomDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context = context.applicationContext,
                    LocalTripsRoomDatabase::class.java,
                    DATABASE_NAME
                ).setJournalMode(JournalMode.TRUNCATE)
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onOpen(db: SupportSQLiteDatabase) {
                            super.onOpen(db)
                            db.execSQL("PRAGMA synchronous=EXTRA;")
                            db.query("PRAGMA wal_checkpoint(FULL);")
                        }
                    })
                    .fallbackToDestructiveMigration()
                    .setQueryCallback({ sqlQuery, bindArgs ->
                        Log.d("LocalTripsRoomDatabase", "Query: $sqlQuery - Args: $bindArgs")
                    }, Dispatchers.IO.asExecutor())
                    .build()
                INSTANCE = instance
                Log.d(TAG, "getInstance - instance created: $instance - context ${context.applicationContext}")
                instance
            }
        }
    }
}