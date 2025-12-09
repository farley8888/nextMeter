package com.vismo.nextgenmeter.repository

import android.util.Log
import com.vismo.nextgenmeter.module.IoDispatcher
import com.vismo.nxgnfirebasemodule.DashManager
import com.vismo.nxgnfirebasemodule.DashManagerConfig
import com.vismo.nxgnfirebasemodule.model.isPageLogEnabled
import io.sentry.Sentry
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository responsible for logging page navigation events
 * - Always logs to Android logcat
 * - Conditionally logs to Firebase based on is_enabled_page_log flag
 */
@Singleton
class NavigationLogger @Inject constructor(
    private val dashManager: DashManager,
    private val dashManagerConfig: DashManagerConfig,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    private var currentPage: String? = null
    private val scope = CoroutineScope(ioDispatcher)

    /**
     * Logs navigation from one page to another
     * @param fromPage The page navigating from (null if initial navigation)
     * @param toPage The page navigating to
     */
    fun logNavigation(fromPage: String?, toPage: String) {
        scope.launch {
            val isPageLogEnabled = dashManager.meterSdkConfig.value.isPageLogEnabled

            // Build log message
            val logMessage = buildString {
                append("PageNavigation: ")
                if (fromPage != null) {
                    append("$fromPage -> $toPage")
                } else {
                    append("Initial -> $toPage")
                }
            }

            // Always log to Android logcat
            Log.d(TAG, logMessage)

            // Add Sentry breadcrumb
            Sentry.addBreadcrumb(logMessage, "PageNavigation")

            // Conditionally log to Firebase
            if (isPageLogEnabled) {
                logToFirebase(fromPage, toPage)
            }

            // Update current page
            currentPage = toPage
        }
    }

    /**
     * Logs navigation event to Firebase Firestore
     */
    private suspend fun logToFirebase(fromPage: String?, toPage: String) {
        try {
            val logData = hashMapOf(
                "from_page" to (fromPage ?: "initial"),
                "to_page" to toPage,
                "timestamp" to com.google.firebase.Timestamp.now(),
                "meter_identifier" to dashManagerConfig.meterIdentifier.value,
                "device_id" to dashManagerConfig.deviceID.value
            )

            dashManager.logPageNavigation(logData)
            Log.d(TAG, "Page navigation logged to Firebase: ${fromPage ?: "initial"} -> $toPage")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log page navigation to Firebase", e)
            Sentry.captureException(e)
        }
    }

    /**
     * Gets the current page
     */
    fun getCurrentPage(): String? = currentPage

    companion object {
        private const val TAG = "NavigationLogger"
    }
}
