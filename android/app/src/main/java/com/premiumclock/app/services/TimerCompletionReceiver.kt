package com.premiumclock.app.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.premiumclock.app.PremiumClockApplication
import com.premiumclock.app.data.TimeEventEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TimerCompletionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) { val pending = goAsync(); val app = context.applicationContext as PremiumClockApplication; CoroutineScope(Dispatchers.IO).launch { try { val id = intent.getLongExtra(TimerForegroundService.EXTRA_TIMER_ID, -1); val timer = app.database.timerDao().timer(id) ?: return@launch; if (timer.running && (timer.endAtMillis ?: Long.MAX_VALUE) <= System.currentTimeMillis()) { app.database.timerDao().update(timer.copy(running = false, endAtMillis = null, remainingMillis = 0)); app.database.eventDao().insert(TimeEventEntity(type = "timer_completed", detail = timer.name)); NotificationHelper.timerCompleted(context, timer.name) } } finally { pending.finish() } } }
}
