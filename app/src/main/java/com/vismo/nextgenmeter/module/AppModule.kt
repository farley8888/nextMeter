package com.vismo.nextgenmeter.module

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.storage.FirebaseStorage
import com.google.gson.Gson
import com.vismo.nextgenmeter.dao.TripsDao
import com.vismo.nextgenmeter.repository.DriverPreferenceRepository
import com.vismo.nextgenmeter.repository.FirebaseAuthRepository
import com.vismo.nextgenmeter.repository.InternetConnectivityObserver
import com.vismo.nextgenmeter.repository.LocalTripsRepository
import com.vismo.nextgenmeter.repository.LocalTripsRepositoryImpl
import com.vismo.nextgenmeter.repository.LogShippingRepository
import com.vismo.nextgenmeter.repository.MeasureBoardRepository
import com.vismo.nextgenmeter.repository.MeasureBoardRepositoryImpl
import com.vismo.nextgenmeter.repository.MeterOApiRepository
import com.vismo.nextgenmeter.repository.MeterPreferenceRepository
import com.vismo.nextgenmeter.repository.NetworkTimeRepository
import com.vismo.nextgenmeter.repository.PeripheralControlRepository
import com.vismo.nextgenmeter.repository.PeripheralControlRepositoryImpl
import com.vismo.nextgenmeter.repository.RemoteMeterControlRepository
import com.vismo.nextgenmeter.repository.RemoteMeterControlRepositoryImpl
import com.vismo.nextgenmeter.repository.SystemControlRepository
import com.vismo.nextgenmeter.repository.TripFileManager
import com.vismo.nextgenmeter.repository.TripRepository
import com.vismo.nextgenmeter.repository.TripRepositoryImpl
import com.vismo.nextgenmeter.util.LocaleHelper
import com.vismo.nextgenmeter.util.AndroidROMOTAUpdateManager
import com.vismo.nextgenmeter.util.TtsUtil
import com.vismo.nxgnfirebasemodule.DashManager
import com.vismo.nxgnfirebasemodule.DashManagerConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Singleton
    @Provides
    fun providesMeterPreferenceRepository(
        @ApplicationContext context: Context,
    ): MeterPreferenceRepository {
        return MeterPreferenceRepository(
            context = context,
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

    @Provides
    @Singleton
    fun providesFirebaseAuth() = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun providesFirebaseFirestore(
        internetConnectivityObserver: InternetConnectivityObserver,
        meterPreferenceRepository: MeterPreferenceRepository
    ): FirebaseFirestore {
        val shouldClearPersistenceCache = runBlocking {
            meterPreferenceRepository.getWasMeterOnlineAtLastAccOff().firstOrNull() == true
        }
        val isMeterOnline = runBlocking {
            internetConnectivityObserver.internetStatus.firstOrNull() == InternetConnectivityObserver.Status.InternetAvailable
        }

        val isTripOngoing = runBlocking {
            !meterPreferenceRepository.getOngoingTripId().firstOrNull().isNullOrBlank()
        }

        val settings = FirebaseFirestoreSettings.Builder()
            .setLocalCacheSettings(
                PersistentCacheSettings.newBuilder()
                    .setSizeBytes(100 * 1024 * 1024) // 100 MB
                    .build()
            )
            .build()

        return FirebaseFirestore.getInstance().apply {
            firestoreSettings = settings
            Log.d("AppModule", "shouldClearPersistenceCache: $shouldClearPersistenceCache, isMeterOnline: $isMeterOnline, isTripOngoing: $isTripOngoing")
            if (shouldClearPersistenceCache && isMeterOnline && !isTripOngoing) {
                clearPersistence().addOnCompleteListener {
                    Log.d("AppModule", "Is Firestore persistence cache cleared - ${it.isSuccessful}")
                }
            }
        }
    }

    @Provides
    @Singleton
    fun providesFirebaseStorage() = FirebaseStorage.getInstance()

    @Provides
    @Singleton
    fun provideLogsStorageReference(storage: com.google.firebase.storage.FirebaseStorage): com.google.firebase.storage.StorageReference {
        return storage.reference.child("dash-meter-logs")
    }

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
    fun providesTripFileManager(
        @ApplicationContext context: Context,
        gson: Gson,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): TripFileManager {
        return TripFileManager(
            context = context,
            gson = gson,
            ioDispatcher = ioDispatcher
        )
    }

    @Singleton
    @Provides
    fun providesLocalTripsRepository(
        @DefaultDispatcher defaultDispatcher: CoroutineDispatcher,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
        tripsDao: TripsDao,
    ): LocalTripsRepository {
        return LocalTripsRepositoryImpl(
            defaultDispatcher = defaultDispatcher,
            ioDispatcher = ioDispatcher,
            tripsDao = tripsDao,
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
        dashManagerConfig: DashManagerConfig
    ): PeripheralControlRepository {
        return PeripheralControlRepositoryImpl(
            ioDispatcher = ioDispatcher,
            measureBoardRepository = measureBoardRepository,
            dashManagerConfig = dashManagerConfig
        )
    }

    @Singleton
    @Provides
    fun provideTripRepository(
        measureBoardRepository: MeasureBoardRepository,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
        dashManager: DashManager,
        meterPreferenceRepository: MeterPreferenceRepository,
        tripFileManager: TripFileManager
    ): TripRepository {
        return TripRepositoryImpl (
            ioDispatcher = ioDispatcher,
            measureBoardRepository = measureBoardRepository,
            dashManager = dashManager,
            meterPreferenceRepository = meterPreferenceRepository,
            tripFileManager = tripFileManager
        )
    }

    @Singleton
    @Provides
    fun providesRemoteMCUControlRepository(
        dashManager: DashManager,
        measureBoardRepository: MeasureBoardRepository,
        logShippingRepository: LogShippingRepository,
        meterPreferenceRepository: MeterPreferenceRepository
    ): RemoteMeterControlRepository {
        return RemoteMeterControlRepositoryImpl(
            dashManager = dashManager,
            measureBoardRepository = measureBoardRepository,
            logShippingRepository = logShippingRepository,
            meterPreferenceRepository = meterPreferenceRepository
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
    fun providesLogShippingRepository(
        @ApplicationContext context: Context,
        storageReference: com.google.firebase.storage.StorageReference,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
        meterPreferenceRepository: MeterPreferenceRepository
    ): LogShippingRepository {
        return LogShippingRepository(
            context = context,
            storageReference = storageReference,
            ioDispatcher = ioDispatcher,
            meterPreferenceRepository = meterPreferenceRepository
        )
    }

    @Singleton
    @Provides
    fun providesOTAUpdateManager(
        @ApplicationContext context: Context,
        internetConnectivityObserver: InternetConnectivityObserver
    ): AndroidROMOTAUpdateManager {
        return AndroidROMOTAUpdateManager(
            context = context,
            internetConnectivityObserver = internetConnectivityObserver
        )
    }

    @Singleton
    @Provides
    fun providesSystemControlRepository(
        @ApplicationContext context: Context
    ): SystemControlRepository {
        return SystemControlRepository(
            context = context
        )
    }
}
