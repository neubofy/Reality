package com.neubofy.reality.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "habit_entries",
    primaryKeys = ["habitId", "date"],
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["habitId"]), Index(value = ["date"])]
)
data class HabitEntryEntity(
    val habitId: Long,
    val date: String,             // Format: yyyy-MM-dd
    val value: Int = VALUE_NO,    // BOOLEAN: 0=NO, 2=YES_MANUAL, 1=YES_AUTO, 3=SKIP. MEASURABLE: value * 1000
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
) {
    companion object {
        const val VALUE_UNKNOWN = -1
        const val VALUE_NO = 0
        const val VALUE_YES_AUTO = 1
        const val VALUE_YES_MANUAL = 2
        const val VALUE_SKIP = 3
    }
}
