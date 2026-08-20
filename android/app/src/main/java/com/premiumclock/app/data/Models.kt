package com.premiumclock.app.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable @Entity(tableName = "alarms", indices = [Index(value = ["enabled"])])
data class AlarmEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val hour: Int, val minute: Int, val label: String = "Alarm", val repeatDays: String = "", val enabled: Boolean = true, val vibration: Boolean = true, val snoozeMinutes: Int = 9, val maxSnoozes: Int = 3, val strictMode: Boolean = false, val gradualVolume: Boolean = false, val preAlarmMinutes: Int = 0, val createdAt: Long = System.currentTimeMillis()) { fun repeatSet(): Set<Int> = repeatDays.split(",").mapNotNull { it.toIntOrNull() }.toSet() }
@Serializable @Entity(tableName = "timers")
data class TimerEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val name: String, val durationMillis: Long, val remainingMillis: Long = durationMillis, val endAtMillis: Long? = null, val running: Boolean = false, val repeating: Boolean = false, val updatedAt: Long = System.currentTimeMillis())
@Serializable @Entity(tableName = "world_clocks")
data class WorldClockEntity(@PrimaryKey val zoneId: String, val displayName: String, val position: Int = 0, val favorite: Boolean = false)
@Serializable @Entity(tableName = "time_events", indices = [Index(value = ["type"]), Index(value = ["occurredAt"])])
data class TimeEventEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val type: String, val occurredAt: Long = System.currentTimeMillis(), val detail: String = "")
@Serializable data class BackupPayload(val schemaVersion: Int = 1, val alarms: List<AlarmEntity>, val timers: List<TimerEntity>, val worldClocks: List<WorldClockEntity>, val settings: BackupSettings)
@Serializable data class BackupSettings(val theme: String, val accent: String, val use24Hour: Boolean, val showSeconds: Boolean, val clockStyle: String)
data class AppSettings(val theme: String = "system", val accent: String = "vermilion", val use24Hour: Boolean = false, val showSeconds: Boolean = true, val clockStyle: String = "large")
data class StopwatchSnapshot(val running: Boolean = false, val elapsedMillis: Long = 0, val startedAtMillis: Long? = null, val laps: List<Long> = emptyList())
