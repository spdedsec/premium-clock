package com.premiumclock.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao interface AlarmDao { @Query("SELECT * FROM alarms ORDER BY hour, minute") fun observeAlarms(): Flow<List<AlarmEntity>>; @Query("SELECT * FROM alarms WHERE id = :id LIMIT 1") suspend fun alarm(id: Long): AlarmEntity?; @Insert suspend fun insert(alarm: AlarmEntity): Long; @Update suspend fun update(alarm: AlarmEntity); @Delete suspend fun delete(alarm: AlarmEntity); @Query("SELECT * FROM alarms") suspend fun all(): List<AlarmEntity> }
@Dao interface TimerDao { @Query("SELECT * FROM timers ORDER BY updatedAt DESC") fun observeTimers(): Flow<List<TimerEntity>>; @Query("SELECT * FROM timers WHERE id = :id LIMIT 1") suspend fun timer(id: Long): TimerEntity?; @Insert suspend fun insert(timer: TimerEntity): Long; @Update suspend fun update(timer: TimerEntity); @Delete suspend fun delete(timer: TimerEntity); @Query("SELECT * FROM timers") suspend fun all(): List<TimerEntity> }
@Dao interface WorldClockDao { @Query("SELECT * FROM world_clocks ORDER BY position, displayName") fun observeWorldClocks(): Flow<List<WorldClockEntity>>; @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(clock: WorldClockEntity); @Delete suspend fun delete(clock: WorldClockEntity); @Query("SELECT * FROM world_clocks") suspend fun all(): List<WorldClockEntity> }
@Dao interface EventDao { @Query("SELECT * FROM time_events ORDER BY occurredAt DESC LIMIT 100") fun observeEvents(): Flow<List<TimeEventEntity>>; @Insert suspend fun insert(event: TimeEventEntity); @Query("DELETE FROM time_events") suspend fun clear() }
