package com.vismo.nextgenmeter.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.vismo.nextgenmeter.datastore.DeviceDataStore
import com.vismo.nxgnfirebasemodule.util.LogConstant
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
        fun remoteMeterControlRepository(): com.vismo.nextgenmeter.repository.RemoteMeterControlRepository
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
                // Problem states
                "ABSENT", "NOT_READY", "UNKNOWN", "CARD_IO_ERROR" -> true

                // OK states - SIM is functioning properly
                "READY", "LOADED" -> false

                // Transitional states - SIM is initializing (not a problem)
                "IMSI", "PIN_REQUIRED", "PUK_REQUIRED", "NETWORK_LOCKED" -> {
                    Log.d(TAG, "ℹ️ SIM in transitional state: $simState (this is normal)")
                    false
                }

                null -> true  // No state reported = problem
                else -> {
                    Log.w(TAG, "⚠️ Unknown SIM state: $simState, treating as problem")
                    true
                }
            }
            
            // Log to Firebase if state changed or there's a problem
            val stateChanged = lastSimState != simState
            val problemStatusChanged = lastHasSimProblem != hasSimProblem

            if (stateChanged || problemStatusChanged || hasSimProblem) {
                logToFirebase(context, simState, reason, hasSimProblem, stateChanged, problemStatusChanged)
            } else {
                Log.d(TAG, "📊 Skipped Firebase logging - No state change and no problem")
            }

            // Update last known state
            lastSimState = simState
            lastHasSimProblem = hasSimProblem

            if (hasSimProblem) {
                Log.w(TAG, "❌ SIM problem detected: $simState - checking if 4G restart needed")
                DeviceDataStore.setIsSimCardAvailable(false)
                triggerModuleCheck(context)
            } else {
                Log.d(TAG, "✅ SIM is working fine: $simState - resetting no-network zone detection")
                DeviceDataStore.setIsSimCardAvailable(true)
                resetNoNetworkZoneDetection(context)
            }

        } catch (e: Exception) {
            Log.e(TAG, "💥 Error handling SIM state change", e)
        }
    }

    private fun logToFirebase(
        context: Context,
        simState: String?,
        reason: String?,
        hasSimProblem: Boolean,
        stateChanged: Boolean,
        problemStatusChanged: Boolean
    ) {
        try {
            val entryPoint = EntryPointAccessors.fromApplication(
                context.applicationContext,
                SimCardStateReceiverEntryPoint::class.java
            )
            val remoteMeterControlRepository = entryPoint.remoteMeterControlRepository()

            val logReason = when {
                stateChanged && problemStatusChanged && hasSimProblem -> "state_and_problem_status_changed_with_problem"
                stateChanged && problemStatusChanged -> "state_and_problem_status_changed"
                stateChanged && hasSimProblem -> "state_changed_with_problem"
                stateChanged -> "state_changed"
                problemStatusChanged && hasSimProblem -> "problem_status_changed_with_problem"
                problemStatusChanged -> "problem_status_changed"
                hasSimProblem -> "has_problem"
                else -> "unknown"
            }

            val logMap = mapOf(
                LogConstant.CREATED_BY to LogConstant.CABLE_METER,
                LogConstant.ACTION to "SIM_STATE_CHANGE",
                LogConstant.SERVER_TIME to FieldValue.serverTimestamp(),
                LogConstant.DEVICE_TIME to Timestamp.now(),
                "sim_state" to (simState ?: "unknown"),
                "previous_sim_state" to (lastSimState ?: "unknown"),
                "reason" to (reason ?: "unknown"),
                "has_problem" to hasSimProblem,
                "previous_has_problem" to (lastHasSimProblem ?: false),
                "state_changed" to stateChanged,
                "problem_status_changed" to problemStatusChanged,
                "log_reason" to logReason
            )

            remoteMeterControlRepository.writeToLoggingCollection(logMap)
            Log.d(TAG, "📊 Logged to Firebase - Reason: $logReason, State: $simState, Problem: $hasSimProblem")

        } catch (e: Exception) {
            Log.e(TAG, "💥 Error logging SIM state to Firebase", e)
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

        // Track last SIM state to only log on changes
        @Volatile
        private var lastSimState: String? = null

        @Volatile
        private var lastHasSimProblem: Boolean? = null
    }
} 