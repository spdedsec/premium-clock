package com.premiumclock.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [AlarmEntity::class, TimerEntity::class, WorldClockEntity::class, TimeEventEntity::class], version = 1, exportSchema = true)
abstract class ClockDatabase : RoomDatabase() {
    abstract fun alarmDao(): AlarmDao
    abstract fun timerDao(): TimerDao
    abstract fun worldClockDao(): WorldClockDao
    abstract fun eventDao(): EventDao
    companion object { fun create(context: Context): ClockDatabase = Room.databaseBuilder(context.applicationContext, ClockDatabase::class.java, "premium-clock.db").fallbackToDestructiveMigration().build() }
}
