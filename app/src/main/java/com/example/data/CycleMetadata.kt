package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cycle_metadata")
data class CycleMetadata(
    @PrimaryKey val id: Int = 1,
    val startDateMillis: Long
)
