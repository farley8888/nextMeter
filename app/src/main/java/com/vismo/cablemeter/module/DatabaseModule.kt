package com.vismo.cablemeter.module

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.dataStoreFile
import com.vismo.cablemeter.db.LocalTripsRoomDatabase
import com.vismo.cablemeter.model.DriverPinPrefs
import com.vismo.cablemeter.model.DriverPinPrefsSerializer
import com.vismo.cablemeter.model.SkippedDrivers
import com.vismo.cablemeter.model.SkippedDriversPrefsSerializer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private const val DATA_STORE_NAME_FILE_NAME_1 = "driver_prefs_1.pb"
    private const val DATA_STORE_NAME_FILE_NAME_2 = "driver_prefs_2.pb"

    @Singleton
    @Provides
    fun provideLocalTripsRoomDatabase(@ApplicationContext context: Context): LocalTripsRoomDatabase {
        return LocalTripsRoomDatabase.getInstance(context)
    }

    @Singleton
    @Provides
    fun provideLocalTripsDao(database: LocalTripsRoomDatabase) = database.localTripsDao()

    @Singleton
    @Provides
    fun provideDriverPinDataStore(
        @ApplicationContext context: Context,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): DataStore<DriverPinPrefs> {
        return DataStoreFactory.create(
            produceFile = { context.dataStoreFile(DATA_STORE_NAME_FILE_NAME_1) },
            serializer = DriverPinPrefsSerializer,
            scope = CoroutineScope(ioDispatcher + SupervisorJob())
        )
    }

    @Singleton
    @Provides
    fun provideSkippedDriverDataStore(
        @ApplicationContext context: Context,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): DataStore<SkippedDrivers> {
        return DataStoreFactory.create(
            produceFile = { context.dataStoreFile(DATA_STORE_NAME_FILE_NAME_2) },
            serializer = SkippedDriversPrefsSerializer,
            scope = CoroutineScope(ioDispatcher + SupervisorJob())
        )
    }
}