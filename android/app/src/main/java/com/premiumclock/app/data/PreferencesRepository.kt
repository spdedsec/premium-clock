package com.premiumclock.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "premium_clock_settings")
class PreferencesRepository(private val context: Context) {
    private object Keys { val theme = stringPreferencesKey("theme"); val accent = stringPreferencesKey("accent"); val use24Hour = booleanPreferencesKey("use_24_hour"); val showSeconds = booleanPreferencesKey("show_seconds"); val clockStyle = stringPreferencesKey("clock_style"); val stopwatchRunning = booleanPreferencesKey("stopwatch_running"); val stopwatchElapsed = longPreferencesKey("stopwatch_elapsed"); val stopwatchStartedAt = longPreferencesKey("stopwatch_started_at"); val stopwatchLaps = stringPreferencesKey("stopwatch_laps") }
    val settings: Flow<AppSettings> = context.dataStore.data.map { values -> AppSettings(values[Keys.theme] ?: "system", values[Keys.accent] ?: "vermilion", values[Keys.use24Hour] ?: false, values[Keys.showSeconds] ?: true, values[Keys.clockStyle] ?: "large") }
    val stopwatch: Flow<StopwatchSnapshot> = context.dataStore.data.map { values -> StopwatchSnapshot(values[Keys.stopwatchRunning] ?: false, values[Keys.stopwatchElapsed] ?: 0L, values[Keys.stopwatchStartedAt], values[Keys.stopwatchLaps]?.split(",")?.mapNotNull { it.toLongOrNull() } ?: emptyList()) }
    suspend fun update(transform: (AppSettings) -> AppSettings) { context.dataStore.edit { values -> val next = transform(AppSettings(values[Keys.theme] ?: "system", values[Keys.accent] ?: "vermilion", values[Keys.use24Hour] ?: false, values[Keys.showSeconds] ?: true, values[Keys.clockStyle] ?: "large")); values[Keys.theme] = next.theme; values[Keys.accent] = next.accent; values[Keys.use24Hour] = next.use24Hour; values[Keys.showSeconds] = next.showSeconds; values[Keys.clockStyle] = next.clockStyle } }
    suspend fun setStopwatch(snapshot: StopwatchSnapshot) { context.dataStore.edit { values -> values[Keys.stopwatchRunning] = snapshot.running; values[Keys.stopwatchElapsed] = snapshot.elapsedMillis; snapshot.startedAtMillis?.let { values[Keys.stopwatchStartedAt] = it } ?: values.remove(Keys.stopwatchStartedAt); values[Keys.stopwatchLaps] = snapshot.laps.joinToString(",") } }
}
