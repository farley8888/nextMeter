package com.vismo.cablemeter.module

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.vismo.cablemeter.interfaces.MeasureBoardRepository
import com.vismo.cablemeter.repository.MeasureBoardRepositoryImpl
import com.vismo.cablemeter.interfaces.TripRepository
import com.vismo.cablemeter.repository.TripRepositoryImpl
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
    fun provideMeasureBoardRepository(
        @ApplicationContext context: Context,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): MeasureBoardRepository {
        return MeasureBoardRepositoryImpl(
            context = context,
            ioDispatcher = ioDispatcher
        )
    }

    @Singleton
    @Provides
    fun provideTripRepository(
        auth: FirebaseAuth,
        firestore: FirebaseFirestore,
        measureBoardRepository: MeasureBoardRepository,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): TripRepository {
        return TripRepositoryImpl (
            auth = auth,
            firestore = firestore,
            ioDispatcher = ioDispatcher,
            measureBoardRepository = measureBoardRepository
        )
    }

}
