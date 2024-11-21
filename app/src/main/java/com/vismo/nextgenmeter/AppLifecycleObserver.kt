package com.vismo.nextgenmeter

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.vismo.nextgenmeter.service.GlobalBackService
import dagger.hilt.android.qualifiers.ApplicationContext

class AppLifecycleObserver(
    @ApplicationContext private val context: Context
) : DefaultLifecycleObserver {
    override fun onStart(owner: LifecycleOwner) {
        // App has entered the foreground
        val backButtonServiceIntent = Intent(context, GlobalBackService::class.java)
        context.stopService(backButtonServiceIntent)
    }

    override fun onStop(owner: LifecycleOwner) {
        // App has entered the background
        val backButtonServiceIntent = Intent(context, GlobalBackService::class.java)
        ContextCompat.startForegroundService(context, backButtonServiceIntent)
    }
}