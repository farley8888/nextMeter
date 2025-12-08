package com.vismo.nextgenmeter

import android.app.Application
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.vismo.nextgenmeter.datastore.DeviceDataStore
import com.vismo.nxgnfirebasemodule.util.LogConstant
import kotlinx.coroutines.flow.first
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.system.exitProcess

@HiltAndroidApp
class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Install crash handler for Firestore corruption FIRST
        setupFirestoreCorruptionHandler()

        val lifecycleObserver = AppLifecycleObserver(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(lifecycleObserver)
    }

    private fun setupFirestoreCorruptionHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            // Check if this is Firestore database corruption
            val isFirestoreCorruption = checkIfFirestoreCorruption(throwable)

            if (isFirestoreCorruption) {
                Log.e(TAG, "🚨🚨🚨 FIRESTORE DATABASE CORRUPTION CRASH DETECTED 🚨🚨🚨")
                Log.e(TAG, "Thread: ${thread.name}")
                Log.e(TAG, "Exception: ${throwable.javaClass.simpleName}")
                Log.e(TAG, "Message: ${throwable.message}")
                Log.e(TAG, "Full stack trace:", throwable)

                // Log to Firebase (synchronously since app is crashing)
                logCorruptionToFirebase(throwable)

                // Check if auto-fix is enabled via Firebase config
                val autoFixEnabled = isAutoFixEnabled()
                Log.d(TAG, "Auto-fix enabled: $autoFixEnabled")

                if (autoFixEnabled) {
                    Log.w(TAG, "Auto-fix is ENABLED - attempting to clear and restart")

                    // Attempt to clear corrupted Firestore database
                    val cleared = clearFirestoreSQLiteDatabase()

                    if (cleared) {
                        Log.i(TAG, "✅ Firestore database cleared, restarting app...")
                        restartApp()
                    } else {
                        Log.e(TAG, "❌ Failed to clear Firestore database")
                        // Let default handler deal with it
                        defaultHandler?.uncaughtException(thread, throwable)
                    }
                } else {
                    Log.w(TAG, "Auto-fix is DISABLED - logging only, will crash normally")
                    Log.w(TAG, "To enable auto-fix, set is_enabled_firestore_corruption_auto_fix=true in Firebase config")
                    // Let default handler deal with it (app will crash)
                    defaultHandler?.uncaughtException(thread, throwable)
                }
            } else {
                // Not Firestore corruption, use default handler
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }
    }

    private fun isAutoFixEnabled(): Boolean {
        return try {
            // Read from SharedPreferences cache (set by RemoteMeterControlRepository)
            val prefs = getSharedPreferences("meter_config_cache", Context.MODE_PRIVATE)
            prefs.getBoolean("is_enabled_firestore_corruption_auto_fix", false)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read auto-fix config, defaulting to false", e)
            false
        }
    }

    private fun logCorruptionToFirebase(throwable: Throwable) {
        try {
            Log.d(TAG, "Attempting to log corruption to Firebase...")

            // Get device ID and meter identifier
            val deviceIdData = runBlocking { DeviceDataStore.deviceIdData.first() }
            val deviceId = deviceIdData?.deviceId ?: "unknown"
            val licensePlate = deviceIdData?.licensePlate ?: "unknown"

            // Build the log entry
            val logMap = mapOf(
                LogConstant.CREATED_BY to LogConstant.CABLE_METER,
                LogConstant.ACTION to "FIRESTORE_DATABASE_CORRUPTION_CRASH",
                LogConstant.SERVER_TIME to FieldValue.serverTimestamp(),
                LogConstant.DEVICE_TIME to Timestamp.now(),
                "thread_name" to Thread.currentThread().name,
                "exception_type" to throwable.javaClass.simpleName,
                "exception_message" to (throwable.message ?: "null"),
                "cause_type" to (throwable.cause?.javaClass?.simpleName ?: "null"),
                "cause_message" to (throwable.cause?.message ?: "null"),
                "stack_trace" to throwable.stackTrace.take(10).joinToString("\n") { it.toString() },
                "auto_fix_enabled" to isAutoFixEnabled(),
                "device_id" to deviceId,
                "license_plate" to licensePlate
            )

            // Try to write directly to Firestore (might fail if corruption is severe)
            FirebaseFirestore.getInstance()
                .collection("meter_devices")
                .document(deviceId)
                .collection("meters")
                .document(licensePlate)
                .collection("logging")
                .add(logMap)
                .addOnSuccessListener {
                    Log.i(TAG, "✅ Successfully logged corruption to Firebase")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "❌ Failed to log corruption to Firebase", e)
                }

            // Give it a moment to send
            Thread.sleep(1000)

        } catch (e: Exception) {
            Log.e(TAG, "Exception while logging to Firebase", e)
        }
    }

    private fun checkIfFirestoreCorruption(throwable: Throwable): Boolean {
        var current: Throwable? = throwable

        // Check the entire exception chain
        while (current != null) {
            val message = current.message ?: ""
            val className = current.javaClass.simpleName

            // Check for Firestore corruption indicators
            if (message.contains("database disk image is malformed", ignoreCase = true) ||
                message.contains("SQLiteDatabaseCorruptException", ignoreCase = true) ||
                className.contains("SQLiteDatabaseCorruptException") ||
                className.contains("SQLiteDatabaseLockedException")) {

                // Also check if it's from Firestore (not our Room database!)
                val stackTrace = current.stackTrace.joinToString("\n") { it.toString() }
                if (stackTrace.contains("com.google.firebase.firestore")) {
                    return true
                }
            }

            current = current.cause
        }

        return false
    }

    private fun clearFirestoreSQLiteDatabase(): Boolean {
        return try {
            Log.w(TAG, "Attempting to clear Firestore SQLite database...")

            // Find and delete ONLY Firestore's SQLite database files
            val databasesDir = getDatabasePath("dummy").parentFile
            var filesDeleted = 0

            if (databasesDir != null && databasesDir.exists()) {
                databasesDir.listFiles()?.forEach { file ->
                    // Firestore uses specific patterns for its database files
                    // Look for files that match Firestore's naming convention
                    val isFirestoreDB = (file.name.startsWith("firestore.") ||
                                        file.name.startsWith("google.")) &&
                                       (file.name.endsWith(".db") ||
                                        file.name.endsWith(".db-journal") ||
                                        file.name.endsWith(".db-wal") ||
                                        file.name.endsWith(".db-shm"))

                    if (isFirestoreDB) {
                        Log.d(TAG, "Deleting Firestore DB file: ${file.name}")
                        if (file.delete()) {
                            filesDeleted++
                            Log.d(TAG, "✅ Deleted: ${file.name}")
                        } else {
                            Log.e(TAG, "❌ Failed to delete: ${file.name}")
                        }
                    }
                }
            }

            Log.i(TAG, "Deleted $filesDeleted Firestore database files")
            filesDeleted > 0

        } catch (e: Exception) {
            Log.e(TAG, "Exception while clearing Firestore database", e)
            false
        }
    }

    private fun restartApp() {
        try {
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)

            startActivity(intent)
            exitProcess(0)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to restart app", e)
            exitProcess(1)
        }
    }

    companion object {
        private const val TAG = "MainApplication"
    }
}
