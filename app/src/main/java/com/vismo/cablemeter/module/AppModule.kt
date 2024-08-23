package com.vismo.cablemeter.module

import android.content.Context
import androidx.datastore.core.DataStore
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.vismo.cablemeter.dao.LocalTripsDao
import com.vismo.cablemeter.model.DriverPinPrefs
import com.vismo.cablemeter.model.SkippedDrivers
import com.vismo.cablemeter.network.api.MeterOApi
import com.vismo.cablemeter.repository.DriverPrefsRepository
import com.vismo.cablemeter.repository.FirebaseAuthRepository
import com.vismo.cablemeter.repository.LocalTripsRepository
import com.vismo.cablemeter.repository.LocalTripsRepositoryImpl
import com.vismo.cablemeter.repository.MeasureBoardRepository
import com.vismo.cablemeter.repository.MeasureBoardRepositoryImpl
import com.vismo.cablemeter.repository.RemoteMeterControlRepository
import com.vismo.cablemeter.repository.RemoteMeterControlRepositoryImpl
import com.vismo.cablemeter.repository.TripRepository
import com.vismo.cablemeter.repository.TripRepositoryImpl
import com.vismo.nxgnfirebasemodule.DashManager
import com.vismo.nxgnfirebasemodule.DashManagerConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth() = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseFirestore() = FirebaseFirestore.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseStorage() = FirebaseStorage.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseCrashlytics() = FirebaseCrashlytics.getInstance()

    @Singleton
    @Provides
    fun provideFirebaseAuthRepository(
        auth: FirebaseAuth,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
        meterOApi: MeterOApi,
        dashManagerConfig: DashManagerConfig
    ): FirebaseAuthRepository {
        return FirebaseAuthRepository(
            auth = auth,
            ioDispatcher = ioDispatcher,
            meterOApi = meterOApi,
            dashManagerConfig = dashManagerConfig
        )
    }

    @Singleton
    @Provides
    fun provideLocalTripsRepository(
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
        localTripsDao: LocalTripsDao
    ): LocalTripsRepository {
        return LocalTripsRepositoryImpl(
            ioDispatcher = ioDispatcher,
            localTripsDao = localTripsDao
        )
    }

    @Provides
    @Singleton
    fun provideMeasureBoardRepository(
        @ApplicationContext context: Context,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
        dashManagerConfig: DashManagerConfig,
        localTripsRepository: LocalTripsRepository
    ): MeasureBoardRepository {
        return MeasureBoardRepositoryImpl(
            context = context,
            ioDispatcher = ioDispatcher,
            dashManagerConfig = dashManagerConfig,
            localTripsRepository = localTripsRepository
        )
    }

    @Singleton
    @Provides
    fun provideTripRepository(
        measureBoardRepository: MeasureBoardRepository,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
        dashManager: DashManager,
        localTripsRepository: LocalTripsRepository
    ): TripRepository {
        return TripRepositoryImpl (
            ioDispatcher = ioDispatcher,
            measureBoardRepository = measureBoardRepository,
            dashManager = dashManager,
            localTripsRepository = localTripsRepository
        )
    }

    @Singleton
    @Provides
    fun provideRemoteMCUControlRepository(
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
        dashManager: DashManager,
        measureBoardRepository: MeasureBoardRepository
    ): RemoteMeterControlRepository {
        return RemoteMeterControlRepositoryImpl(
            ioDispatcher = ioDispatcher,
            dashManager = dashManager,
            measureBoardRepository = measureBoardRepository
        )
    }

    @Singleton
    @Provides
    fun provideDriverPrefsRepository(
        driverPrefsDataStore: DataStore<DriverPinPrefs>,
        skippedDrivers: DataStore<SkippedDrivers>,
        meterOApi: MeterOApi,
        firebaseAuthRepository: FirebaseAuthRepository
    ): DriverPrefsRepository {
        return DriverPrefsRepository(
            driverPrefsDataStore = driverPrefsDataStore,
            skippedDriversDS = skippedDrivers,
            meterOApi = meterOApi,
            firebaseAuthRepository = firebaseAuthRepository
        )
    }

}
