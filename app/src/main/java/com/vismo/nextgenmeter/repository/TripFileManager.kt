package com.vismo.nextgenmeter.repository

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.vismo.nextgenmeter.model.TripData
import com.vismo.nextgenmeter.module.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import javax.inject.Inject

class TripFileManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {

    // Get the file
    private fun getFile(): File = File(context.filesDir, FILE_NAME)
    private val _descendingSortedTrip = MutableStateFlow<List<TripData>>(emptyList())
    val descendingSortedTrip: StateFlow<List<TripData>> = _descendingSortedTrip
    private val mutex = Mutex()

    // Load trips from the file
    private suspend fun loadTrips(): List<TripData> = withContext(ioDispatcher) {
        val originalFile = getFile()
        if (!originalFile.exists()) {
            Log.d(TAG, "Trip file does not exist. Returning empty list.")
            return@withContext emptyList<TripData>()
        }

        return@withContext try {
            Log.d(TAG, "load trip from ${originalFile.path}")
            val json = originalFile.readText()
            val type = object : TypeToken<List<TripData>>() {}.type
            gson.fromJson<List<TripData>>(json, type) ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error loading trips from backup file: ${e.localizedMessage}", e)
            emptyList<TripData>()
        }
    }

    // Save trips to the file atomically
    private suspend fun saveTrips(trips: List<TripData>): Boolean = withContext(ioDispatcher) {
        val file = getFile()
        Log.d(TAG, "save trip at ${file.path}")
        return@withContext try {
            val sortedTrips = trips.sortedByDescending { trip ->
                trip.endTime ?: com.google.firebase.Timestamp.now()
            }

            // Trim the list to the maximum allowed trips
            val trimmedTrips = if (sortedTrips.size > MAX_TRIPS) {
                sortedTrips.take(MAX_TRIPS)
            } else {
                sortedTrips
            }

            // Serialize trips to JSON
            val json = try {
                gson.toJson(trimmedTrips)
            } catch (e: Exception) {
                Log.e(TAG, "Error serializing trips to JSON", e)
                return@withContext false
            }

            file.outputStream().use { fos ->
                // Write the text data
                fos.write(json.toByteArray(Charsets.UTF_8))
                fos.flush()  // Flushes the stream's internal buffers

                // Force the OS to sync changes to the disk
                fos.fd.sync()
            }
            if (!forceOsWideSync()) {
                Log.e(TAG, "OS-wide sync failed")
            }

            try {
                _descendingSortedTrip.emit(trimmedTrips)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to emit trips", e)
            }

            Log.d(TAG, "Trips saved successfully")
            true

        } catch (e: IOException) {
            Log.e(TAG, "Error saving trips", e)
            false
        }
    }

    /**
     * Some how our meter is not able to sync the file to disk, so we are forcing the OS to sync the file to disk.
     */
    private suspend fun forceOsWideSync(): Boolean = withContext(Dispatchers.IO) {
        try {
            val process = Runtime.getRuntime().exec("sync")
            val exitCode = process.waitFor()
            if (exitCode == 0) {
                Log.d(TAG, "Sync command executed successfully.")
                true
            } else {
                val errorMessage = process.errorOutput()
                Log.e(TAG, "Sync command failed with exit code $exitCode. Error: $errorMessage")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception occurred while executing sync: ${e.message}")
            false
        }
    }

    private fun Process.errorOutput(): String =
        errorStream.bufferedReader().use { it.readText() }

    // Add a trip
    suspend fun addTrip(newTrip: TripData): Boolean = withContext(ioDispatcher) {
        mutex.withLock {
            val trips = loadTrips().toMutableList()
            trips.add(newTrip)
            saveTrips(trips)
        }
    }

    // Update a trip
    suspend fun updateTrip(updatedTrip: TripData): Boolean = withContext(ioDispatcher) {
        mutex.withLock {
            val trips = loadTrips().toMutableList()
            val index = trips.indexOfFirst { it.internalId == updatedTrip.internalId }
            return@withLock if (index != -1) {
                trips[index] = updatedTrip
                saveTrips(trips)
            } else {
                Log.e(TAG, "Trip with ID ${updatedTrip.internalId} not found for update.")
                false
            }
        }
    }

    // Delete a trip
    suspend fun deleteTrip(id: Long): Boolean = withContext(ioDispatcher) {
        mutex.withLock {
            val trips = loadTrips().toMutableList()
            val removed = trips.removeAll { it.internalId == id }
            return@withLock if (removed) {
                saveTrips(trips)
            } else {
                Log.e(TAG, "Trip with ID $id not found for deletion.")
                false
            }
        }
    }

    suspend fun deleteAllTrips(): Boolean = withContext(ioDispatcher) {
        mutex.withLock {
            try {
                // Save an empty list of trips
                val saveSuccessful = saveTrips(emptyList())
                saveSuccessful
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting all trips: ${e.localizedMessage}", e)
                false
            }
        }
    }

    suspend fun initializeTrips() {
        mutex.withLock {
            val trips = loadTrips()
            _descendingSortedTrip.emit(trips)
        }
    }

    companion object {
        private const val TAG = "TripFileManager"
        private const val FILE_NAME = "trips.json"
        private const val MAX_TRIPS = 100 // Maximum number of trips to retain
    }
}