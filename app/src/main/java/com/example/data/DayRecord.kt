package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "day_records")
data class DayRecord(
    @PrimaryKey val dayIndex: Int, // 0 to 27
    val isWorkoutDone: Boolean = false,
    val assignedWorkoutType: String? = null,
    val completedWorkoutType: String? = null,
    val isRestDay: Boolean = false,
    val isExtraRestDay: Boolean = false, // Sleep-based extra rest day
    val sleepHours: Float? = null // Sleep hours logged for the night before
)
