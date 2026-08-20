package com.premiumclock.app.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.premiumclock.app.PremiumClockApplication
import com.premiumclock.app.data.TimeEventEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) { val pending = goAsync(); val app = context.applicationContext as PremiumClockApplication; CoroutineScope(Dispatchers.IO).launch { try { val id = intent.getLongExtra(EXTRA_ALARM_ID, -1); val alarm = app.database.alarmDao().alarm(id) ?: return@launch; if (intent.getBooleanExtra(EXTRA_PRE_ALARM, false)) NotificationHelper.preAlarmNotification(context, alarm) else { context.getSystemService(android.app.NotificationManager::class.java).notify(NotificationHelper.ALARM_NOTIFICATION_BASE + id.toInt(), NotificationHelper.alarmNotification(context, alarm)); app.database.eventDao().insert(TimeEventEntity(type = "alarm_triggered", detail = alarm.label)); if (!intent.getBooleanExtra(EXTRA_SNOOZE, false)) app.alarmScheduler.rescheduleAfterFire(id) } } finally { pending.finish() } } }
    companion object { const val EXTRA_ALARM_ID = "alarm_id"; const val EXTRA_PRE_ALARM = "pre_alarm"; const val EXTRA_SNOOZE = "snooze" }
}
