package com.vismo.nextgenmeter.service

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.vismo.nextgenmeter.module.IoDispatcher
import com.vismo.nextgenmeter.repository.MeterPreferenceRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreCacheManager @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val meterPreferenceRepository: MeterPreferenceRepository,
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    private val TAG = "FirestoreCacheManager"

    fun runCacheCheckOnStartup() {
        CoroutineScope(ioDispatcher).launch {
            val shouldClear = meterPreferenceRepository.getWasMeterOnlineAtLastAccOff().firstOrNull() == true
            val isTripOngoing = !meterPreferenceRepository.getOngoingTripId().firstOrNull().isNullOrBlank()

            val cm = context.getSystemService(ConnectivityManager::class.java)
            val isOnline = cm.activeNetwork?.let { net ->
                cm.getNetworkCapabilities(net)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
            } ?: false

            Log.d(TAG, "Checking conditions: shouldClear=$shouldClear, isTripOngoing=$isTripOngoing, isOnline=$isOnline")

            if (shouldClear && isOnline && !isTripOngoing) {
                Log.w(TAG, "Conditions met. Attempting to clear Firestore persistence.")
                // To be safe, we must terminate first.
                try {
                    firestore.terminate().await()
                    firestore.clearPersistence().await()
                    Log.d(TAG, "Firestore persistence cleared successfully! App must be restarted.")
                    restartApp()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to clear Firestore persistence.", e)
                }
            }
        }
    }

    private fun restartApp() {
        val packageManager = context.packageManager
        val intent = packageManager.getLaunchIntentForPackage(context.packageName)
        val componentName = intent!!.component
        val mainIntent = Intent.makeRestartActivityTask(componentName)
        context.startActivity(mainIntent)
        Runtime.getRuntime().exit(0)
    }
}