package com.neubofy.reality.utils.tools

import android.content.Context
import com.neubofy.reality.data.db.HabitEntity
import com.neubofy.reality.data.repository.HabitRepository
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

class HabitTool : AgentTool {
    override val id = "habit"
    override val name = "Habit Tracker"
    override val shortDesc = "Manage habits, check-in, inspect scores, streaks, and statistics"
    override val category = ToolCategory.DATA

    override fun getSchema(): JSONObject {
        return createSchema(
            "habit",
            "Read and mutate habit tracker data. Actions: 'list' (all active habits), 'stats' (score & streak details), 'log' (check-in or set measurable value), 'create' (create new habit), 'archive' (archive habit).",
            mapOf(
                "action" to "Required: 'list', 'stats', 'log', 'create', 'archive'",
                "habit_id" to "Required for stats, log, archive: Habit ID (long)",
                "date" to "Optional: YYYY-MM-DD (default today)",
                "value" to "Required for 'log' measurable habits: numeric value (e.g. 8.0). Optional for boolean (defaults to toggle)",
                "name" to "Required for 'create': Habit name",
                "question" to "Optional for 'create': Reflection prompt",
                "type" to "Optional for 'create': 'boolean' or 'measurable' (default boolean)",
                "target_value" to "Optional for 'create' measurable habits: numeric target",
                "unit" to "Optional for 'create' measurable habits: e.g. 'steps', 'mins', 'pages'"
            )
        )
    }

    override suspend fun execute(context: Context, args: JSONObject): String {
        val repo = HabitRepository(context)
        val action = args.optString("action", "list")
        val dateStr = args.optString("date", "")
        val date = if (dateStr.isNotEmpty()) LocalDate.parse(dateStr) else LocalDate.now()

        return when (action) {
            "list" -> {
                val habits = repo.getHabitsWithStatusForDate(date)
                val jsonArr = JSONArray()
                habits.forEach { item ->
                    jsonArr.put(JSONObject().apply {
                        put("id", item.habit.id)
                        put("name", item.habit.name)
                        put("question", item.habit.question)
                        put("type", if (item.habit.type == HabitEntity.TYPE_MEASURABLE) "measurable" else "boolean")
                        put("target_value", item.habit.targetValue)
                        put("unit", item.habit.unit)
                        put("is_completed_today", item.isCompleted)
                        put("current_score", String.format("%.2f", item.currentScore * 100) + "%")
                        put("current_streak", item.currentStreak)
                        put("best_streak", item.bestStreak)
                    })
                }
                JSONObject().apply {
                    put("date", date.toString())
                    put("total_habits", habits.size)
                    put("completed_habits", habits.count { it.isCompleted })
                    put("habits", jsonArr)
                }.toString()
            }
            "stats" -> {
                val habitId = args.optLong("habit_id", -1L)
                if (habitId == -1L) return JSONObject().put("error", "habit_id is required").toString()

                val habits = repo.getHabitsWithStatusForDate(date)
                val target = habits.find { it.habit.id == habitId }
                    ?: return JSONObject().put("error", "Habit not found with id $habitId").toString()

                JSONObject().apply {
                    put("id", target.habit.id)
                    put("name", target.habit.name)
                    put("description", target.habit.description)
                    put("current_score", String.format("%.2f", target.currentScore * 100) + "%")
                    put("current_streak", target.currentStreak)
                    put("best_streak", target.bestStreak)
                    put("is_completed", target.isCompleted)
                    put("auto_source", target.habit.autoSourceType)
                }.toString()
            }
            "log" -> {
                val habitId = args.optLong("habit_id", -1L)
                if (habitId == -1L) return JSONObject().put("error", "habit_id is required").toString()

                val habits = repo.getActiveHabits()
                val target = habits.find { it.id == habitId }
                    ?: return JSONObject().put("error", "Habit not found").toString()

                if (target.type == HabitEntity.TYPE_MEASURABLE) {
                    val valDouble = args.optDouble("value", -1.0)
                    if (valDouble < 0) return JSONObject().put("error", "value (numeric) is required for measurable habit").toString()
                    repo.setMeasurableValue(habitId, date, valDouble)
                    JSONObject().apply {
                        put("success", true)
                        put("habit_id", habitId)
                        put("logged_value", valDouble)
                        put("date", date.toString())
                    }.toString()
                } else {
                    val entry = repo.toggleHabit(habitId, date)
                    JSONObject().apply {
                        put("success", true)
                        put("habit_id", habitId)
                        put("new_state_value", entry.value)
                        put("date", date.toString())
                    }.toString()
                }
            }
            "create" -> {
                val name = args.optString("name", "")
                if (name.isEmpty()) return JSONObject().put("error", "name is required").toString()

                val question = args.optString("question", "Did you complete $name today?")
                val typeStr = args.optString("type", "boolean")
                val isMeasurable = typeStr.lowercase() == "measurable"
                val targetVal = args.optDouble("target_value", 0.0)
                val unit = args.optString("unit", "")

                val habit = HabitEntity(
                    name = name,
                    question = question,
                    type = if (isMeasurable) HabitEntity.TYPE_MEASURABLE else HabitEntity.TYPE_BOOLEAN,
                    targetValue = targetVal,
                    unit = unit
                )
                val newId = repo.saveHabit(habit)
                JSONObject().apply {
                    put("success", true)
                    put("created_habit_id", newId)
                    put("name", name)
                }.toString()
            }
            "archive" -> {
                val habitId = args.optLong("habit_id", -1L)
                if (habitId == -1L) return JSONObject().put("error", "habit_id is required").toString()
                repo.archiveHabit(habitId, true)
                JSONObject().apply {
                    put("success", true)
                    put("archived_habit_id", habitId)
                }.toString()
            }
            else -> JSONObject().put("error", "Unknown action $action").toString()
        }
    }
}
