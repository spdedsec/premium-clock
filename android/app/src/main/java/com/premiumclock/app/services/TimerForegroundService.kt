package com.premiumclock.app.services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.premiumclock.app.PremiumClockApplication
import com.premiumclock.app.data.TimeEventEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class TimerForegroundService : Service() {
    private val scope = CoroutineScope(Dispatchers.IO); private var ticker: Job? = null; private var timerId = -1L; private var name = "Timer"; private var endAt = 0L
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int { when (intent?.action) { ACTION_START -> { timerId = intent.getLongExtra(EXTRA_TIMER_ID, -1); name = intent.getStringExtra(EXTRA_TIMER_NAME) ?: "Timer"; endAt = intent.getLongExtra(EXTRA_END_AT, System.currentTimeMillis()); startForeground(TIMER_FOREGROUND_ID, NotificationHelper.timerNotification(this, timerId, name, endAt - System.currentTimeMillis())); startTicker() }; ACTION_PAUSE -> pauseTimer(intent.getLongExtra(EXTRA_TIMER_ID, timerId)); ACTION_STOP -> stopTimer(intent.getLongExtra(EXTRA_TIMER_ID, timerId)) }; return START_NOT_STICKY }
    private fun startTicker() { ticker?.cancel(); ticker = scope.launch { while (isActive) { val remaining = endAt - System.currentTimeMillis(); if (remaining <= 0) { stopSelf(); return@launch }; getSystemService(android.app.NotificationManager::class.java).notify(TIMER_FOREGROUND_ID, NotificationHelper.timerNotification(this@TimerForegroundService, timerId, name, remaining)); delay(1000) } } }
    private fun pauseTimer(id: Long) = scope.launch { val app = application as PremiumClockApplication; app.database.timerDao().timer(id)?.let { timer -> app.database.timerDao().update(timer.copy(running = false, endAtMillis = null, remainingMillis = (endAt - System.currentTimeMillis()).coerceAtLeast(0))); app.timerScheduler.cancel(id); app.database.eventDao().insert(TimeEventEntity(type = "timer_paused", detail = timer.name)) }; stopSelf() }
    private fun stopTimer(id: Long) = scope.launch { val app = application as PremiumClockApplication; app.database.timerDao().timer(id)?.let { timer -> app.database.timerDao().delete(timer); app.timerScheduler.cancel(id); app.database.eventDao().insert(TimeEventEntity(type = "timer_stopped", detail = timer.name)) }; stopSelf() }
    override fun onDestroy() { ticker?.cancel(); super.onDestroy() }; override fun onBind(intent: Intent?): IBinder? = null
    companion object { const val ACTION_START = "com.premiumclock.timer.START"; const val ACTION_PAUSE = "com.premiumclock.timer.PAUSE"; const val ACTION_STOP = "com.premiumclock.timer.STOP"; const val EXTRA_TIMER_ID = "timer_id"; const val EXTRA_TIMER_NAME = "timer_name"; const val EXTRA_END_AT = "end_at"; const val TIMER_FOREGROUND_ID = 61 }
}
