package com.vismo.nextgenmeter.util

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import com.vismo.nextgenmeter.datastore.TripDataStore
import com.vismo.nextgenmeter.model.TripStatus
import com.vismo.nextgenmeter.repository.InternetConnectivityObserver
import dagger.hilt.android.qualifiers.ApplicationContext
import io.sentry.Sentry
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import javax.inject.Singleton
import java.util.concurrent.atomic.AtomicBoolean

data class RomOtaState(
    val state: Int,
    val value: Int,
    val message: String?
)

@Singleton
class AndroidROMOTAUpdateManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val internetConnectivityObserver: InternetConnectivityObserver
) {
    private val _romOtaEvents = MutableSharedFlow<RomOtaState>(replay = 0, extraBufferCapacity = 64)
    val romOtaEvents: SharedFlow<RomOtaState> = _romOtaEvents

    private var otaStateReceiver: BroadcastReceiver? = null
    private val isReceiverRegistered = AtomicBoolean(false)
    
    
    suspend fun attemptROMUpdate() {
        try {
            if (isSafeToUpdate()) {
                registerOtaReceiverIfNeeded()
                triggerROMUpdate()
                Log.i(TAG, "ROM OTA update triggered successfully")
            } else {
                Log.i(TAG, "ROM OTA update skipped - safety checks failed")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error attempting ROM OTA update: ${e.message}", e)
            Sentry.captureException(e)
        }
    }
    
    private suspend fun isSafeToUpdate(): Boolean {
        val ongoingTrip = TripDataStore.ongoingTripData.firstOrNull()
        val isTripSafe = ongoingTrip == null || ongoingTrip.tripStatus == TripStatus.ENDED
        
        val internetStatus = internetConnectivityObserver.internetStatus.firstOrNull()
        val hasInternet = internetStatus == InternetConnectivityObserver.Status.InternetAvailable
        
        val isSafe = isTripSafe && hasInternet
        
        Log.d(TAG, "Safety check - Trip safe: $isTripSafe, Has internet: $hasInternet, Overall safe: $isSafe")
        
        return isSafe
    }
    
    @SuppressLint("WrongConstant")
    private fun triggerROMUpdate() {
        val intent = Intent("android.intent.action.askillsdk.ota").apply {
            flags = 0x01000000
            component = ComponentName("com.ota.skillsdk", "com.ota.skillsdk.SkillSdkOtaReceiver")
            putExtra("option", 2)
        }
        
        Log.i(TAG, "Sending broadcast to: ${intent.component}")
        Log.i(TAG, "Intent action: ${intent.action}")
        Log.i(TAG, "Intent flags: 0x${Integer.toHexString(intent.flags)}")
        Log.i(TAG, "Intent extras: option=${intent.getIntExtra("option", -1)}")
        
        context.sendBroadcast(intent)
        Log.i(TAG, "ROM OTA broadcast sent")
    }

    @SuppressLint("WrongConstant")
    fun terminateOngoingROMUpdate() {
        val intent = Intent("android.intent.action.askillsdk.ota").apply {
            flags = 0x01000000
            component = ComponentName("com.ota.skillsdk", "com.ota.skillsdk.SkillSdkOtaReceiver")
            putExtra("option", 4)
        }
        context.sendBroadcast(intent)
        Log.i(TAG, "Skipping ROM OTA update")
        unregisterOtaReceiverSafe()
    }
    
    private fun registerOtaReceiverIfNeeded() {
        if (isReceiverRegistered.compareAndSet(false, true)) {
            val filter = IntentFilter().apply {
                addAction("android.intent.action.INSTALL_STATE")
            }
            otaStateReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    val action = intent?.action
                    if ("android.intent.action.INSTALL_STATE" == action) {
                        val state = intent.getIntExtra("state", 0)
                        val value = intent.getIntExtra("value", 0)
                        val msg = intent.getStringExtra("message")
                        Log.i(TAG, "ROM OTA state=$state, value=$value, msg=$msg")
                        _romOtaEvents.tryEmit(RomOtaState(state, value, msg))
                        // Auto-cleanup on terminal-like states
                        if (state == 3 || state == 5 || state == 6) {
                            unregisterOtaReceiverSafe()
                        }
                    }
                }
            }
            try {
                context.registerReceiver(otaStateReceiver, filter)
                Log.i(TAG, "Registered ROM OTA receiver for INSTALL_STATE")
            } catch (e: Exception) {
                isReceiverRegistered.set(false)
                Log.e(TAG, "Failed to register ROM OTA receiver: ${e.message}", e)
            }
        }
    }
    
    private fun unregisterOtaReceiverSafe() {
        if (isReceiverRegistered.compareAndSet(true, false)) {
            try {
                otaStateReceiver?.let { context.unregisterReceiver(it) }
                Log.i(TAG, "Unregistered ROM OTA receiver")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to unregister ROM OTA receiver: ${e.message}", e)
            } finally {
                otaStateReceiver = null
            }
        }
    }
    
    companion object {
        private const val TAG = "ROMOTAUpdateManager"
    }
}