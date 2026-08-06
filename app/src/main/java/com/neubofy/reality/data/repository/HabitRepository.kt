package com.neubofy.reality.data.repository

import android.content.Context
import com.neubofy.reality.data.db.AppDatabase
import com.neubofy.reality.data.db.HabitEntity
import com.neubofy.reality.data.db.HabitEntryEntity
import com.neubofy.reality.utils.HabitEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class HabitRepository(private val context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val habitDao = db.habitDao()
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    data class HabitWithStatus(
        val habit: HabitEntity,
        val entry: HabitEntryEntity?,
        val currentScore: Double,
        val currentStreak: Int,
        val bestStreak: Int,
        val isCompleted: Boolean
    )

    suspend fun getActiveHabits(): List<HabitEntity> = withContext(Dispatchers.IO) {
        habitDao.getAllActiveHabitsList()
    }

    suspend fun getHabitsWithStatusForDate(date: LocalDate): List<HabitWithStatus> = withContext(Dispatchers.IO) {
        // Run passive auto-evaluator on demand
        try {
            HabitEngine.evaluateAutoHabits(context, date)
        } catch (e: Exception) {
            // Fail gracefully if permissions/services are unavailable
        }

        val dateStr = date.format(dateFormatter)
        val habits = habitDao.getAllActiveHabitsList()
        val entriesForDate = habitDao.getEntriesForDate(dateStr).associateBy { it.habitId }

        val startDate = date.minusDays(60)

        habits.map { habit ->
            val entriesList = habitDao.getEntriesForHabitRange(habit.id, startDate.format(dateFormatter), dateStr)
            val entriesMap = entriesList.associateBy { it.date }

            val entry = entriesForDate[habit.id]
            val scores = HabitEngine.computeScoresForRange(habit, entriesMap, startDate, date)
            val scoreToday = scores[dateStr] ?: 0.0

            val streaks = HabitEngine.computeStreaks(habit, entriesMap, date)

            val valInt = entry?.value ?: HabitEntryEntity.VALUE_NO
            val isCompleted = if (habit.type == HabitEntity.TYPE_MEASURABLE) {
                val valReal = valInt / 1000.0
                if (habit.targetType == HabitEntity.TARGET_AT_LEAST) {
                    valReal >= habit.targetValue && habit.targetValue > 0
                } else {
                    valInt != HabitEntryEntity.VALUE_NO && valReal <= habit.targetValue
                }
            } else {
                valInt == HabitEntryEntity.VALUE_YES_MANUAL || valInt == HabitEntryEntity.VALUE_YES_AUTO
            }

            HabitWithStatus(
                habit = habit,
                entry = entry,
                currentScore = scoreToday,
                currentStreak = streaks.currentStreak,
                bestStreak = streaks.bestStreak,
                isCompleted = isCompleted
            )
        }
    }

    suspend fun toggleHabit(habitId: Long, date: LocalDate): HabitEntryEntity = withContext(Dispatchers.IO) {
        val dateStr = date.format(dateFormatter)
        val currentEntry = habitDao.getEntry(habitId, dateStr)
        val currentValue = currentEntry?.value ?: HabitEntryEntity.VALUE_NO

        // Cycle through: NO (0) -> YES_MANUAL (2) -> SKIP (3) -> NO (0)
        val nextValue = when (currentValue) {
            HabitEntryEntity.VALUE_NO -> HabitEntryEntity.VALUE_YES_MANUAL
            HabitEntryEntity.VALUE_YES_MANUAL -> HabitEntryEntity.VALUE_SKIP
            HabitEntryEntity.VALUE_YES_AUTO -> HabitEntryEntity.VALUE_SKIP
            HabitEntryEntity.VALUE_SKIP -> HabitEntryEntity.VALUE_NO
            else -> HabitEntryEntity.VALUE_YES_MANUAL
        }

        val newEntry = HabitEntryEntity(
            habitId = habitId,
            date = dateStr,
            value = nextValue,
            notes = currentEntry?.notes ?: "",
            timestamp = System.currentTimeMillis()
        )
        habitDao.insertOrUpdateEntry(newEntry)
        
        // Recalculate daily stats XP
        try {
            com.neubofy.reality.utils.XPManager.recalculateDailyStats(context, dateStr)
        } catch (e: Exception) {}

        newEntry
    }

    suspend fun setMeasurableValue(habitId: Long, date: LocalDate, value: Double, notes: String = ""): HabitEntryEntity = withContext(Dispatchers.IO) {
        val dateStr = date.format(dateFormatter)
        val intValue = (value * 1000).toInt()

        val entry = HabitEntryEntity(
            habitId = habitId,
            date = dateStr,
            value = intValue,
            notes = notes,
            timestamp = System.currentTimeMillis()
        )
        habitDao.insertOrUpdateEntry(entry)

        try {
            com.neubofy.reality.utils.XPManager.recalculateDailyStats(context, dateStr)
        } catch (e: Exception) {}

        entry
    }

    suspend fun saveHabit(habit: HabitEntity): Long = withContext(Dispatchers.IO) {
        if (habit.id == 0L) {
            habitDao.insertHabit(habit)
        } else {
            habitDao.updateHabit(habit)
            habit.id
        }
    }

    suspend fun archiveHabit(habitId: Long, isArchived: Boolean) = withContext(Dispatchers.IO) {
        habitDao.setArchivedStatus(habitId, isArchived)
    }

    suspend fun deleteHabit(habitId: Long) = withContext(Dispatchers.IO) {
        habitDao.deleteHabit(habitId)
    }

    suspend fun clearHabitEntries(habitId: Long) = withContext(Dispatchers.IO) {
        habitDao.deleteAllEntriesForHabit(habitId)
    }

    suspend fun deleteAllHabitsAndEntries() = withContext(Dispatchers.IO) {
        val habits = habitDao.getAllHabitsIncludingArchived()
        habits.forEach { habitDao.deleteHabit(it.id) }
    }
}
