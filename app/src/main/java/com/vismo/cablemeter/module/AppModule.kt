package com.vismo.cablemeter.module

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.vismo.cablemeter.network.api.MeterOApi
import com.vismo.cablemeter.repository.FirebaseAuthRepository
import com.vismo.cablemeter.repository.MeasureBoardRepository
import com.vismo.cablemeter.repository.MeasureBoardRepositoryImpl
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

    @Singleton
    @Provides
    fun provideFirebaseAuthRepository(
        auth: FirebaseAuth,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
        meterOApi: MeterOApi,
    ): FirebaseAuthRepository {
        return FirebaseAuthRepository(
            auth = auth,
            ioDispatcher = ioDispatcher,
            meterOApi = meterOApi
        )
    }

    @Provides
    @Singleton
    fun provideMeasureBoardRepository(
        @ApplicationContext context: Context,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
        dashManagerConfig: DashManagerConfig
    ): MeasureBoardRepository {
        return MeasureBoardRepositoryImpl(
            context = context,
            ioDispatcher = ioDispatcher,
            dashManagerConfig = dashManagerConfig
        )
    }

    @Singleton
    @Provides
    fun provideTripRepository(
        firebaseAuthRepository: FirebaseAuthRepository,
        firestore: FirebaseFirestore,
        measureBoardRepository: MeasureBoardRepository,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
        dashManager: DashManager
    ): TripRepository {
        return TripRepositoryImpl (
            firebaseAuthRepository = firebaseAuthRepository,
            firestore = firestore,
            ioDispatcher = ioDispatcher,
            measureBoardRepository = measureBoardRepository,
            dashManager = dashManager
        )
    }

}
