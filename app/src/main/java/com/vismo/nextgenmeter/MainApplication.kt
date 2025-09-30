package com.vismo.nextgenmeter

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import com.vismo.nextgenmeter.service.FirestoreCacheManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class MainApplication : Application() {
    @Inject
    lateinit var firestoreCacheManager: FirestoreCacheManager

    override fun onCreate() {
        super.onCreate()
        val lifecycleObserver = AppLifecycleObserver(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(lifecycleObserver)

        firestoreCacheManager.runCacheCheckOnStartup()
    }
}
