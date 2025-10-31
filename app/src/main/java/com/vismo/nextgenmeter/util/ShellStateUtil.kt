package com.vismo.nextgenmeter.util

import com.ilin.util.ShellUtils
import kotlinx.coroutines.delay

object ShellStateUtil {
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
        
        repeat(DEBOUNCE_CONFIRMATION_COUNT) {
            val currentStatus = isACCSleeping()
            
            if (currentStatus == lastStatus) {
                consecutiveCount++
            } else {
                consecutiveCount = 1
                lastStatus = currentStatus
            }
            
            // If we've reached the required consecutive count, return the status
            if (consecutiveCount >= DEBOUNCE_CONFIRMATION_COUNT) {
                return currentStatus
            }
            
            delay(DEBOUNCE_CHECK_INTERVAL)
        }
        
        // If we complete all checks without reaching consecutive count, return the last status
        return lastStatus ?: false
    }

    fun getAndroidId(): String {
        return ShellUtils.execShellCmd("getprop ro.serialno")
    }

    fun getROMVersion(): String {
        return ShellUtils.execShellCmd("getprop firmwareVersion")
    }
} 