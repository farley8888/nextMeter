package com.vismo.nextgenmeter.repository

import android.content.Context
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
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


    companion object {
        private const val SETTING_PREFS_NAME = "settings"
        private val KEY_TOTP_SECRET = stringPreferencesKey("totp_secret")
        private val KEY_SHOW_LOGIN_TOGGLE = stringPreferencesKey("show_login_toggle")
    }

}