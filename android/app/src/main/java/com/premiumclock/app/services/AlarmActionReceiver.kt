package com.premiumclock.app.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.premiumclock.app.PremiumClockApplication
import com.premiumclock.app.data.TimeEventEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) { val pending = goAsync(); val app = context.applicationContext as PremiumClockApplication; CoroutineScope(Dispatchers.IO).launch { try { val id = intent.getLongExtra(AlarmReceiver.EXTRA_ALARM_ID, -1); val alarm = app.database.alarmDao().alarm(id) ?: return@launch; when (intent.action) { ACTION_SNOOZE -> { app.alarmScheduler.snooze(alarm); app.database.eventDao().insert(TimeEventEntity(type = "alarm_snoozed", detail = alarm.label)) }; ACTION_DISMISS -> app.database.eventDao().insert(TimeEventEntity(type = "alarm_dismissed", detail = alarm.label)) }; NotificationHelper.cancelAlarm(context, id) } finally { pending.finish() } } }
    companion object { const val ACTION_SNOOZE = "com.premiumclock.SNOOZE"; const val ACTION_DISMISS = "com.premiumclock.DISMISS" }
}
