package com.vismo.nextgenmeter.service

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.ilin.util.ShellUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentLinkedQueue
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@Singleton
class ModuleRestartManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val preferences: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    // Mutex for synchronizing check operations
    private val checkMutex = Mutex()
    
    // State flows for UI updates
    private val _isRestarting = MutableStateFlow(false)
    val isRestarting: StateFlow<Boolean> = _isRestarting.asStateFlow()
    
    private val _restartHistory = MutableStateFlow<List<RestartEvent>>(emptyList())
    val restartHistory: StateFlow<List<RestartEvent>> = _restartHistory.asStateFlow()
    
    // Restart tracking
    private val restartTimestamps = ConcurrentLinkedQueue<Long>()
    private var noNetworkZoneStartTime: Long? = null
    private var consecutiveFailedChecks = 0
    
    // Coroutine scope for background operations
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    init {
        loadRestartHistory()
        cleanupOldRestarts()
    }
    
    /**
     * Main method to check SIM card state and potentially restart module
     * Uses multiple checks with delays before triggering restart
     * Synchronized to prevent concurrent operations
     */
    suspend fun checkAndRestartIfNeeded(isManualTrigger: Boolean = false): RestartResult {
        val sessionId = System.currentTimeMillis() % 10000 // Short ID for this session
        Log.d(TAG, "🆔 Session $sessionId: Starting check (manual: $isManualTrigger)")
        
        // For manual triggers, bypass mutex to allow forced restart
        if (isManualTrigger) {
            return executeCheckSequence(sessionId, isManualTrigger)
        }
        
        // For automatic triggers, use mutex to prevent concurrent operations
        return if (checkMutex.tryLock()) {
            try {
                Log.d(TAG, "🔒 Session $sessionId: Acquired lock, proceeding with check")
                executeCheckSequence(sessionId, isManualTrigger)
            } finally {
                checkMutex.unlock()
                Log.d(TAG, "🔓 Session $sessionId: Released lock")
            }
        } else {
            Log.d(TAG, "⏳ Session $sessionId: Another check in progress, skipping")
            RestartResult.AlreadyRestarting
        }
    }
    
    /**
     * Execute the actual check sequence
     */
    private suspend fun executeCheckSequence(sessionId: Long, isManualTrigger: Boolean): RestartResult {
        Log.d(TAG, "🚀 Session $sessionId: Executing check sequence")
        
        // Perform multiple checks with delays
        val checkResults = performMultipleChecks(sessionId)
        val allChecksFailed = checkResults.all { !it.success }
        val failedCheckCount = checkResults.count { !it.success }
        
        if (!allChecksFailed) {
            consecutiveFailedChecks = 0
            Log.d(TAG, "✅ Session $sessionId: Some checks passed ($failedCheckCount/$CHECK_ATTEMPTS failed), no restart needed")
            return RestartResult.ChecksPassed
        }
        
        consecutiveFailedChecks++
        Log.w(TAG, "❌ Session $sessionId: All $CHECK_ATTEMPTS checks failed (consecutive session failures: $consecutiveFailedChecks)")
        
        // Check if we're in a no-network zone (shouldn't restart)
        if (isInNoNetworkZone() && !isManualTrigger) {
            logEvent("Session $sessionId: Skipping restart - likely in no-network zone")
            return RestartResult.NoNetworkZone
        }
        
        // Check restart rate limiting
        if (!isManualTrigger && !canRestart()) {
            val message = "Session $sessionId: Restart rate limit exceeded (max $MAX_RESTARTS_PER_15_MINUTES per 15 minutes)"
            logEvent(message)
            return RestartResult.RateLimited(message)
        }
        
        // Create detailed failure reason
        val failureDetails = checkResults.mapIndexed { index, result ->
            if (!result.success) "Check${index+1}: ${result.details}" else null
        }.filterNotNull().joinToString("; ")
        
        // Execute restart with detailed failure information
        return executeRestart(sessionId, isManualTrigger, failedCheckCount, failureDetails)
    }
    
    /**
     * Perform 3 checks with 10-second intervals
     */
    private suspend fun performMultipleChecks(sessionId: Long): List<CheckResult> = withContext(Dispatchers.Main) {
        val results = mutableListOf<CheckResult>()
        val startTime = System.currentTimeMillis()
        
        Log.d(TAG, "🔍 Session $sessionId: Starting $CHECK_ATTEMPTS checks with ${CHECK_INTERVAL.inWholeSeconds}s intervals")
        
        repeat(CHECK_ATTEMPTS) { attempt ->
            val checkStartTime = System.currentTimeMillis()
            Log.d(TAG, "⏰ Session $sessionId: Check ${attempt + 1} starting at $checkStartTime")
            
            // Switch to IO for the actual check
            val result = withContext(Dispatchers.IO) {
                performSingleCheck()
            }
            results.add(result)
            
            Log.d(TAG, "📋 Session $sessionId: Check ${attempt + 1}/$CHECK_ATTEMPTS: ${if (result.success) "PASSED" else "FAILED"} - ${result.details}")
            
            if (attempt < CHECK_ATTEMPTS - 1) {
                Log.d(TAG, "⏱️ Session $sessionId: Waiting ${CHECK_INTERVAL.inWholeSeconds} seconds before next check...")
                val delayStart = System.currentTimeMillis()
                delay(CHECK_INTERVAL.inWholeMilliseconds)
                val delayEnd = System.currentTimeMillis()
                val actualDelay = delayEnd - delayStart
                Log.d(TAG, "⏱️ Session $sessionId: Delay completed in ${actualDelay}ms (expected: ${CHECK_INTERVAL.inWholeMilliseconds}ms)")
            }
        }
        
        val totalTime = System.currentTimeMillis() - startTime
        Log.d(TAG, "📊 Session $sessionId: All checks completed in ${totalTime}ms")
        return@withContext results
    }
    
    /**
     * Perform a single SIM card and network check
     */
    private suspend fun performSingleCheck(): CheckResult {
        return try {
            // Check SIM card registration first
            val simCheck = checkSimCardRegistration()
            if (!simCheck.success) {
                return simCheck
            }
            
            // Check network signal strength
            val signalCheck = checkSignalStrength()
            if (!signalCheck.success) {
                return signalCheck
            }
            
            // Check network registration
            val networkCheck = checkNetworkRegistration()
            return networkCheck
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during module check", e)
            CheckResult(false, "Exception during check: ${e.message}")
        }
    }
    
    private fun checkSimCardRegistration(): CheckResult {
        val result = ShellUtils.execShellCmd("getprop gsm.sim.state")
        val isReady = result.contains("READY") || result.contains("LOADED")
        Log.d(TAG, "📱 SIM check: $result -> ${if (isReady) "OK" else "PROBLEM"}")
        return CheckResult(
            success = isReady,
            details = "SIM state: $result"
        )
    }
    
    private fun checkSignalStrength(): CheckResult {
        val result = ShellUtils.execShellCmd("getprop gsm.operator.alpha")
        val hasOperator = result.isNotEmpty() && !result.contains("null") && result.trim() != ""
        Log.d(TAG, "📶 Signal check: $result -> ${if (hasOperator) "OK" else "PROBLEM"}")
        return CheckResult(
            success = hasOperator,
            details = "Operator: $result"
        )
    }
    
    private fun checkNetworkRegistration(): CheckResult {
        val result = ShellUtils.execShellCmd("getprop gsm.network.type")
        val isRegistered = result.isNotEmpty() && !result.contains("unknown") && result.trim() != ""
        Log.d(TAG, "🌐 Network check: $result -> ${if (isRegistered) "OK" else "PROBLEM"}")
        return CheckResult(
            success = isRegistered,
            details = "Network type: $result"
        )
    }
    
    /**
     * Determine if device is likely in a no-network zone
     */
    private fun isInNoNetworkZone(): Boolean {
        val currentTime = System.currentTimeMillis()
        
        // If we haven't detected a no-network zone yet, start tracking
        if (noNetworkZoneStartTime == null) {
            noNetworkZoneStartTime = currentTime
            return false
        }
        
        // If it's been failing for more than NO_NETWORK_THRESHOLD, consider it a no-network zone
        val timeSinceFirstFailure = currentTime - noNetworkZoneStartTime!!
        return timeSinceFirstFailure > NO_NETWORK_THRESHOLD.inWholeMilliseconds
    }
    
    /**
     * Check if restart is allowed based on rate limiting
     */
    private fun canRestart(): Boolean {
        val currentTime = System.currentTimeMillis()
        val fifteenMinutesAgo = currentTime - 15.minutes.inWholeMilliseconds
        
        // Remove timestamps older than 15 minutes
        while (restartTimestamps.isNotEmpty() && restartTimestamps.peek() < fifteenMinutesAgo) {
            restartTimestamps.poll()
        }
        
        return restartTimestamps.size < MAX_RESTARTS_PER_15_MINUTES
    }
    
    /**
     * Execute the actual module restart
     */
    private suspend fun executeRestart(sessionId: Long, isManualTrigger: Boolean, failedCheckCount: Int, failureDetails: String): RestartResult {
        return withContext(Dispatchers.IO) {
            try {
                _isRestarting.value = true
                
                val restartEvent = RestartEvent(
                    timestamp = System.currentTimeMillis(),
                    isManual = isManualTrigger,
                    reason = if (isManualTrigger) {
                        "Manual trigger (Session $sessionId)"
                    } else {
                        "Session $sessionId: $failedCheckCount/$CHECK_ATTEMPTS checks failed - $failureDetails (consecutive failures: $consecutiveFailedChecks)"
                    }
                )
                
                logEvent("🔄 Session $sessionId: Starting 4G module restart - ${restartEvent.reason} | Manual trigger: ${restartEvent.isManual}")
                
                // Execute the restart command (as shown in your original code)
                Log.i(TAG, "💥 Session $sessionId: Executing restart command: setprop persist.boot.gpio1 true")
                ShellUtils.execShellCmd("setprop persist.boot.gpio1 true")

                // Record the restart
                restartTimestamps.offer(System.currentTimeMillis())
                saveRestartEvent(restartEvent)
                
                // Reset failure counters
                consecutiveFailedChecks = 0
                noNetworkZoneStartTime = null
                
                // Wait for module to restart
                Log.i(TAG, "⏳ Session $sessionId: Waiting ${MODULE_RESTART_DELAY.inWholeSeconds} seconds for module restart...")
                delay(MODULE_RESTART_DELAY.inWholeMilliseconds)
                
                logEvent("✅ Session $sessionId: 4G module restart completed")
                
                // Post-restart verification check
                Log.i(TAG, "🔍 Session $sessionId: Starting post-restart verification...")
                delay(2.seconds.inWholeMilliseconds) // Give module extra time to stabilize
                
                val verificationResult = withContext(Dispatchers.IO) {
                    performSingleCheck()
                }
                
                if (verificationResult.success) {
                    Log.i(TAG, "✅ Session $sessionId: Post-restart verification PASSED - module is working")
                    logEvent("✅ Session $sessionId: Restart successful, module verified working")
                } else {
                    Log.w(TAG, "⚠️ Session $sessionId: Post-restart verification FAILED - ${verificationResult.details}")
                    logEvent("⚠️ Session $sessionId: Restart completed but verification failed - ${verificationResult.details}")
                    
                    // Schedule another check after a delay if we're not rate limited
                    if (canRestart()) {
                        Log.i(TAG, "🔄 Session $sessionId: Scheduling follow-up check in 30 seconds...")
                        scheduleFollowUpCheck(sessionId)
                    } else {
                        Log.w(TAG, "⏰ Session $sessionId: Would schedule follow-up but rate limited")
                    }
                }
                
                RestartResult.Success(restartEvent)
                
            } catch (e: Exception) {
                Log.e(TAG, "💥 Session $sessionId: Error during module restart", e)
                RestartResult.Error("Restart failed: ${e.message}")
            } finally {
                _isRestarting.value = false
            }
        }
    }
    
    /**
     * Manual restart trigger for hidden button
     */
    suspend fun manualRestart(): RestartResult {
        Log.i(TAG, "Manual restart triggered")
        return checkAndRestartIfNeeded(isManualTrigger = true)
    }
    
    /**
     * Reset no-network zone detection timer
     * Called when SIM card state indicates network is working
     */
    fun resetNoNetworkZoneDetection() {
        noNetworkZoneStartTime = null
        Log.d(TAG, "No-network zone detection reset - SIM is working properly")
    }
    
    private fun logEvent(message: String) {
        Log.i(TAG, message)
    }
    
    private fun saveRestartEvent(event: RestartEvent) {
        val events = _restartHistory.value.toMutableList()
        events.add(0, event) // Add to beginning
        
        // Keep only last 50 events
        if (events.size > MAX_HISTORY_SIZE) {
            events.removeAt(events.size - 1)
        }
        
        _restartHistory.value = events
        
        // Save to preferences
        val editor = preferences.edit()
        val timestamps = events.map { it.timestamp }.joinToString(",")
        val reasons = events.map { "${it.isManual}:${it.reason}" }.joinToString("|")
        editor.putString(PREF_RESTART_TIMESTAMPS, timestamps)
        editor.putString(PREF_RESTART_REASONS, reasons)
        editor.apply()
    }
    
    private fun loadRestartHistory() {
        val timestamps = preferences.getString(PREF_RESTART_TIMESTAMPS, "")?.split(",")?.mapNotNull { 
            it.toLongOrNull() 
        } ?: emptyList()
        
        val reasons = preferences.getString(PREF_RESTART_REASONS, "")?.split("|")?.map { 
            val parts = it.split(":", limit = 2)
            if (parts.size == 2) {
                parts[0].toBoolean() to parts[1]
            } else {
                false to "Unknown"
            }
        } ?: emptyList()
        
        val events = timestamps.zip(reasons) { timestamp, (isManual, reason) ->
            RestartEvent(timestamp, isManual, reason)
        }
        
        _restartHistory.value = events
    }
    
    private fun cleanupOldRestarts() {
        val fifteenMinutesAgo = System.currentTimeMillis() - 15.minutes.inWholeMilliseconds
        while (restartTimestamps.isNotEmpty() && restartTimestamps.peek() < fifteenMinutesAgo) {
            restartTimestamps.poll()
        }
    }
    
    /**
     * Schedules a follow-up check after a delay
     */
    private fun scheduleFollowUpCheck(originalSessionId: Long) {
        scope.launch {
            delay(30.seconds.inWholeMilliseconds)
            Log.i(TAG, "🔄 Follow-up check triggered by session $originalSessionId")
            checkAndRestartIfNeeded(isManualTrigger = false)
        }
    }
    
    companion object {
        private const val TAG = "ModuleRestartManager"
        private const val PREFS_NAME = "module_restart_prefs"
        private const val PREF_RESTART_TIMESTAMPS = "restart_timestamps"
        private const val PREF_RESTART_REASONS = "restart_reasons"
        
        // Configuration constants
        private const val MAX_RESTARTS_PER_15_MINUTES = 3
        private const val CHECK_ATTEMPTS = 3
        private val CHECK_INTERVAL = 10.seconds
        private val NO_NETWORK_THRESHOLD = 5.minutes
        private val MODULE_RESTART_DELAY = 10.seconds
        private const val MAX_HISTORY_SIZE = 50
    }
}

/**
 * Result of a single module check
 */
data class CheckResult(
    val success: Boolean,
    val details: String
)

/**
 * Event record for restart history
 */
data class RestartEvent(
    val timestamp: Long,
    val isManual: Boolean,
    val reason: String
)

/**
 * Result of restart operation
 */
sealed class RestartResult {
    object ChecksPassed : RestartResult()
    object AlreadyRestarting : RestartResult()
    object NoNetworkZone : RestartResult()
    data class RateLimited(val message: String) : RestartResult()
    data class Success(val event: RestartEvent) : RestartResult()
    data class Error(val message: String) : RestartResult()
} 