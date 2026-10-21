package com.batteryscope.pro.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.batteryscope.pro.data.model.AppSettings
import com.batteryscope.pro.data.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val NOMINAL_CAPACITY = intPreferencesKey("nominal_capacity_mah")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        // 旧設定の互換用（prefer_dark_theme があれば theme_mode に移行）
        val PREFER_DARK = booleanPreferencesKey("prefer_dark_theme")
        val UPDATE_INTERVAL = longPreferencesKey("realtime_update_interval_ms")
        val MONITOR_ENABLED = booleanPreferencesKey("charging_monitor_enabled")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        val themeMode = prefs[Keys.THEME_MODE]?.let { name ->
            runCatching { ThemeMode.valueOf(name) }.getOrNull()
        } ?: when (prefs[Keys.PREFER_DARK]) {
            true -> ThemeMode.DARK
            false -> ThemeMode.LIGHT
            null -> ThemeMode.SYSTEM
        }

        AppSettings(
            nominalCapacityMah = prefs[Keys.NOMINAL_CAPACITY],
            themeMode = themeMode,
            realtimeUpdateIntervalMs = prefs[Keys.UPDATE_INTERVAL] ?: 2000L,
            chargingMonitorEnabled = prefs[Keys.MONITOR_ENABLED] ?: true
        )
    }

    suspend fun setNominalCapacity(mah: Int?) {
        context.dataStore.edit { prefs ->
            if (mah == null || mah <= 0) {
                prefs.remove(Keys.NOMINAL_CAPACITY)
            } else {
                prefs[Keys.NOMINAL_CAPACITY] = mah
            }
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setUpdateInterval(ms: Long) {
        context.dataStore.edit { it[Keys.UPDATE_INTERVAL] = ms }
    }

    suspend fun setMonitorEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.MONITOR_ENABLED] = enabled }
    }
}
