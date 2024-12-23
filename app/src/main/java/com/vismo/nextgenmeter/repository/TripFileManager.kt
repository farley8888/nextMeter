package com.vismo.nextgenmeter.repository

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.vismo.nextgenmeter.model.TripData
import com.vismo.nextgenmeter.module.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import javax.inject.Inject

class TripFileManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    private val TAG = "TripFileManager"
    private val fileName = "trips.json"
    private val backupFileName = "trips_backup.json"
    private val maxTrips = 100 // Maximum number of trips to retain

    // Get the file
    private fun getFile(): File = File(context.filesDir, fileName)
    // Get the backup file
    private fun getBackupFile(): File = File(context.filesDir, backupFileName)

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
            Log.e("TripFileManager", "Error loading trips from main file: ${e.localizedMessage}", e)
            // Attempt to load from backup
            loadFromBackup()
        }
    }

    private suspend fun loadFromBackup(): List<TripData> = withContext(ioDispatcher) {
        val backupFile = getBackupFile()
        if (!backupFile.exists()) {
            Log.e("TripFileManager", "Backup trip file does not exist. Returning empty list.")
            return@withContext emptyList<TripData>()
        }

        return@withContext try {
            val json = backupFile.readText()
            val type = object : TypeToken<List<TripData>>() {}.type
            gson.fromJson<List<TripData>>(json, type) ?: emptyList()
        } catch (e: Exception) {
            Log.e("TripFileManager", "Error loading trips from backup file: ${e.localizedMessage}", e)
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
            val trimmedTrips = if (sortedTrips.size > maxTrips) {
                sortedTrips.take(maxTrips)
            } else {
                sortedTrips
            }

            // Serialize trips to JSON
            val json = gson.toJson(trimmedTrips)

            file.outputStream().use { fos ->
                // Write the text data
                fos.write(json.toByteArray(Charsets.UTF_8))
                fos.flush()  // Flushes the stream's internal buffers

                // Force the OS to sync changes to the disk
                fos.fd.sync()
                _descendingSortedTrip.emit(trimmedTrips)
                true
            }
        } catch (e: IOException) {
            Log.e("TripFileManager", "Error saving trips", e)
            false
        }
    }

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
                Log.e("TripFileManager", "Trip with ID ${updatedTrip.internalId} not found for update.")
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
                Log.e("TripFileManager", "Trip with ID $id not found for deletion.")
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
                Log.e("TripFileManager", "Error deleting all trips: ${e.localizedMessage}", e)
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
}