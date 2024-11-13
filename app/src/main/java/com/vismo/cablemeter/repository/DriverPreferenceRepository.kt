package com.vismo.cablemeter.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.IOException
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import com.google.gson.Gson
import com.vismo.cablemeter.model.Driver
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject

class DriverPreferenceRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson
) {
    private val _driverFlow = MutableStateFlow<Driver>(defaultDriver)
    val driverFlow: StateFlow<Driver> = _driverFlow

    suspend fun loadDriver() {
        try {
            context.driverDataStore.data.collect { driver ->
                _driverFlow.value = driver
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    suspend fun getDriver(): Driver {
        return try {
            context.driverDataStore.data.first()
        } catch (e: IOException) {
            e.printStackTrace()
            defaultDriver
        }
    }

    suspend fun saveDriver(driver: Driver) {
        try {
            context.driverDataStore.updateData { driver }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    suspend fun resetDriver() {
        try {
            context.driverDataStore.updateData { defaultDriver }
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