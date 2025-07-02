package com.vismo.nextgenmeter.util

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import com.vismo.nextgenmeter.datastore.TripDataStore
import com.vismo.nextgenmeter.model.TripStatus
import com.vismo.nextgenmeter.repository.InternetConnectivityObserver
import dagger.hilt.android.qualifiers.ApplicationContext
import io.sentry.Sentry
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidROMOTAUpdateManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val internetConnectivityObserver: InternetConnectivityObserver
) {
    
    suspend fun attemptROMUpdate() {
        try {
            if (isSafeToUpdate()) {
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
        Log.i(TAG, "ROM OTA broadcast sent with option=2 (device sleeping)")
    }
    
    companion object {
        private const val TAG = "OTAUpdateManager"
    }
}