package com.vismo.nextgenmeter.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.IOException
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import com.google.gson.Gson
import com.vismo.nextgenmeter.model.Driver
import com.vismo.nextgenmeter.util.GlobalUtils.withTransactionSync
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject

class DriverPreferenceRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson
) {

    fun getDriver(): Flow<Driver> {
        return context.driverDataStore.data.map { it }
    }

    suspend fun getDriverOnce(): Driver {
        return try {
            context.driverDataStore.data.first()
        } catch (e: IOException) {
            e.printStackTrace()
            defaultDriver
        }
    }

    suspend fun saveDriver(driver: Driver) {
        try {
            withTransactionSync {
                context.driverDataStore.updateData { driver }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    suspend fun resetDriver() {
        try {
            withTransactionSync {
                context.driverDataStore.updateData { defaultDriver }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private val Context.driverDataStore: DataStore<Driver> by dataStore(fileName = DRIVER_PREFS_NAME, serializer = DriverSerializer)
    init {
        DriverSerializer.gson = gson
    }
    object DriverSerializer: Serializer<Driver> {
        lateinit var gson: Gson

        override suspend fun readFrom(input: InputStream): Driver {
            return try {
                val jsonString = input.bufferedReader().use { it.readText() }
                gson.fromJson(jsonString, Driver::class.java) ?: defaultValue
            } catch (e: IOException) {
                e.printStackTrace()
                defaultValue
            }
        }

        override suspend fun writeTo(t: Driver, output: OutputStream) {
            output.use {
                it.write(gson.toJson(t).toByteArray())
            }
        }

        override val defaultValue: Driver
            get() = defaultDriver
    }

    companion object {
        private const val DRIVER_PREFS_NAME = "driver.json"
        private val defaultDriver = Driver("", "", "", "")
    }
}