package com.neubofy.reality.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uuid: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    val question: String = "",           // Prompt for reflection/check-in e.g., "Did you read today?"
    val type: Int = TYPE_BOOLEAN,        // 0 = BOOLEAN, 1 = MEASURABLE
    val targetValue: Double = 0.0,       // For measurable habits (e.g. 8.0)
    val targetType: Int = TARGET_AT_LEAST, // 0 = AT_LEAST, 1 = AT_MOST
    val unit: String = "",               // e.g., "mins", "steps", "pages"
    val freqNumerator: Int = 1,          // X times...
    val freqDenominator: Int = 1,        // ...per Y days
    val color: Int = 0,                  // Color palette index
    val position: Int = 0,               // Sort order
    val isArchived: Boolean = false,
    
    // Passive Auto-Sourcing (Reads from Reality's existing DB/Preferences)
    val autoSourceType: String = SOURCE_NONE, // NONE, HEALTH_STEPS, HEALTH_SLEEP, USAGE_SCREEN_TIME, TAPASYA_FOCUS, TASK_COMPLETION
    val autoSourceTarget: Double = 0.0,
    
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val TYPE_BOOLEAN = 0
        const val TYPE_MEASURABLE = 1

        const val TARGET_AT_LEAST = 0
        const val TARGET_AT_MOST = 1

        const val SOURCE_NONE = "NONE"
        const val SOURCE_HEALTH_STEPS = "HEALTH_STEPS"
        const val SOURCE_HEALTH_SLEEP = "HEALTH_SLEEP"
        const val SOURCE_USAGE_SCREEN_TIME = "USAGE_SCREEN_TIME"
        const val SOURCE_TAPASYA_FOCUS = "TAPASYA_FOCUS"
        const val SOURCE_TASK_COMPLETION = "TASK_COMPLETION"
    }
}
