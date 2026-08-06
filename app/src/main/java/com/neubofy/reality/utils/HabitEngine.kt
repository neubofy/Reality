package com.neubofy.reality.utils

import android.content.Context
import com.neubofy.reality.data.db.AppDatabase
import com.neubofy.reality.data.db.HabitEntity
import com.neubofy.reality.data.db.HabitEntryEntity
import com.neubofy.reality.health.HealthManager
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

object HabitEngine {

    private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    data class StreakResult(
        val currentStreak: Int = 0,
        val bestStreak: Int = 0
    )

    /**
     * Reality's Habit Score Calculation (Exponential Smoothing Formula).
     *
     * multiplier = 0.5 ^ (sqrt(frequency) / 13.0)
     * score = previousScore * multiplier + checkmarkValue * (1 - multiplier)
     */
    fun computeScore(
        frequency: Double,
        previousScore: Double,
        checkmarkValue: Double
    ): Double {
        val freq = if (frequency <= 0.0) 1.0 else frequency
        val multiplier = 0.5.pow(sqrt(freq) / 13.0)
        var score = previousScore * multiplier + checkmarkValue * (1.0 - multiplier)
        return score.coerceIn(0.0, 1.0)
    }

    /**
     * Compute score series for a habit across a date range.
     */
    fun computeScoresForRange(
        habit: HabitEntity,
        entriesMap: Map<String, HabitEntryEntity>, // key: yyyy-MM-dd
        fromDate: LocalDate,
        toDate: LocalDate
    ): Map<String, Double> {
        val scores = mutableMapOf<String, Double>()
        if (fromDate.isAfter(toDate)) return scores

        val freqDouble = habit.freqNumerator.toDouble() / habit.freqDenominator.coerceAtLeast(1)
        var previousScore = 0.0

        var current = fromDate
        while (!current.isAfter(toDate)) {
            val dateStr = current.format(DATE_FORMATTER)
            val entry = entriesMap[dateStr]
            val value = entry?.value ?: HabitEntryEntity.VALUE_UNKNOWN

            val checkmark = calculateCheckmarkValue(habit, value)
            if (value != HabitEntryEntity.VALUE_SKIP && value != HabitEntryEntity.VALUE_UNKNOWN) {
                previousScore = computeScore(freqDouble, previousScore, checkmark)
            }
            scores[dateStr] = previousScore
            current = current.plusDays(1)
        }
        return scores
    }

    private fun calculateCheckmarkValue(habit: HabitEntity, value: Int): Double {
        if (value == HabitEntryEntity.VALUE_SKIP || value == HabitEntryEntity.VALUE_UNKNOWN) return 0.0

        if (habit.type == HabitEntity.TYPE_MEASURABLE) {
            val actualValue = value / 1000.0
            val target = habit.targetValue
            if (target <= 0) return 1.0

            return if (habit.targetType == HabitEntity.TARGET_AT_LEAST) {
                min(1.0, actualValue / target)
            } else {
                if (actualValue <= target) 1.0 else (1.0 - ((actualValue - target) / target)).coerceIn(0.0, 1.0)
            }
        } else {
            return if (value == HabitEntryEntity.VALUE_YES_MANUAL || value == HabitEntryEntity.VALUE_YES_AUTO) 1.0 else 0.0
        }
    }

    /**
     * Calculates Current and Best Streaks for a habit.
     */
    fun computeStreaks(
        habit: HabitEntity,
        entriesMap: Map<String, HabitEntryEntity>,
        today: LocalDate
    ): StreakResult {
        var currentStreak = 0
        var bestStreak = 0
        var tempStreak = 0

        // Find earliest entry date or default to 365 days ago
        val earliestDate = entriesMap.keys
            .mapNotNull { try { LocalDate.parse(it, DATE_FORMATTER) } catch (e: Exception) { null } }
            .minOrNull() ?: today.minusDays(90)

        var date = earliestDate
        while (!date.isAfter(today)) {
            val dateStr = date.format(DATE_FORMATTER)
            val entry = entriesMap[dateStr]
            val valInt = entry?.value ?: HabitEntryEntity.VALUE_NO

            val isSuccess = if (habit.type == HabitEntity.TYPE_MEASURABLE) {
                val valReal = valInt / 1000.0
                if (habit.targetType == HabitEntity.TARGET_AT_LEAST) {
                    valReal >= habit.targetValue && habit.targetValue > 0
                } else {
                    valInt != HabitEntryEntity.VALUE_NO && valReal <= habit.targetValue
                }
            } else {
                valInt == HabitEntryEntity.VALUE_YES_MANUAL || valInt == HabitEntryEntity.VALUE_YES_AUTO
            }

            if (isSuccess) {
                tempStreak++
                if (tempStreak > bestStreak) bestStreak = tempStreak
            } else if (valInt == HabitEntryEntity.VALUE_SKIP) {
                // SKIP does not break streak
            } else {
                tempStreak = 0
            }

            date = date.plusDays(1)
        }

        // Current streak logic (count backwards from today)
        var checkDate = today
        // If today is not checked yet, look at yesterday
        val todayEntry = entriesMap[today.format(DATE_FORMATTER)]
        val todaySuccess = todayEntry != null && (todayEntry.value == HabitEntryEntity.VALUE_YES_MANUAL || todayEntry.value == HabitEntryEntity.VALUE_YES_AUTO)
        if (!todaySuccess) {
            checkDate = today.minusDays(1)
        }

        while (!checkDate.isBefore(earliestDate)) {
            val dateStr = checkDate.format(DATE_FORMATTER)
            val entry = entriesMap[dateStr]
            val valInt = entry?.value ?: HabitEntryEntity.VALUE_NO

            val isSuccess = if (habit.type == HabitEntity.TYPE_MEASURABLE) {
                val valReal = valInt / 1000.0
                if (habit.targetType == HabitEntity.TARGET_AT_LEAST) {
                    valReal >= habit.targetValue && habit.targetValue > 0
                } else {
                    valInt != HabitEntryEntity.VALUE_NO && valReal <= habit.targetValue
                }
            } else {
                valInt == HabitEntryEntity.VALUE_YES_MANUAL || valInt == HabitEntryEntity.VALUE_YES_AUTO
            }

            if (isSuccess) {
                currentStreak++
            } else if (entry?.value == HabitEntryEntity.VALUE_SKIP) {
                // Skip doesn't break
            } else {
                break
            }
            checkDate = checkDate.minusDays(1)
        }

        return StreakResult(currentStreak = currentStreak, bestStreak = max(bestStreak, currentStreak))
    }

