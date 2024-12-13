package com.vismo.nextgenmeter.repository

import java.io.File
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject

class LogShippingRepository @Inject constructor(
    private val storageReference: StorageReference,
    private val ioDispatcher: CoroutineDispatcher
) {
    /**
     * Attempts to upload all pending log files, then create a new one, upload it,
     * and handle the logic of deletion if successful.
     */
    suspend fun handleLogUploadFlow(logsDir: File): Flow<Result<Unit>> = flow {
        try {
            // First, upload all pending logs
            uploadAllPendingLogs(logsDir)

            // Now create a new log file from logcat
            val newLogFile = createLogFileFromLogcat(logsDir)

            // Attempt to upload this new file
            val uploadSuccess = uploadFileToFirebase(newLogFile)
            if (uploadSuccess) {
                newLogFile.delete()
            }

            emit(Result.success(Unit))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    private suspend fun uploadAllPendingLogs(logsDir: File) {
        val allLogFiles = logsDir.listFiles()?.filter { it.isFile } ?: return
        for (file in allLogFiles) {
            val uploadSuccess = uploadFileToFirebase(file)
            if (uploadSuccess) {
                file.delete()
            }
        }
    }

    private suspend fun createLogFileFromLogcat(logsDir: File): File = withContext(Dispatchers.IO) {
        if (!logsDir.exists()) {
            logsDir.mkdirs()
        }

        val timeStamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US).format(java.util.Date())
        val logFileName = "logcat_$timeStamp.txt"
        val logFile = File(logsDir, logFileName)
        try {
            val process = Runtime.getRuntime().exec("logcat -d -f ${logFile.absolutePath}")
            process.waitFor()
        } catch (e: java.io.IOException) {
            e.printStackTrace()
        }
        return@withContext logFile
    }

    private suspend fun uploadFileToFirebase(file: File): Boolean = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext false

        return@withContext try {
            storageReference.child(file.name)
                .putFile(android.net.Uri.fromFile(file))
                .await() // Suspends until upload is done
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}