package com.vismo.nxgnfirebasemodule.module

import com.google.firebase.firestore.FirebaseFirestore
import com.vismo.nxgnfirebasemodule.DashManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    @Singleton
    @Provides
    fun providesDashManager(
        firestore: FirebaseFirestore
    ): DashManager {
        return DashManager(
            firestore = firestore
        )
    }
}