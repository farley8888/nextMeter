package com.vismo.nextgenmeter.repository

import android.content.Context
import android.util.Log
import com.vismo.nextgenmeter.util.PowerUtil
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class SystemControlRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SystemControlRepository {
    
    private val TAG = "SystemControlRepository"
    
    override suspend fun sleepDevice() {
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "sleepDevice: Putting device to sleep via PowerManager reflection")
                
                // Use PowerUtil to put device to sleep
                PowerUtil.intoSleepByPower(context)
                Log.d(TAG, "sleepDevice: Device sleep initiated successfully")
                
            } catch (e: Exception) {
                Log.e(TAG, "sleepDevice: PowerManager sleep failed", e)
            }
        }
    }
    
    override suspend fun shutdownDevice() {
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "shutdownDevice: Attempting PowerManager shutdown via reflection")
                
                // Primary method: Use PowerUtil.shutdownByPower (PowerManager reflection)
                PowerUtil.shutdownByPower(context)
                Log.d(TAG, "shutdownDevice: PowerManager shutdown initiated successfully")
                
            } catch (e: Exception) {
                Log.e(TAG, "shutdownDevice: PowerManager shutdown failed", e)
            }
        }
    }
} 