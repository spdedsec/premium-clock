package com.premiumclock.app.ui

import android.app.Application
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.premiumclock.app.PremiumClockApplication
import com.premiumclock.app.data.AlarmEntity
import com.premiumclock.app.data.AppSettings
import com.premiumclock.app.data.BackupPayload
import com.premiumclock.app.data.BackupSettings
import com.premiumclock.app.data.StopwatchSnapshot
import com.premiumclock.app.data.TimeEventEntity
import com.premiumclock.app.data.TimerEntity
import com.premiumclock.app.data.WorldClockEntity
import com.premiumclock.app.services.TimerForegroundService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ClockViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as PremiumClockApplication
    private val alarmsDao = app.database.alarmDao()
    private val timersDao = app.database.timerDao()
    private val worldsDao = app.database.worldClockDao()
    private val eventsDao = app.database.eventDao()
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    val alarms = alarmsDao.observeAlarms().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val timers = timersDao.observeTimers().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val worldClocks = worldsDao.observeWorldClocks().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val events = eventsDao.observeEvents().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val settings = app.preferences.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())
    val stopwatch = app.preferences.stopwatch.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StopwatchSnapshot())

    fun saveAlarm(alarm: AlarmEntity) = viewModelScope.launch {
        val id = if (alarm.id == 0L) alarmsDao.insert(alarm) else { alarmsDao.update(alarm); alarm.id }
        val stored = alarmsDao.alarm(id) ?: return@launch
        app.alarmScheduler.schedule(stored)
        eventsDao.insert(TimeEventEntity(type = if (alarm.id == 0L) "alarm_created" else "alarm_updated", detail = stored.label))
    }

    fun toggleAlarm(alarm: AlarmEntity) = saveAlarm(alarm.copy(enabled = !alarm.enabled))
    fun deleteAlarm(alarm: AlarmEntity) = viewModelScope.launch { app.alarmScheduler.cancel(alarm.id); alarmsDao.delete(alarm); eventsDao.insert(TimeEventEntity(type = "alarm_deleted", detail = alarm.label)) }

    fun createTimer(name: String, minutes: Int, repeating: Boolean = false) = viewModelScope.launch {
        val duration = minutes.coerceAtLeast(1) * 60_000L
        val endAt = System.currentTimeMillis() + duration
        val id = timersDao.insert(TimerEntity(name = name.ifBlank { "$minutes min timer" }, durationMillis = duration, remainingMillis = duration, endAtMillis = endAt, running = true, repeating = repeating))
        startNativeTimer(id, name.ifBlank { "$minutes min timer" }, endAt)
        eventsDao.insert(TimeEventEntity(type = "timer_started", detail = name.ifBlank { "$minutes min timer" }))
    }

    fun toggleTimer(timer: TimerEntity) = viewModelScope.launch {
        if (timer.running) {
            ContextCompat.startForegroundService(getApplication(), Intent(getApplication(), TimerForegroundService::class.java).setAction(TimerForegroundService.ACTION_PAUSE).putExtra(TimerForegroundService.EXTRA_TIMER_ID, timer.id))
        } else {
            val remaining = if (timer.remainingMillis <= 0) timer.durationMillis else timer.remainingMillis
            val endAt = System.currentTimeMillis() + remaining
            timersDao.update(timer.copy(running = true, remainingMillis = remaining, endAtMillis = endAt, updatedAt = System.currentTimeMillis()))
            startNativeTimer(timer.id, timer.name, endAt)
            eventsDao.insert(TimeEventEntity(type = "timer_started", detail = timer.name))
        }
    }

    fun resetTimer(timer: TimerEntity) = viewModelScope.launch {
        app.timerScheduler.cancel(timer.id)
        timersDao.update(timer.copy(running = false, endAtMillis = null, remainingMillis = timer.durationMillis, updatedAt = System.currentTimeMillis()))
    }
    fun deleteTimer(timer: TimerEntity) = viewModelScope.launch { app.timerScheduler.cancel(timer.id); timersDao.delete(timer); eventsDao.insert(TimeEventEntity(type = "timer_deleted", detail = timer.name)) }

    private fun startNativeTimer(id: Long, name: String, endAt: Long) {
        app.timerScheduler.schedule(id, endAt)
        ContextCompat.startForegroundService(getApplication(), Intent(getApplication(), TimerForegroundService::class.java).setAction(TimerForegroundService.ACTION_START).putExtra(TimerForegroundService.EXTRA_TIMER_ID, id).putExtra(TimerForegroundService.EXTRA_TIMER_NAME, name).putExtra(TimerForegroundService.EXTRA_END_AT, endAt))
    }

    fun addWorldClock(zoneId: String, name: String) = viewModelScope.launch { worldsDao.insert(WorldClockEntity(zoneId = zoneId, displayName = name, position = worldClocks.value.size)) }
    fun removeWorldClock(clock: WorldClockEntity) = viewModelScope.launch { worldsDao.delete(clock) }

    fun updateSettings(transform: (AppSettings) -> AppSettings) = viewModelScope.launch { app.preferences.update(transform) }

    fun toggleStopwatch() = viewModelScope.launch {
        val state = stopwatch.value
        val now = System.currentTimeMillis()
        val next = if (state.running) state.copy(running = false, elapsedMillis = state.elapsedMillis + (now - (state.startedAtMillis ?: now)), startedAtMillis = null) else state.copy(running = true, startedAtMillis = now)
        app.preferences.setStopwatch(next)
        if (!state.running) eventsDao.insert(TimeEventEntity(type = "stopwatch_started", detail = "Stopwatch"))
    }
    fun lapStopwatch(displayElapsed: Long) = viewModelScope.launch { val state = stopwatch.value; if (state.running) app.preferences.setStopwatch(state.copy(laps = listOf(displayElapsed) + state.laps)) }
    fun resetStopwatch() = viewModelScope.launch { app.preferences.setStopwatch(StopwatchSnapshot()); eventsDao.insert(TimeEventEntity(type = "stopwatch_reset", detail = "Stopwatch")) }

    suspend fun backupJson(): String = json.encodeToString(BackupPayload(alarms = alarmsDao.all(), timers = timersDao.all(), worldClocks = worldsDao.all(), settings = settings.value.let { BackupSettings(it.theme, it.accent, it.use24Hour, it.showSeconds, it.clockStyle) }))
    fun importBackup(raw: String, onResult: (Boolean) -> Unit) = viewModelScope.launch {
        runCatching { json.decodeFromString<BackupPayload>(raw) }.onSuccess { payload ->
            payload.alarms.forEach { alarm -> val id = alarmsDao.insert(alarm.copy(id = 0)); alarmsDao.alarm(id)?.let(app.alarmScheduler::schedule) }
            payload.timers.forEach { timersDao.insert(it.copy(id = 0, running = false, endAtMillis = null)) }
            payload.worldClocks.forEach { worldsDao.insert(it) }
            app.preferences.update { AppSettings(payload.settings.theme, payload.settings.accent, payload.settings.use24Hour, payload.settings.showSeconds, payload.settings.clockStyle) }
            eventsDao.insert(TimeEventEntity(type = "backup_imported", detail = "Local JSON backup")); onResult(true)
        }.onFailure { onResult(false) }
    }
    fun clearAnalytics() = viewModelScope.launch { eventsDao.clear() }
}
