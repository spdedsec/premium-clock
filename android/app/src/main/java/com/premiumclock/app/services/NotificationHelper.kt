package com.premiumclock.app.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.premiumclock.app.R
import com.premiumclock.app.data.AlarmEntity
import com.premiumclock.app.ui.AlarmRingActivity

object NotificationHelper {
    const val ALARM_CHANNEL = "alarms"; const val TIMER_CHANNEL = "timers"; const val ALARM_NOTIFICATION_BASE = 20_000; const val TIMER_NOTIFICATION_BASE = 30_000
    fun ensureChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        val alarm = NotificationChannel(ALARM_CHANNEL, "Alarms", NotificationManager.IMPORTANCE_HIGH).apply { description = "Wake-up alarms and pre-alarm reminders"; enableVibration(true); setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM), null); lockscreenVisibility = Notification.VISIBILITY_PUBLIC }
        val timer = NotificationChannel(TIMER_CHANNEL, "Timers", NotificationManager.IMPORTANCE_DEFAULT).apply { description = "Running and completed timers" }
        manager.createNotificationChannels(listOf(alarm, timer))
    }
    fun alarmNotification(context: Context, alarm: AlarmEntity): Notification {
        val open = PendingIntent.getActivity(context, alarm.id.toInt(), Intent(context, AlarmRingActivity::class.java).putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarm.id), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val snooze = PendingIntent.getBroadcast(context, alarm.id.toInt() + 10_000, Intent(context, AlarmActionReceiver::class.java).setAction(AlarmActionReceiver.ACTION_SNOOZE).putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarm.id), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val dismiss = PendingIntent.getBroadcast(context, alarm.id.toInt() + 20_000, Intent(context, AlarmActionReceiver::class.java).setAction(AlarmActionReceiver.ACTION_DISMISS).putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarm.id), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(context, ALARM_CHANNEL).setSmallIcon(R.drawable.ic_clock_notification).setContentTitle(alarm.label.ifBlank { "Wake up" }).setContentText("Alarm at ${String.format("%02d:%02d", alarm.hour, alarm.minute)}").setCategory(NotificationCompat.CATEGORY_ALARM).setPriority(NotificationCompat.PRIORITY_MAX).setOngoing(true).setAutoCancel(false).setFullScreenIntent(open, true).setContentIntent(open).addAction(R.drawable.ic_snooze, "Snooze", snooze).addAction(R.drawable.ic_dismiss, "Dismiss", dismiss).build()
    }
    fun preAlarmNotification(context: Context, alarm: AlarmEntity) { NotificationManagerCompat.from(context).notify(ALARM_NOTIFICATION_BASE + alarm.id.toInt() + 5_000, NotificationCompat.Builder(context, ALARM_CHANNEL).setSmallIcon(R.drawable.ic_clock_notification).setContentTitle("Alarm in ${alarm.preAlarmMinutes} minutes").setContentText(alarm.label.ifBlank { "Your next alarm is approaching." }).setAutoCancel(true).build()) }
    fun timerNotification(context: Context, timerId: Long, name: String, remainingMillis: Long): Notification {
        val pause = PendingIntent.getService(context, timerId.toInt() + 31_000, Intent(context, TimerForegroundService::class.java).setAction(TimerForegroundService.ACTION_PAUSE).putExtra(TimerForegroundService.EXTRA_TIMER_ID, timerId), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val stop = PendingIntent.getService(context, timerId.toInt() + 32_000, Intent(context, TimerForegroundService::class.java).setAction(TimerForegroundService.ACTION_STOP).putExtra(TimerForegroundService.EXTRA_TIMER_ID, timerId), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(context, TIMER_CHANNEL).setSmallIcon(R.drawable.ic_clock_notification).setContentTitle(name).setContentText("${formatRemaining(remainingMillis)} remaining").setOnlyAlertOnce(true).setOngoing(true).addAction(R.drawable.ic_pause, "Pause", pause).addAction(R.drawable.ic_stop, "Stop", stop).build()
    }
    fun timerCompleted(context: Context, name: String) { NotificationManagerCompat.from(context).notify(TIMER_NOTIFICATION_BASE + (System.currentTimeMillis() % 10_000).toInt(), NotificationCompat.Builder(context, TIMER_CHANNEL).setSmallIcon(R.drawable.ic_clock_notification).setContentTitle("Timer complete").setContentText(name).setPriority(NotificationCompat.PRIORITY_HIGH).setAutoCancel(true).build()) }
    fun cancelAlarm(context: Context, alarmId: Long) = NotificationManagerCompat.from(context).cancel(ALARM_NOTIFICATION_BASE + alarmId.toInt())
    fun formatRemaining(ms: Long): String { val seconds = ms.coerceAtLeast(0L) / 1_000; return "%02d:%02d".format(seconds / 60, seconds % 60) }
}
