package com.turbodownloader.ui.theme

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class ThemeManager @Inject constructor(
    private val context: Context
) {
    private val darkModeKey = booleanPreferencesKey("dark_mode")
    private val wifiOnlyKey = booleanPreferencesKey("wifi_only")
    private val notificationsKey = booleanPreferencesKey("notifications")
    private val autoRetryKey = booleanPreferencesKey("auto_retry")
    private val speedLimitKey = booleanPreferencesKey("speed_limit")
    private val concurrentDownloadsKey = floatPreferencesKey("concurrent_downloads")
    private val threadsPerDownloadKey = floatPreferencesKey("threads_per_download")
    private val downloadPathKey = stringPreferencesKey("download_path")

    val isDarkMode: Flow<Boolean> = context.dataStore.data.map { it[darkModeKey] ?: false }
    val isWifiOnly: Flow<Boolean> = context.dataStore.data.map { it[wifiOnlyKey] ?: false }
    val isNotifications: Flow<Boolean> = context.dataStore.data.map { it[notificationsKey] ?: true }
    val isAutoRetry: Flow<Boolean> = context.dataStore.data.map { it[autoRetryKey] ?: true }
    val isSpeedLimit: Flow<Boolean> = context.dataStore.data.map { it[speedLimitKey] ?: false }
    val concurrentDownloads: Flow<Float> = context.dataStore.data.map { it[concurrentDownloadsKey] ?: 3f }
    val threadsPerDownload: Flow<Float> = context.dataStore.data.map { it[threadsPerDownloadKey] ?: 4f }
    val downloadPath: Flow<String> = context.dataStore.data.map { it[downloadPathKey] ?: "" }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { it[darkModeKey] = enabled }
    }

    suspend fun setWifiOnly(enabled: Boolean) {
        context.dataStore.edit { it[wifiOnlyKey] = enabled }
    }

    suspend fun setNotifications(enabled: Boolean) {
        context.dataStore.edit { it[notificationsKey] = enabled }
    }

    suspend fun setAutoRetry(enabled: Boolean) {
        context.dataStore.edit { it[autoRetryKey] = enabled }
    }

    suspend fun setSpeedLimit(enabled: Boolean) {
        context.dataStore.edit { it[speedLimitKey] = enabled }
    }

    suspend fun setConcurrentDownloads(value: Float) {
        context.dataStore.edit { it[concurrentDownloadsKey] = value }
    }

    suspend fun setThreadsPerDownload(value: Float) {
        context.dataStore.edit { it[threadsPerDownloadKey] = value }
    }

    suspend fun setDownloadPath(path: String) {
        context.dataStore.edit { it[downloadPathKey] = path }
    }
}
