package com.vismo.cablemeter.module

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.vismo.nxgnfirebasemodule.DashManager
import com.vismo.nxgnfirebasemodule.DashManagerConfig
import com.vismo.nxgnfirebasemodule.model.Heartbeat
import com.vismo.nxgnfirebasemodule.model.HeartbeatSerializer
import com.vismo.nxgnfirebasemodule.model.TimestampDeserializer
import com.vismo.nxgnfirebasemodule.model.TimestampSerializer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    @Singleton
    @Provides
    fun providesGson(): Gson {
        return GsonBuilder()
            .registerTypeAdapter(Heartbeat::class.java, HeartbeatSerializer())
            .registerTypeAdapter(Timestamp::class.java, TimestampSerializer())
            .registerTypeAdapter(Timestamp::class.java, TimestampDeserializer())
            .serializeNulls()
            .create()
    }

    @Singleton
    @Provides
    fun providesDashManagerConfig(
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ) = DashManagerConfig(
        ioDispatcher = ioDispatcher
    )

    @Singleton
    @Provides
    fun providesDashManager(
        firestore: FirebaseFirestore,
        gson: Gson,
        dashManagerConfig: DashManagerConfig,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): DashManager {
        return DashManager(
            firestore = firestore,
            gson = gson,
            dashManagerConfig = dashManagerConfig,
            ioDispatcher = ioDispatcher
        )
    }
}