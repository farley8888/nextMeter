package com.vismo.nextgenmeter.repository

import android.util.Log
import com.vismo.nextgenmeter.util.PowerUtil
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class SystemControlRepository @Inject constructor(
    @ApplicationContext private val context: android.content.Context
) {
    private val TAG = "SystemControlRepository"

    suspend fun shutdownDevice() {
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