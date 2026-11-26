package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.ConnectionMode
import com.example.model.CustomFirebaseConfig
import com.example.model.MemberAccessStatus
import com.example.ui.theme.DeskThemeId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Locale
import java.util.UUID

private val Context.signalDeskDataStore by preferencesDataStore(name = "signal_desk_prefs")

data class PersistedDeskPreferences(
    val connectionMode: ConnectionMode = ConnectionMode.DEMO,
    val customConfig: CustomFirebaseConfig = CustomFirebaseConfig(),
    val isSoundMuted: Boolean = false,
    val themeId: DeskThemeId = DeskThemeId.OBSIDIAN_GOLD,
    val isLightMode: Boolean = false,
    val clearedBeforeTimestamp: Long = 0L,
    val clearedLiveIds: Set<String> = emptySet(),
    val historyClearedBeforeTimestamp: Long = 0L,
    val clearedHistoryIds: Set<String> = emptySet(),
    val adminPin: String = "2026",
    val backgroundAlertsEnabled: Boolean = false,
    val autoTrackTpSlEnabled: Boolean = false,
    val autoSaveCardImages: Boolean = false,
    val showManualStatusButtons: Boolean = false,
    val deviceId: String = "",
    val memberName: String = "Trader",
    val isOwnerDevice: Boolean = false,
    val liveTodayOnly: Boolean = true,
    val mutedPairs: Set<String> = emptySet(),
    val cachedMemberStatus: MemberAccessStatus = MemberAccessStatus.ACTIVE
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
        val IS_LIGHT_MODE = booleanPreferencesKey("is_light_mode")
        val CLEARED_BEFORE_TS = longPreferencesKey("cleared_before_ts")
        val CLEARED_LIVE_IDS = stringSetPreferencesKey("cleared_live_ids")
        val HISTORY_CLEARED_BEFORE_TS = longPreferencesKey("history_cleared_before_ts")
        val CLEARED_HISTORY_IDS = stringSetPreferencesKey("cleared_history_ids")
        val ADMIN_PIN = stringPreferencesKey("admin_pin")
        val BACKGROUND_ALERTS = booleanPreferencesKey("background_alerts_enabled")
        val AUTO_TRACK_TP_SL = booleanPreferencesKey("auto_track_tp_sl_v2")
        val AUTO_SAVE_CARD_IMAGES = booleanPreferencesKey("auto_save_card_images_v2")
        val SHOW_MANUAL_STATUS_BUTTONS = booleanPreferencesKey("show_manual_status_buttons")
        val DEVICE_ID = stringPreferencesKey("member_device_id")
        val MEMBER_NAME = stringPreferencesKey("member_name")
        val IS_OWNER_DEVICE = booleanPreferencesKey("is_owner_device")
        val LIVE_TODAY_ONLY = booleanPreferencesKey("live_today_only")
        val MUTED_PAIRS = stringSetPreferencesKey("muted_pairs")
        val CACHED_MEMBER_STATUS = stringPreferencesKey("cached_member_status")
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
                isLightMode = prefs[Keys.IS_LIGHT_MODE] ?: false,
                clearedBeforeTimestamp = prefs[Keys.CLEARED_BEFORE_TS] ?: 0L,
                clearedLiveIds = prefs[Keys.CLEARED_LIVE_IDS] ?: emptySet(),
                historyClearedBeforeTimestamp = prefs[Keys.HISTORY_CLEARED_BEFORE_TS] ?: 0L,
                clearedHistoryIds = prefs[Keys.CLEARED_HISTORY_IDS] ?: emptySet(),
                adminPin = prefs[Keys.ADMIN_PIN] ?: "2026",
                backgroundAlertsEnabled = prefs[Keys.BACKGROUND_ALERTS] ?: false,
                autoTrackTpSlEnabled = prefs[Keys.AUTO_TRACK_TP_SL] ?: false,
                autoSaveCardImages = prefs[Keys.AUTO_SAVE_CARD_IMAGES] ?: false,
                showManualStatusButtons = prefs[Keys.SHOW_MANUAL_STATUS_BUTTONS] ?: false,
                deviceId = prefs[Keys.DEVICE_ID] ?: "",
                memberName = prefs[Keys.MEMBER_NAME] ?: "Trader",
                isOwnerDevice = prefs[Keys.IS_OWNER_DEVICE] ?: false,
                liveTodayOnly = prefs[Keys.LIVE_TODAY_ONLY] ?: true,
                mutedPairs = prefs[Keys.MUTED_PAIRS] ?: emptySet(),
                cachedMemberStatus = MemberAccessStatus.fromRaw(prefs[Keys.CACHED_MEMBER_STATUS])
            )
        }

    suspend fun ensureDeviceId(): String {
        var resolved = ""
        context.signalDeskDataStore.edit { prefs ->
            val existing = prefs[Keys.DEVICE_ID]
            if (!existing.isNullOrBlank()) {
                resolved = existing
            } else {
                val created = "DVX-${UUID.randomUUID().toString().replace("-", "").take(8).uppercase(Locale.US)}"
                prefs[Keys.DEVICE_ID] = created
                resolved = created
            }
        }
        return resolved
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

    suspend fun setLightMode(isLightMode: Boolean) {
        context.signalDeskDataStore.edit { prefs ->
            prefs[Keys.IS_LIGHT_MODE] = isLightMode
        }
    }

    suspend fun setClearedFeed(timestampMillis: Long, clearedIds: Set<String>) {
        context.signalDeskDataStore.edit { prefs ->
            prefs[Keys.CLEARED_BEFORE_TS] = timestampMillis
            prefs[Keys.CLEARED_LIVE_IDS] = clearedIds
        }
    }

    suspend fun clearLiveDismissals() {
        context.signalDeskDataStore.edit { prefs ->
            prefs[Keys.CLEARED_BEFORE_TS] = 0L
            prefs[Keys.CLEARED_LIVE_IDS] = emptySet()
        }
    }

    suspend fun setHistoryClearedFeed(timestampMillis: Long, clearedIds: Set<String>) {
        context.signalDeskDataStore.edit { prefs ->
            prefs[Keys.HISTORY_CLEARED_BEFORE_TS] = timestampMillis
            prefs[Keys.CLEARED_HISTORY_IDS] = clearedIds
        }
    }

    suspend fun clearHistoryDismissals() {
        context.signalDeskDataStore.edit { prefs ->
            prefs[Keys.HISTORY_CLEARED_BEFORE_TS] = 0L
            prefs[Keys.CLEARED_HISTORY_IDS] = emptySet()
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

    suspend fun setAutoTrackTpSlEnabled(enabled: Boolean) {
        context.signalDeskDataStore.edit { prefs ->
            prefs[Keys.AUTO_TRACK_TP_SL] = enabled
        }
    }

    suspend fun setAutoSaveCardImages(enabled: Boolean) {
        context.signalDeskDataStore.edit { prefs ->
            prefs[Keys.AUTO_SAVE_CARD_IMAGES] = enabled
        }
    }

    suspend fun setShowManualStatusButtons(show: Boolean) {
        context.signalDeskDataStore.edit { prefs ->
            prefs[Keys.SHOW_MANUAL_STATUS_BUTTONS] = show
        }
    }

    suspend fun setMemberName(name: String) {
        val clean = name.trim().ifBlank { "Trader" }.take(32)
        context.signalDeskDataStore.edit { prefs ->
            prefs[Keys.MEMBER_NAME] = clean
        }
    }

    suspend fun setIsOwnerDevice(isOwner: Boolean) {
        context.signalDeskDataStore.edit { prefs ->
            prefs[Keys.IS_OWNER_DEVICE] = isOwner
        }
    }

    suspend fun setLiveTodayOnly(todayOnly: Boolean) {
        context.signalDeskDataStore.edit { prefs ->
            prefs[Keys.LIVE_TODAY_ONLY] = todayOnly
        }
    }

    suspend fun setMutedPairs(pairs: Set<String>) {
        val normalized = pairs.map { it.trim().uppercase(Locale.US) }.filter { it.isNotBlank() }.toSet()
        context.signalDeskDataStore.edit { prefs ->
            prefs[Keys.MUTED_PAIRS] = normalized
        }
    }

    suspend fun setCachedMemberStatus(status: MemberAccessStatus) {
        context.signalDeskDataStore.edit { prefs ->
            prefs[Keys.CACHED_MEMBER_STATUS] = status.name
        }
    }
}
