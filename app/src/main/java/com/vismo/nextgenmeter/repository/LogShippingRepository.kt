package com.vismo.nextgenmeter.repository

import android.content.Context
import android.util.Log
import java.io.File
import com.google.firebase.storage.StorageReference
import com.vismo.nextgenmeter.module.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.IOException
import javax.inject.Inject

class LogShippingRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val storageReference: StorageReference,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val meterPreferenceRepository: MeterPreferenceRepository,
) {
    private val TAG = "LogShippingRepository"

    /**
     * Attempts to:
     * 1. Create a new log file from logcat (if fails, we still proceed with upload attempts).
     * 2. Upload all files in the "logcat" directory.
     *
     * Success is defined as having at least one file successfully uploaded.
     * Returns Result<Int> representing the number of files successfully uploaded.
     */
    suspend fun handleLogUploadFlow(): Result<Int> = withContext(ioDispatcher) {
        Log.d(TAG, "Starting log upload flow")
        return@withContext try {
            // Create a new log file (if this fails, we still try to upload old files)
            val logcatExecutedSuccessfully = createLogFileFromLogcat()
            if (!logcatExecutedSuccessfully) {
                Log.e(TAG, "Failed to create new log file from logcat, proceeding with any existing files.")
            }

            // Upload all files in the directory
            val logcatDir = File(context.filesDir, "logcat")
            val files = logcatDir.listFiles()?.toList().orEmpty()
            if (files.isEmpty()) {
                // No files to upload means no success
                return@withContext Result.failure<Int>(Exception("No log files available to upload"))
            }

            // Retrieve license plate from the flow
            val licensePlate = meterPreferenceRepository.getLicensePlate().firstOrNull()
            // If null or blank, you could fallback to a default path segment, e.g. "unknown"
            val finalLicensePlate = if (licensePlate.isNullOrBlank()) "unknown" else licensePlate

            var uploadedCount = 0
            for (file in files) {
                val uploadSuccess = uploadFileToFirebase(file, finalLicensePlate)
                if (uploadSuccess) {
                    uploadedCount++
                    // Delete the file only after successful upload
                    file.delete()
                } else {
                    // return immediately and not to try other files
                    return@withContext Result.failure<Int>(Exception("Failed to upload."))
                }
            }

            if (uploadedCount > 0) {
                Result.success(uploadedCount)
            } else {
                // None of the files succeeded in uploading
                Result.failure<Int>(Exception("Failed to upload any log files"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in log upload flow", e)
            Result.failure(e)
        }
    }

    private suspend fun createLogFileFromLogcat(): Boolean = withContext(ioDispatcher) {
        val timeStamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US).format(java.util.Date())
        val logFileName = "logcat_$timeStamp.txt"
        val logDir = File(context.filesDir, "logcat")
        val logFile = File(logDir, logFileName)
        return@withContext try {
            logDir.mkdirs()
            val process = Runtime.getRuntime().exec("logcat -d -f ${logFile.absolutePath}")
            val exitValue = process.waitFor()
            exitValue == 0
        } catch (e: IOException) {
            Log.e(TAG, "Error creating log file from logcat", e)
            false
        }
    }

    private suspend fun uploadFileToFirebase(file: File, licensePlate: String): Boolean = withContext(ioDispatcher) {
        if (!file.exists()) {
            Log.e(TAG, "File does not exist: ${file.name}")
            return@withContext false
        }

        return@withContext try {
            withTimeout(15_000) {
                storageReference.child(licensePlate).child(file.name)
                    .putFile(android.net.Uri.fromFile(file))
                    .await() // Suspends until upload completes or fails
                val url = storageReference.child(licensePlate).child(file.name).downloadUrl.await()
                Log.d(TAG, "File uploaded at: $url")
                true
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Upload timed out for file: ${file.name}", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload file: ${file.name}", e)
            false
        }
    }
}