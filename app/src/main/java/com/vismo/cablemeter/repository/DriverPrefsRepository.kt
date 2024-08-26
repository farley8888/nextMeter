package com.vismo.cablemeter.repository

import android.util.Log
import androidx.datastore.core.DataStore
import com.vismo.cablemeter.model.DriverPinPrefs
import com.vismo.cablemeter.model.SkippedDrivers
import com.vismo.cablemeter.network.api.MeterOApi
import com.vismo.cablemeter.util.GlobalUtils.encrypt
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

class DriverPrefsRepository @Inject constructor(
    private val driverPrefsDataStore: DataStore<DriverPinPrefs>,
    private val skippedDriversDS: DataStore<SkippedDrivers>,
    private val meterOApi: MeterOApi,
    private val firebaseAuthRepository: FirebaseAuthRepository
) {

    suspend fun getDriverPrefs(): DriverPinPrefs {
        return driverPrefsDataStore.data.first()
    }

    suspend fun updateDriverPrefs(driverPrefs: DriverPinPrefs) {
        driverPrefsDataStore.updateData { driverPrefs }
    }

    suspend fun getSkippedDrivers(): SkippedDrivers {
        return skippedDriversDS.data.first()
    }

    suspend fun updateSkippedDrivers(skippedDrivers: SkippedDrivers) {
        skippedDriversDS.updateData { skippedDrivers }
    }

    suspend fun pairWithDriver(driverId: String): Boolean {
        val response = meterOApi.postPairWithDriver(
            headers = firebaseAuthRepository.getHeaders(),
            driverId = driverId
        )
        return if (response.isSuccessful) {
            Log.d(TAG, "pairWithDriver success: ${response.body()}")
            true
        } else {
            Log.d(TAG, "pairWithDriver error: ${response.errorBody()?.string()}")
            false
        }
    }

    /**
     * Generate the QR code to allow the driver app to scan and bind with the corresponding meter
     */
    fun genQR(meterId: String): String {
        try {
            val version = 1
            val keyVersion = 1
            val licensePlate = meterId //"AZ1099"
            val date = Calendar.getInstance().time
            val sdf = SimpleDateFormat("yyMMddHHmmss", Locale.US)
            val timeStamp = sdf.format(date)

            /**
             *  11 - version + key version
            AZ1099 - lic plate
            2206271010001 - YYMMDDHHMMSS+seed <= encrypted
             */
            val seed = "1"
            return "$version$keyVersion$licensePlate-${encrypt("$timeStamp$seed")}"
        } catch (e: UninitializedPropertyAccessException) {
            return ""
        }
    }

    companion object {
        private const val TAG = "DriverPrefsRepository"
    }
}