package com.vismo.nextgenmeter.util

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.ilin.util.ShellUtils
import com.vismo.nextgenmeter.repository.RemoteMeterControlRepository
import com.vismo.nxgnfirebasemodule.model.isDetailAccLogEnabled
import com.vismo.nxgnfirebasemodule.util.LogConstant
import kotlinx.coroutines.delay

object ShellStateUtil {
    private const val TAG = "ShellStateUtil"
    private const val ACC_SLEEP_STATUS = "1"
    private const val DEBOUNCE_CHECK_INTERVAL = 2L // 2ms
    private const val DEBOUNCE_CONFIRMATION_COUNT = 500 // 500 confirmations needed

    // Track last ACC status to only log on changes
    private var lastAccStatus: Boolean? = null
    
    fun isACCSleeping(): Boolean {
        val acc = ShellUtils.execShellCmd("cat /sys/class/gpio/gpio75/value")
        return acc == ACC_SLEEP_STATUS
    }
    
    suspend fun isACCSleepingDebounced(remoteMeterControlRepository: RemoteMeterControlRepository? = null): Boolean {
        var consecutiveCount = 0
        var lastStatus: Boolean? = null
        var statusChanges = 0
        val allReadings = mutableListOf<Boolean>()

        // Check if detailed ACC logging is enabled
        val isDetailedLoggingEnabled = remoteMeterControlRepository?.meterSdkConfiguration?.value.isDetailAccLogEnabled

        if (isDetailedLoggingEnabled) {
            Log.d(TAG, "Starting debounced ACC sleep check with 500 iterations")
        }

        repeat(DEBOUNCE_CONFIRMATION_COUNT) { iteration ->
            val currentStatus = isACCSleeping()
            allReadings.add(currentStatus)

            if (currentStatus != lastStatus && lastStatus != null) {
                statusChanges++
                if (isDetailedLoggingEnabled) {
                    Log.d(TAG, "ACC status change detected at iteration $iteration: $lastStatus -> $currentStatus (total changes: $statusChanges)")
                    Log.d(TAG, "ACC signal unstable - returning false due to status change")
                    logFinalSummary(allReadings, statusChanges, isDetailedLoggingEnabled)
                }
                return false
            }

            if (currentStatus == lastStatus) {
                consecutiveCount++
            } else {
                consecutiveCount = 1
                lastStatus = currentStatus
            }

            // Log every 100 iterations for visibility (only if detailed logging enabled)
            if (isDetailedLoggingEnabled && (iteration + 1) % 100 == 0) {
                Log.d(TAG, "ACC debounce progress: ${iteration + 1}/500, current status: $currentStatus, consecutive: $consecutiveCount, changes: $statusChanges")
            }

            // Only return early if we've completed all iterations with same status
            // Remove early return to always test full 500 iterations

            delay(DEBOUNCE_CHECK_INTERVAL)
        }

        val finalStatus = lastStatus ?: false
        Log.d(TAG, "ACC debounce completed all 500 iterations: final_status=$finalStatus, total_changes=$statusChanges")
        if (isDetailedLoggingEnabled) {
            logFinalSummary(allReadings, statusChanges, isDetailedLoggingEnabled)
        }

        // Only log to Firebase on status change or instability to reduce log volume
        val statusChanged = lastAccStatus != finalStatus
        val isUnstable = statusChanges > 0

        if (statusChanged || isUnstable) {
            remoteMeterControlRepository?.let { repo ->
                val trueCount = allReadings.count { it }
                val falseCount = allReadings.count { !it }
                val stability = if (statusChanges == 0) "STABLE" else if (statusChanges < 10) "MOSTLY_STABLE" else "UNSTABLE"

                val logReason = when {
                    statusChanged && isUnstable -> "status_change_and_unstable"
                    statusChanged -> "status_change"
                    isUnstable -> "unstable_signal"
                    else -> "unknown"
                }

                val logMap = mapOf(
                    LogConstant.CREATED_BY to LogConstant.CABLE_METER,
                    LogConstant.ACTION to "ACC_DEBOUNCE_CHECK",
                    LogConstant.SERVER_TIME to FieldValue.serverTimestamp(),
                    LogConstant.DEVICE_TIME to Timestamp.now(),
                    "final_status" to (if (finalStatus) "sleeping" else "awake"),
                    "previous_status" to (lastAccStatus?.let { if (it) "sleeping" else "awake" } ?: "unknown"),
                    "true_count" to trueCount,
                    "false_count" to falseCount,
                    "status_changes" to statusChanges,
                    "stability" to stability,
                    "total_readings" to allReadings.size,
                    "log_reason" to logReason
                )
                repo.writeToLoggingCollection(logMap)

                Log.d(TAG, "Logged to Firebase - Reason: $logReason")
            }
        } else {
            Log.d(TAG, "Skipped Firebase logging - No status change and stable signal")
        }

        // Update last known status
        lastAccStatus = finalStatus

        // If we complete all checks without reaching consecutive count, return the last status
        return finalStatus
    }
    
    private fun logFinalSummary(allReadings: List<Boolean>, statusChanges: Int, isDetailedLoggingEnabled: Boolean) {
        // Only log detailed summary if detailed logging is enabled
        if (!isDetailedLoggingEnabled) return

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