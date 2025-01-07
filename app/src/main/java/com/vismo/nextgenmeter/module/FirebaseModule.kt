package com.vismo.nextgenmeter.module

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.vismo.nextgenmeter.BuildConfig
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
            .create()
    }

    @Singleton
    @Provides
    fun providesDashManagerConfig(
    ) = DashManagerConfig()

    @Singleton
    @Provides
    fun providesEnv(): String = BuildConfig.FLAVOR

    @Singleton
    @Provides
    fun providesDashManager(
        firestore: FirebaseFirestore,
        gson: Gson,
        dashManagerConfig: DashManagerConfig,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
        env: String
    ): DashManager {
        return DashManager(
            firestore = firestore,
            gson = gson,
            dashManagerConfig = dashManagerConfig,
            ioDispatcher = ioDispatcher,
            env = env
        )
    }
}