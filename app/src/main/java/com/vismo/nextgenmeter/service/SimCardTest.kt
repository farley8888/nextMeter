package com.vismo.nextgenmeter.service

import android.content.Context
import android.content.IntentFilter
import android.util.Log

/**
 * Simple helper class to register SIM receiver
 * For Android 8.1 - Just handles receiver registration
 */
class SimCardTest(private val context: Context) {
    private var simReceiver: SimCardStateReceiver? = null
    
    /**
     * Register the SIM receiver to listen for SIM state changes
     * Call this from your Activity's onCreate or onResume
     */
    fun startMonitoring() {
        try {
            simReceiver = SimCardStateReceiver()
            
            val filter = IntentFilter().apply {
                addAction("android.intent.action.SIM_STATE_CHANGED")
            }
            
            context.registerReceiver(simReceiver, filter)
            Log.i(TAG, "✅ SIM receiver registered successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to register SIM receiver", e)
        }
    }
    
    /**
     * Unregister the SIM receiver
     * Call this from your Activity's onDestroy or onPause
     */
    fun stopMonitoring() {
        try {
            simReceiver?.let {
                context.unregisterReceiver(it)
                Log.i(TAG, "📴 SIM receiver unregistered")
            }
            simReceiver = null
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error unregistering SIM receiver", e)
        }
    }
    

    
    companion object {
        private const val TAG = "SimCardTest"
    }
} 