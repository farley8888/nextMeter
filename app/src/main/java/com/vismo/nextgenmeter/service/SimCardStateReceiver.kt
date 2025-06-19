package com.vismo.nextgenmeter.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Simple SIM card state receiver for Android 8.1
 * Monitors SIM state changes and triggers 4G module restart when needed
 * Uses EntryPointAccessors to get Hilt dependencies
 */
class SimCardStateReceiver : BroadcastReceiver() {
    
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface SimCardStateReceiverEntryPoint {
        fun moduleRestartManager(): ModuleRestartManager
    }
    
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) {
            Log.w(TAG, "Received null context or intent")
            return
        }
        
        val action = intent.action
        Log.d(TAG, "📱 SIM broadcast received: $action")
        
        // Handle SIM state changes
        if (action == "android.intent.action.SIM_STATE_CHANGED") {
            handleSimStateChange(context, intent)
        }
    }
    
    private fun handleSimStateChange(context: Context, intent: Intent) {
        try {
            // Get SIM state from intent extras
            val simState = intent.getStringExtra("state") ?: intent.getStringExtra("ss")
            val reason = intent.getStringExtra("reason")
            
            Log.d(TAG, "🔍 SIM state: $simState, reason: $reason")
            
            // Check if SIM has a problem
            val hasSimProblem = when (simState?.uppercase()) {
                "ABSENT", "NOT_READY", "UNKNOWN", "CARD_IO_ERROR" -> true
                "READY", "LOADED" -> false
                null -> true  // No state reported = problem
                else -> {
                    Log.w(TAG, "⚠️ Unknown SIM state: $simState, treating as problem")
                    true
                }
            }
            
            if (hasSimProblem) {
                Log.w(TAG, "❌ SIM problem detected: $simState - checking if 4G restart needed")
                triggerModuleCheck(context)
            } else {
                Log.d(TAG, "✅ SIM is working fine: $simState - resetting no-network zone detection")
                resetNoNetworkZoneDetection(context)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "💥 Error handling SIM state change", e)
        }
    }
    
    private fun resetNoNetworkZoneDetection(context: Context) {
        try {
            // Get ModuleRestartManager using EntryPointAccessors
            val entryPoint = EntryPointAccessors.fromApplication(
                context.applicationContext,
                SimCardStateReceiverEntryPoint::class.java
            )
            val moduleRestartManager = entryPoint.moduleRestartManager()
            
            // Reset the no-network zone timer
            moduleRestartManager.resetNoNetworkZoneDetection()
            
        } catch (e: Exception) {
            Log.e(TAG, "💥 Error resetting no-network zone detection", e)
        }
    }

    private fun triggerModuleCheck(context: Context) {
        // Create a scope for the restart operation
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        
        scope.launch {
            try {
                Log.i(TAG, "🔄 Triggering 4G module check...")
                
                // Get ModuleRestartManager using EntryPointAccessors
                val entryPoint = EntryPointAccessors.fromApplication(
                    context.applicationContext,
                    SimCardStateReceiverEntryPoint::class.java
                )
                val moduleRestartManager = entryPoint.moduleRestartManager()
                
                // Trigger the check and restart logic
                val result = moduleRestartManager.checkAndRestartIfNeeded()
                
                // Log the result
                when (result) {
                    is RestartResult.Success -> {
                        Log.i(TAG, "✅ 4G module restart SUCCESS")
                    }
                    is RestartResult.ChecksPassed -> {
                        Log.d(TAG, "✅ Module checks PASSED - no restart needed")
                    }
                    is RestartResult.RateLimited -> {
                        Log.w(TAG, "⏰ Restart RATE LIMITED: ${result.message}")
                    }
                    is RestartResult.NoNetworkZone -> {
                        Log.i(TAG, "🚫 Skipped restart - in NO NETWORK zone")
                    }
                    is RestartResult.AlreadyRestarting -> {
                        Log.d(TAG, "🔄 Restart already IN PROGRESS")
                    }
                    is RestartResult.Error -> {
                        Log.e(TAG, "❌ Restart FAILED: ${result.message}")
                    }
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "💥 Error during module restart check", e)
            }
        }
    }
    
    companion object {
        private const val TAG = "SimCardStateReceiver"
    }
} 