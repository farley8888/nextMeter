package com.vismo.nextgenmeter.repository

import android.content.Context
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.vismo.nextgenmeter.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MeterPreferenceRepository(
    @ApplicationContext private val context: Context,
) {
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = SETTING_PREFS_NAME)

    suspend fun saveTotpSecret(secret: ByteArray) {
        context.dataStore.edit { settings ->
            settings[KEY_TOTP_SECRET] = Base64.encodeToString(secret, Base64.DEFAULT)
        }
    }

    suspend fun getTotpSecret(): Flow<ByteArray?> {
        return context.dataStore.data
            .map { settings ->
                settings[KEY_TOTP_SECRET]?.let { Base64.decode(it, Base64.DEFAULT) }
            }
    }

    suspend fun saveShowLoginToggle(showLoginToggle: Boolean) {
        context.dataStore.edit { settings ->
            settings[KEY_SHOW_LOGIN_TOGGLE] = showLoginToggle.toString()
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
                settings[KEY_SHOW_CONNECTION_ICONS_TOGGLE]?.toBoolean() ?: false
            }
    }

    suspend fun saveLicensePlate(licensePlate: String) {
        context.dataStore.edit { settings ->
            settings[KEY_LICENSE_PLATE] = licensePlate
        }
    }

    suspend fun getLicensePlate(): Flow<String?> {
        return context.dataStore.data
            .map { settings ->
                settings[KEY_LICENSE_PLATE]
            }
    }

    suspend fun saveDeviceId(deviceId: String) {
        context.dataStore.edit { settings ->
            settings[KEY_DEVICE_ID] = deviceId
        }
    }

    suspend fun getDeviceId(): Flow<String?> {
        return context.dataStore.data
            .map { settings ->
                settings[KEY_DEVICE_ID]
            }
    }

    suspend fun saveSelectedLocale(ttsLanguage: String) {
        context.dataStore.edit { settings ->
            settings[KEY_LOCALE] = ttsLanguage
        }
    }

    suspend fun getSelectedLocale(): Flow<String?> {
        return context.dataStore.data
            .map { settings ->
                settings[KEY_LOCALE]
            }
    }

    suspend fun saveMcuStartPrice(mcuStartPrice: String) {
        context.dataStore.edit { settings ->
            settings[KEY_MCU_START_PRICE] = mcuStartPrice
        }
    }

    suspend fun getMcuStartPrice(): Flow<String?> {
        return context.dataStore.data
            .map { settings ->
                settings[KEY_MCU_START_PRICE]
            }
    }

    suspend fun saveOngoingTripId(ongoingTripId: String) {
        context.dataStore.edit { settings ->
            settings[KEY_ONGOING_TRIP_ID] = ongoingTripId
        }
    }

    suspend fun getOngoingTripId(): Flow<String?> {
        return context.dataStore.data
            .map { settings ->
                settings[KEY_ONGOING_TRIP_ID]
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
    }

}