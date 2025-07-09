package com.vismo.nextgenmeter.util

import android.content.Context
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import java.lang.reflect.Method

object PowerUtil {
    private const val TAG = "PowerUtil"
    
    /**
     * Shutdown device using PowerManager reflection
     * This uses the correct method signature for system apps
     */
    fun shutdownByPower(context: Context) {
        try {
            Log.d(TAG, "shutdownByPower: Initiating shutdown via PowerManager reflection")
            
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            
            // Get the shutdown method with correct signature: shutdown(boolean, String, boolean)
            val shutdownMethod = PowerManager::class.java.getMethod(
                "shutdown", 
                Boolean::class.javaPrimitiveType, 
                String::class.java, 
                Boolean::class.javaPrimitiveType
            )
            
            // Invoke: pm.shutdown(false, null, false)
            shutdownMethod.invoke(pm, false, null, false)
            
            Log.d(TAG, "shutdownByPower: Shutdown method invoked successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "shutdownByPower: Failed to shutdown via PowerManager", e)
            throw e // Re-throw to allow fallback handling
        }
    }
    
    /**
     * Put device to sleep using PowerManager reflection
     */
    fun intoSleepByPower(context: Context) {
        try {
            Log.d(TAG, "intoSleepByPower: Putting device to sleep")
            
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            val goToSleep = PowerManager::class.java.getMethod("goToSleep", Long::class.javaPrimitiveType)
            goToSleep.invoke(pm, SystemClock.uptimeMillis())
            
            Log.d(TAG, "intoSleepByPower: Device sleep method invoked successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "intoSleepByPower: Failed to put device to sleep", e)
            throw e
        }
    }
} 