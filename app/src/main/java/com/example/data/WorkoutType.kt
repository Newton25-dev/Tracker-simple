package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workout_types")
data class WorkoutType(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String
)
