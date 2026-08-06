package com.neubofy.reality.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {

    @Query("SELECT * FROM habits WHERE isArchived = 0 ORDER BY position ASC, id ASC")
    fun getAllActiveHabits(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE isArchived = 0 ORDER BY position ASC, id ASC")
    suspend fun getAllActiveHabitsList(): List<HabitEntity>

    @Query("SELECT * FROM habits ORDER BY position ASC, id ASC")
    suspend fun getAllHabitsIncludingArchived(): List<HabitEntity>

    @Query("SELECT * FROM habits WHERE id = :id LIMIT 1")
    suspend fun getHabitById(id: Long): HabitEntity?

    @Query("SELECT * FROM habits WHERE uuid = :uuid LIMIT 1")
    suspend fun getHabitByUuid(uuid: String): HabitEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: HabitEntity): Long

    @Update
    suspend fun updateHabit(habit: HabitEntity)

    @Query("UPDATE habits SET isArchived = :isArchived WHERE id = :id")
    suspend fun setArchivedStatus(id: Long, isArchived: Boolean)

    @Query("DELETE FROM habits WHERE id = :id")
    suspend fun deleteHabit(id: Long)

    // Entry Queries
    @Query("SELECT * FROM habit_entries WHERE habitId = :habitId AND date = :date LIMIT 1")
    suspend fun getEntry(habitId: Long, date: String): HabitEntryEntity?

    @Query("SELECT * FROM habit_entries WHERE habitId = :habitId ORDER BY date DESC")
    suspend fun getEntriesForHabit(habitId: Long): List<HabitEntryEntity>

    @Query("SELECT * FROM habit_entries WHERE date = :date")
    suspend fun getEntriesForDate(date: String): List<HabitEntryEntity>

    @Query("SELECT * FROM habit_entries WHERE date >= :startDate AND date <= :endDate")
    suspend fun getEntriesForDateRange(startDate: String, endDate: String): List<HabitEntryEntity>

    @Query("SELECT * FROM habit_entries WHERE habitId = :habitId AND date >= :startDate AND date <= :endDate ORDER BY date ASC")
    suspend fun getEntriesForHabitRange(habitId: Long, startDate: String, endDate: String): List<HabitEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateEntry(entry: HabitEntryEntity)

    @Query("DELETE FROM habit_entries WHERE habitId = :habitId AND date = :date")
    suspend fun deleteEntry(habitId: Long, date: String)

    @Query("DELETE FROM habit_entries WHERE habitId = :habitId")
    suspend fun deleteAllEntriesForHabit(habitId: Long)
}
