package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.ConnectionMode
import com.example.model.CustomFirebaseConfig
import com.example.ui.theme.DeskThemeId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.signalDeskDataStore by preferencesDataStore(name = "signal_desk_prefs")

data class PersistedDeskPreferences(
    val connectionMode: ConnectionMode = ConnectionMode.DEMO,
    val customConfig: CustomFirebaseConfig = CustomFirebaseConfig(),
    val isSoundMuted: Boolean = false,
    val themeId: DeskThemeId = DeskThemeId.OBSIDIAN_GOLD,
    val clearedBeforeTimestamp: Long = 0L,
    val adminPin: String = "2026",
    val backgroundAlertsEnabled: Boolean = false
)

class ConfigPreferencesStore(private val context: Context) {
    private object Keys {
        val CONNECTION_MODE = stringPreferencesKey("connection_mode")
        val API_KEY = stringPreferencesKey("fb_api_key")
        val AUTH_DOMAIN = stringPreferencesKey("fb_auth_domain")
        val DATABASE_URL = stringPreferencesKey("fb_database_url")
        val PROJECT_ID = stringPreferencesKey("fb_project_id")
        val APP_ID = stringPreferencesKey("fb_app_id")
        val SOUND_MUTED = booleanPreferencesKey("sound_muted")
        val THEME_ID = stringPreferencesKey("theme_id")
        val CLEARED_BEFORE_TS = longPreferencesKey("cleared_before_ts")
        val ADMIN_PIN = stringPreferencesKey("admin_pin")
        val BACKGROUND_ALERTS = booleanPreferencesKey("background_alerts_enabled")
    }

    val preferencesFlow: Flow<PersistedDeskPreferences> =
        context.signalDeskDataStore.data.map { prefs ->
            val modeRaw = prefs[Keys.CONNECTION_MODE] ?: ConnectionMode.DEMO.name
            val mode = ConnectionMode.entries.firstOrNull { it.name == modeRaw } ?: ConnectionMode.DEMO
            PersistedDeskPreferences(
                connectionMode = mode,
                customConfig = CustomFirebaseConfig(
                    apiKey = prefs[Keys.API_KEY] ?: "",
                    authDomain = prefs[Keys.AUTH_DOMAIN] ?: "",
                    databaseURL = prefs[Keys.DATABASE_URL] ?: "",
                    projectId = prefs[Keys.PROJECT_ID] ?: "",
                    appId = prefs[Keys.APP_ID] ?: ""
                ),
                isSoundMuted = prefs[Keys.SOUND_MUTED] ?: false,
                themeId = DeskThemeId.fromRaw(prefs[Keys.THEME_ID]),
                clearedBeforeTimestamp = prefs[Keys.CLEARED_BEFORE_TS] ?: 0L,
                adminPin = prefs[Keys.ADMIN_PIN] ?: "2026",
                backgroundAlertsEnabled = prefs[Keys.BACKGROUND_ALERTS] ?: false
            )
        }

    suspend fun saveCustomConfig(config: CustomFirebaseConfig, mode: ConnectionMode) {
        context.signalDeskDataStore.edit { prefs ->
            prefs[Keys.CONNECTION_MODE] = mode.name
            prefs[Keys.API_KEY] = config.apiKey
            prefs[Keys.AUTH_DOMAIN] = config.authDomain
            prefs[Keys.DATABASE_URL] = config.databaseURL
            prefs[Keys.PROJECT_ID] = config.projectId
            prefs[Keys.APP_ID] = config.appId
        }
    }

    suspend fun setConnectionMode(mode: ConnectionMode) {
        context.signalDeskDataStore.edit { prefs ->
            prefs[Keys.CONNECTION_MODE] = mode.name
        }
    }

    suspend fun setSoundMuted(muted: Boolean) {
        context.signalDeskDataStore.edit { prefs ->
            prefs[Keys.SOUND_MUTED] = muted
        }
    }

    suspend fun setThemeId(themeId: DeskThemeId) {
        context.signalDeskDataStore.edit { prefs ->
            prefs[Keys.THEME_ID] = themeId.name
        }
    }

    suspend fun setClearedBeforeTimestamp(timestampMillis: Long) {
        context.signalDeskDataStore.edit { prefs ->
            prefs[Keys.CLEARED_BEFORE_TS] = timestampMillis
        }
    }

    suspend fun setAdminPin(newPin: String) {
        context.signalDeskDataStore.edit { prefs ->
            prefs[Keys.ADMIN_PIN] = newPin
        }
    }

    suspend fun setBackgroundAlertsEnabled(enabled: Boolean) {
        context.signalDeskDataStore.edit { prefs ->
            prefs[Keys.BACKGROUND_ALERTS] = enabled
        }
    }
}
