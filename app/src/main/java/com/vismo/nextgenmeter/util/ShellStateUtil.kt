package com.vismo.nextgenmeter.util

import android.util.Log
import com.ilin.util.ShellUtils
import kotlinx.coroutines.delay

object ShellStateUtil {
    private const val TAG = "ShellStateUtil"
    private const val ACC_SLEEP_STATUS = "1"
    private const val DEBOUNCE_CHECK_INTERVAL = 2L // 2ms
    private const val DEBOUNCE_CONFIRMATION_COUNT = 500 // 500 confirmations needed
    
    fun isACCSleeping(): Boolean {
        val acc = ShellUtils.execShellCmd("cat /sys/class/gpio/gpio75/value")
        return acc == ACC_SLEEP_STATUS
    }
    
    suspend fun isACCSleepingDebounced(): Boolean {
        var consecutiveCount = 0
        var lastStatus: Boolean? = null
        var statusChanges = 0
        val allReadings = mutableListOf<Boolean>()
        
        Log.d(TAG, "Starting debounced ACC sleep check with 500 iterations")
        
        repeat(DEBOUNCE_CONFIRMATION_COUNT) { iteration ->
            val currentStatus = isACCSleeping()
            allReadings.add(currentStatus)
            
            if (currentStatus != lastStatus && lastStatus != null) {
                statusChanges++
                Log.d(TAG, "ACC status change detected at iteration $iteration: $lastStatus -> $currentStatus (total changes: $statusChanges)")
            }
            
            if (currentStatus == lastStatus) {
                consecutiveCount++
            } else {
                consecutiveCount = 1
                lastStatus = currentStatus
            }
            
            // Log every 100 iterations for visibility
            if ((iteration + 1) % 100 == 0) {
                Log.d(TAG, "ACC debounce progress: ${iteration + 1}/500, current status: $currentStatus, consecutive: $consecutiveCount, changes: $statusChanges")
            }
            
            // If we've reached the required consecutive count, return the status
            if (consecutiveCount >= DEBOUNCE_CONFIRMATION_COUNT) {
                Log.d(TAG, "ACC debounce completed early at iteration ${iteration + 1}: status=$currentStatus, total_changes=$statusChanges")
                logFinalSummary(allReadings, statusChanges)
                return currentStatus
            }
            
            delay(DEBOUNCE_CHECK_INTERVAL)
        }
        
        val finalStatus = lastStatus ?: false
        Log.d(TAG, "ACC debounce completed all 500 iterations: final_status=$finalStatus, total_changes=$statusChanges")
        logFinalSummary(allReadings, statusChanges)
        
        // If we complete all checks without reaching consecutive count, return the last status
        return finalStatus
    }
    
    private fun logFinalSummary(allReadings: List<Boolean>, statusChanges: Int) {
        val trueCount = allReadings.count { it }
        val falseCount = allReadings.count { !it }
        val stability = if (statusChanges == 0) "STABLE" else if (statusChanges < 10) "MOSTLY_STABLE" else "UNSTABLE"
        
        Log.d(TAG, "ACC Debounce Summary:")
        Log.d(TAG, "  Total readings: ${allReadings.size}")
        Log.d(TAG, "  True readings: $trueCount (${trueCount * 100 / allReadings.size}%)")
        Log.d(TAG, "  False readings: $falseCount (${falseCount * 100 / allReadings.size}%)")
        Log.d(TAG, "  Status changes: $statusChanges")
        Log.d(TAG, "  Stability: $stability")
        Log.d(TAG, "  First 10 readings: ${allReadings.take(10)}")
        Log.d(TAG, "  Last 10 readings: ${allReadings.takeLast(10)}")
    }

    fun getAndroidId(): String {
        return ShellUtils.execShellCmd("getprop ro.serialno")
    }

    fun getROMVersion(): String {
        return ShellUtils.execShellCmd("getprop firmwareVersion")
    }
} 