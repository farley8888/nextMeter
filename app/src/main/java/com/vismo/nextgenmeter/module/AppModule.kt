package com.vismo.nextgenmeter.module

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.gson.Gson
import com.vismo.nextgenmeter.dao.TripsDao
import com.vismo.nextgenmeter.repository.DriverPreferenceRepository
import com.vismo.nextgenmeter.repository.FirebaseAuthRepository
import com.vismo.nextgenmeter.repository.InternetConnectivityObserver
import com.vismo.nextgenmeter.repository.LocalTripsRepository
import com.vismo.nextgenmeter.repository.LocalTripsRepositoryImpl
import com.vismo.nextgenmeter.repository.MeasureBoardRepository
import com.vismo.nextgenmeter.repository.MeasureBoardRepositoryImpl
import com.vismo.nextgenmeter.repository.MeterOApiRepository
import com.vismo.nextgenmeter.repository.MeterPreferenceRepository
import com.vismo.nextgenmeter.repository.NetworkTimeRepository
import com.vismo.nextgenmeter.repository.PeripheralControlRepository
import com.vismo.nextgenmeter.repository.PeripheralControlRepositoryImpl
import com.vismo.nextgenmeter.repository.RemoteMeterControlRepository
import com.vismo.nextgenmeter.repository.RemoteMeterControlRepositoryImpl
import com.vismo.nextgenmeter.repository.TripRepository
import com.vismo.nextgenmeter.repository.TripRepositoryImpl
import com.vismo.nextgenmeter.util.LocaleHelper
import com.vismo.nextgenmeter.util.TtsUtil
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
    fun providesFirebaseAuth() = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun providesFirebaseFirestore() = FirebaseFirestore.getInstance()

    @Provides
    @Singleton
    fun providesFirebaseStorage() = FirebaseStorage.getInstance()

    @Provides
    @Singleton
    fun providesFirebaseCrashlytics() = FirebaseCrashlytics.getInstance()

    @Provides
    @Singleton
    fun providesLocaleHelper(
        @ApplicationContext context: Context
    ): LocaleHelper {
        return LocaleHelper(appContext = context)
    }

    @Provides
    @Singleton
    fun providesTtsUtil(
        @ApplicationContext context: Context,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
        @MainDispatcher mainDispatcher: CoroutineDispatcher,
        localeHelper: LocaleHelper
    ): TtsUtil {
        return TtsUtil(
            appContext = context,
            ioDispatcher = ioDispatcher,
            mainDispatcher = mainDispatcher,
            localeHelper = localeHelper
        )
    }


    @Singleton
    @Provides
    fun provideFirebaseAuthRepository(
        auth: FirebaseAuth,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
        meterOApiRepository: MeterOApiRepository,
        meterPreferenceRepository: MeterPreferenceRepository
    ): FirebaseAuthRepository {
        return FirebaseAuthRepository(
            auth = auth,
            ioDispatcher = ioDispatcher,
            meterOApiRepository = meterOApiRepository,
            meterPreferenceRepository = meterPreferenceRepository
        )
    }

    @Singleton
    @Provides
    fun providesLocalTripsRepository(
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
        tripsDao: TripsDao,
    ): LocalTripsRepository {
        return LocalTripsRepositoryImpl(
            ioDispatcher = ioDispatcher,
            tripsDao = tripsDao,
        )
    }

    @Singleton
    @Provides
    fun providesMeterPreferenceRepository(
        @ApplicationContext context: Context,
    ): MeterPreferenceRepository {
        return MeterPreferenceRepository(
            context = context,
        )
    }

    @Provides
    @Singleton
    fun provideMeasureBoardRepository(
        @ApplicationContext context: Context,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
        dashManagerConfig: DashManagerConfig,
        meterPreferenceRepository: MeterPreferenceRepository
    ): MeasureBoardRepository {
        return MeasureBoardRepositoryImpl(
            context = context,
            ioDispatcher = ioDispatcher,
            dashManagerConfig = dashManagerConfig,
            meterPreferenceRepository = meterPreferenceRepository
        )
    }

    @Singleton
    @Provides
    fun providesPeripheralControlRepository(
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
        measureBoardRepository: MeasureBoardRepository,
    ): PeripheralControlRepository {
        return PeripheralControlRepositoryImpl(
            ioDispatcher = ioDispatcher,
            measureBoardRepository = measureBoardRepository
        )
    }

    @Singleton
    @Provides
    fun provideTripRepository(
        measureBoardRepository: MeasureBoardRepository,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
        dashManager: DashManager,
        localTripsRepository: LocalTripsRepository,
        meterPreferenceRepository: MeterPreferenceRepository
    ): TripRepository {
        return TripRepositoryImpl (
            ioDispatcher = ioDispatcher,
            measureBoardRepository = measureBoardRepository,
            dashManager = dashManager,
            localTripsRepository = localTripsRepository,
            meterPreferenceRepository = meterPreferenceRepository
        )
    }

    @Singleton
    @Provides
    fun providesRemoteMCUControlRepository(
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
        dashManager: DashManager,
        measureBoardRepository: MeasureBoardRepository,
    ): RemoteMeterControlRepository {
        return RemoteMeterControlRepositoryImpl(
            ioDispatcher = ioDispatcher,
            dashManager = dashManager,
            measureBoardRepository = measureBoardRepository,
        )
    }

    @Singleton
    @Provides
    fun providesNetworkTimeRepository(
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): NetworkTimeRepository {
        return NetworkTimeRepository(
            ioDispatcher = ioDispatcher,
        )
    }

    @Singleton
    @Provides
    fun providesDriverPreferenceRepository(
        @ApplicationContext context: Context,
        gson: Gson
    ): DriverPreferenceRepository {
        return DriverPreferenceRepository(
            context = context,
            gson = gson
        )
    }

    @Singleton
    @Provides
    fun providesInternetConnectivityObserver(
        @ApplicationContext context: Context,
    ): InternetConnectivityObserver {
        return InternetConnectivityObserver(
            context = context,
        )
    }
}