    /**
     * Passive Auto-Evaluator: Evaluates habits linked to Reality's existing health/usage/focus sources.
     * ZERO background process, ZERO extra permissions requested!
     */
    suspend fun evaluateAutoHabits(context: Context, date: LocalDate) {
        val db = AppDatabase.getDatabase(context)
        val habitDao = db.habitDao()
        val habits = habitDao.getAllActiveHabitsList()

        val autoHabits = habits.filter { it.autoSourceType != HabitEntity.SOURCE_NONE }
        if (autoHabits.isEmpty()) return

        val dateStr = date.format(DATE_FORMATTER)
        val healthManager = HealthManager(context)

        for (habit in autoHabits) {
            val existingEntry = habitDao.getEntry(habit.id, dateStr)
            // If user manually edited, don't overwrite manual check-in unless it was automatic
            if (existingEntry != null && existingEntry.value == HabitEntryEntity.VALUE_YES_MANUAL) {
                continue
            }

            val target = if (habit.autoSourceTarget > 0) habit.autoSourceTarget else habit.targetValue
            var isFulfilled = false
            var measuredValDouble = 0.0

            when (habit.autoSourceType) {
                HabitEntity.SOURCE_HEALTH_STEPS -> {
                    val steps = healthManager.getSteps(date)
                    measuredValDouble = steps.toDouble()
                    isFulfilled = steps >= target
                }
                HabitEntity.SOURCE_HEALTH_SLEEP -> {
                    val sessions = healthManager.getSleepSessions(date)
                    val totalSleepMins = sessions.sumOf { (start, end) ->
                        java.time.Duration.between(start, end).toMinutes()
                    }
                    measuredValDouble = totalSleepMins.toDouble()
                    isFulfilled = totalSleepMins >= target
                }
                HabitEntity.SOURCE_USAGE_SCREEN_TIME -> {
                    val metrics = UsageUtils.getProUsageMetrics(context, date)
                    val screenMins = metrics.screenTimeMs / 60000.0
                    measuredValDouble = screenMins
                    isFulfilled = if (habit.targetType == HabitEntity.TARGET_AT_MOST) {
                        screenMins <= target
                    } else {
                        screenMins >= target
                    }
                }
                HabitEntity.SOURCE_TAPASYA_FOCUS -> {
                    val startOfDay = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    val endOfDay = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    val sessions = db.tapasyaSessionDao().getSessionsInWindow(startOfDay, endOfDay)
                    val focusMins = sessions.sumOf { it.effectiveTimeMs } / 60000.0
                    measuredValDouble = focusMins
                    isFulfilled = focusMins >= target
                }
                HabitEntity.SOURCE_TASK_COMPLETION -> {
                    val dailyStats = db.dailyStatsDao().getDailyStats(dateStr)
                    val tasksDone = (dailyStats?.tasksCompleted ?: 0).toDouble()
                    measuredValDouble = tasksDone
                    isFulfilled = tasksDone >= target
                }
            }

            val newValue = if (habit.type == HabitEntity.TYPE_MEASURABLE) {
                (measuredValDouble * 1000).toInt()
            } else {
                if (isFulfilled) HabitEntryEntity.VALUE_YES_AUTO else HabitEntryEntity.VALUE_NO
            }

            if (existingEntry == null || existingEntry.value != newValue) {
                habitDao.insertOrUpdateEntry(
                    HabitEntryEntity(
                        habitId = habit.id,
                        date = dateStr,
                        value = newValue,
                        notes = "Auto-sourced via ${habit.autoSourceType}",
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
        }
    }
}
