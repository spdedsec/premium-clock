package com.premiumclock.app.services

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.premiumclock.app.data.AlarmDao
import com.premiumclock.app.data.AlarmEntity
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.ZoneId

class AlarmScheduler(private val context: Context, private val alarmDao: AlarmDao) {
    private val manager = context.getSystemService(AlarmManager::class.java)
    fun schedule(alarm: AlarmEntity) { cancel(alarm.id); if (!alarm.enabled) return; val next = nextOccurrence(alarm) ?: return; val trigger = next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(); manager.setAlarmClock(AlarmManager.AlarmClockInfo(trigger, launchIntent(alarm.id)), receiverIntent(alarm.id, false)); if (alarm.preAlarmMinutes > 0) { val pre = trigger - alarm.preAlarmMinutes * 60_000L; if (pre > System.currentTimeMillis()) manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, pre, receiverIntent(alarm.id, true)) } }
    fun cancel(alarmId: Long) { manager.cancel(receiverIntent(alarmId, false)); manager.cancel(receiverIntent(alarmId, true)) }
    fun snooze(alarm: AlarmEntity) { manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + alarm.snoozeMinutes * 60_000L, receiverIntent(alarm.id, false, true)) }
    suspend fun rescheduleAfterFire(alarmId: Long) { alarmDao.alarm(alarmId)?.let { if (it.enabled && it.repeatSet().isNotEmpty()) schedule(it) } }
    private fun nextOccurrence(alarm: AlarmEntity): LocalDateTime? { val now = LocalDateTime.now(); val activeDays = alarm.repeatSet(); repeat(8) { offset -> val candidate = now.toLocalDate().plusDays(offset.toLong()).atTime(alarm.hour, alarm.minute); val day = if (candidate.dayOfWeek == DayOfWeek.SUNDAY) 0 else candidate.dayOfWeek.value; if ((activeDays.isEmpty() || day in activeDays) && candidate.isAfter(now)) return candidate }; return null }
    private fun receiverIntent(id: Long, preAlarm: Boolean, snooze: Boolean = false): PendingIntent { val request = (id * 10 + if (preAlarm) 1 else if (snooze) 2 else 0).toInt(); return PendingIntent.getBroadcast(context, request, Intent(context, AlarmReceiver::class.java).putExtra(AlarmReceiver.EXTRA_ALARM_ID, id).putExtra(AlarmReceiver.EXTRA_PRE_ALARM, preAlarm).putExtra(AlarmReceiver.EXTRA_SNOOZE, snooze), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE) }
    private fun launchIntent(id: Long): PendingIntent = PendingIntent.getActivity(context, id.toInt(), Intent(context, com.premiumclock.app.ui.MainActivity::class.java).putExtra("open_alarm", id), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
}

class TimerScheduler(private val context: Context) {
    private val manager = context.getSystemService(AlarmManager::class.java)
    fun schedule(timerId: Long, endAtMillis: Long) = manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endAtMillis, pending(timerId))
    fun cancel(timerId: Long) = manager.cancel(pending(timerId))
    private fun pending(timerId: Long): PendingIntent = PendingIntent.getBroadcast(context, (timerId + 40_000).toInt(), Intent(context, TimerCompletionReceiver::class.java).putExtra(TimerForegroundService.EXTRA_TIMER_ID, timerId), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
}
