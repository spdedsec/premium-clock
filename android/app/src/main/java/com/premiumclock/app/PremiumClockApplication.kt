package com.premiumclock.app

import android.app.Application
import com.premiumclock.app.data.ClockDatabase
import com.premiumclock.app.data.PreferencesRepository
import com.premiumclock.app.services.AlarmScheduler
import com.premiumclock.app.services.NotificationHelper
import com.premiumclock.app.services.TimerScheduler

class PremiumClockApplication : Application() {
    val database by lazy { ClockDatabase.create(this) }
    val preferences by lazy { PreferencesRepository(this) }
    val alarmScheduler by lazy { AlarmScheduler(this, database.alarmDao()) }
    val timerScheduler by lazy { TimerScheduler(this) }
    override fun onCreate() { super.onCreate(); NotificationHelper.ensureChannels(this) }
}
