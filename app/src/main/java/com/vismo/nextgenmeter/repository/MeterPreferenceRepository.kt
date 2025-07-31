package com.vismo.nextgenmeter.repository

import android.content.Context
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.vismo.nextgenmeter.BuildConfig
import com.vismo.nextgenmeter.util.GlobalUtils.withTransactionSync
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MeterPreferenceRepository(
    @ApplicationContext private val context: Context,
) {
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = SETTING_PREFS_NAME)

    suspend fun saveTotpSecret(secret: ByteArray) {
        withTransactionSync {
            context.dataStore.edit { settings ->
                settings[KEY_TOTP_SECRET] = Base64.encodeToString(secret, Base64.DEFAULT)
            }
        }
    }

    suspend fun getTotpSecret(): Flow<ByteArray?> {
        return context.dataStore.data
            .map { settings ->
                settings[KEY_TOTP_SECRET]?.let { Base64.decode(it, Base64.DEFAULT) }
            }
    }

    suspend fun saveShowLoginToggle(showLoginToggle: Boolean) {
        withTransactionSync {
            context.dataStore.edit { settings ->
                settings[KEY_SHOW_LOGIN_TOGGLE] = showLoginToggle.toString()
            }
        }
    }

    suspend fun getShowLoginToggle(): Flow<Boolean> {
        return context.dataStore.data
            .map { settings ->
                settings[KEY_SHOW_LOGIN_TOGGLE]?.toBoolean() ?: false
            }
    }

    suspend fun saveShowConnectionIconsToggle(showLoginToggle: Boolean) {
        context.dataStore.edit { settings ->
            settings[KEY_SHOW_CONNECTION_ICONS_TOGGLE] = showLoginToggle.toString()
        }
    }

    suspend fun getShowConnectionIconsToggle(): Flow<Boolean> {
        return context.dataStore.data
            .map { settings ->
                settings[KEY_SHOW_CONNECTION_ICONS_TOGGLE]?.toBoolean() ?: true
            }
    }

    suspend fun saveLicensePlate(licensePlate: String) {
        withTransactionSync {
            context.dataStore.edit { settings ->
                settings[KEY_LICENSE_PLATE] = licensePlate
            }
        }
    }

    suspend fun getLicensePlate(): Flow<String?> {
        return context.dataStore.data
            .map { settings ->
                settings[KEY_LICENSE_PLATE]
            }
    }

    suspend fun saveDeviceId(deviceId: String) {
        withTransactionSync {
            context.dataStore.edit { settings ->
                settings[KEY_DEVICE_ID] = deviceId
            }
        }
    }

    suspend fun getDeviceId(): Flow<String?> {
        return context.dataStore.data
            .map { settings ->
                settings[KEY_DEVICE_ID]
            }
    }

    suspend fun saveSelectedLocale(ttsLanguage: String) {
        withTransactionSync {
            context.dataStore.edit { settings ->
                settings[KEY_LOCALE] = ttsLanguage
            }
        }
    }

    suspend fun getSelectedLocale(): Flow<String?> {
        return context.dataStore.data
            .map { settings ->
                settings[KEY_LOCALE]
            }
    }

    suspend fun saveMcuStartPrice(mcuStartPrice: String) {
        withTransactionSync {
            context.dataStore.edit { settings ->
                settings[KEY_MCU_START_PRICE] = mcuStartPrice
            }
        }
    }

    suspend fun getMcuStartPrice(): Flow<String?> {
        return context.dataStore.data
            .map { settings ->
                settings[KEY_MCU_START_PRICE]
            }
    }

    suspend fun saveOngoingTripId(ongoingTripId: String, startTime: Long) {
        withTransactionSync {
            saveOngoingTripStartTime(startTime)
            context.dataStore.edit { settings ->
                settings[KEY_ONGOING_TRIP_ID] = ongoingTripId
            }
        }
    }

    suspend fun getOngoingTripId(): Flow<String?> {
        return context.dataStore.data
            .map { settings ->
                settings[KEY_ONGOING_TRIP_ID]
            }
    }

    private suspend fun saveOngoingTripStartTime(startTime: Long) {
        context.dataStore.edit { settings ->
            settings[KEY_ONGOING_TRIP_START_TIME] = startTime
        }
    }

    suspend fun getOngoingTripStartTime(): Flow<Long?> {
        return context.dataStore.data
            .map { settings ->
                settings[KEY_ONGOING_TRIP_START_TIME]
            }
    }

    suspend fun saveFirmwareFilenameForOTA(fileName: String) {
        withTransactionSync {
            context.dataStore.edit { settings ->
                settings[KEY_FIRMWARE_FILENAME_FOR_OTA] = fileName
            }
        }
    }

    suspend fun getFirmwareFilenameForOTA(): Flow<String?> {
        return context.dataStore.data
            .map { settings ->
                settings[KEY_FIRMWARE_FILENAME_FOR_OTA]
            }
    }

    suspend fun saveRecentlyCompletedUpdateId(id: String) {
        withTransactionSync {
            context.dataStore.edit { settings ->
                settings[KEY_MOST_RECENTLY_COMPLETED_OTA_UPDATE_ID] = id
            }
        }
    }

    fun getRecentlyCompletedUpdateId(): Flow<String?> {
        return context.dataStore.data
            .map { settings ->
                settings[KEY_MOST_RECENTLY_COMPLETED_OTA_UPDATE_ID]
            }
    }

    suspend fun saveAndroidGpsLastUpdateTime(time: Long) {
        withTransactionSync {
            context.dataStore.edit { settings ->
                settings[KEY_ANDROID_GPS_LAST_UPDATE_TIME] = time
            }
        }
    }

    fun getAndroidGpsLastUpdateTime(): Flow<Long?> {
        return context.dataStore.data
            .map { settings ->
                settings[KEY_ANDROID_GPS_LAST_UPDATE_TIME]
            }
    }

    companion object {
        private const val SETTING_PREFS_NAME = "settings"
        private val KEY_TOTP_SECRET = stringPreferencesKey("totp_secret")
        private val KEY_SHOW_LOGIN_TOGGLE = stringPreferencesKey("show_login_toggle")
        private val KEY_SHOW_CONNECTION_ICONS_TOGGLE = stringPreferencesKey("show_connection_icons_toggle")
        private val KEY_LICENSE_PLATE = stringPreferencesKey("license_plate")
        private val KEY_DEVICE_ID = stringPreferencesKey("device_id")
        private val KEY_LOCALE = stringPreferencesKey("selected_language")
        private val KEY_MCU_START_PRICE = stringPreferencesKey("mcu_start_price")
        private val KEY_ONGOING_TRIP_ID = stringPreferencesKey("ongoing_trip_id_${BuildConfig.FLAVOR}")
        private val KEY_ONGOING_TRIP_START_TIME = longPreferencesKey("ongoing_trip_start_time_${BuildConfig.FLAVOR}")
        private val KEY_FIRMWARE_FILENAME_FOR_OTA = stringPreferencesKey("latest_firmware_filename_for_ota")
        private val KEY_MOST_RECENTLY_COMPLETED_OTA_UPDATE_ID = stringPreferencesKey("completed_update_id_${BuildConfig.FLAVOR}")
        private val KEY_ANDROID_GPS_LAST_UPDATE_TIME = longPreferencesKey("android_gps_last_update_time")
    }

}