package com.vismo.nextgenmeter.module

import android.content.Context
import com.vismo.nextgenmeter.db.LocalTripsRoomDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Singleton
    @Provides
    fun provideDatabase(@ApplicationContext context: Context): LocalTripsRoomDatabase {
        return LocalTripsRoomDatabase.getInstance(context)
    }

    @Singleton
    @Provides
    fun provideTripsDao(database: LocalTripsRoomDatabase) = database.tripsDao()
}