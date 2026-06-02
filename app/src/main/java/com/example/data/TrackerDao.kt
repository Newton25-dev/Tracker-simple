package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackerDao {
    @Query("SELECT * FROM day_records ORDER BY dayIndex ASC")
    fun getAllDayRecordsFlow(): Flow<List<DayRecord>>

    @Query("SELECT * FROM day_records ORDER BY dayIndex ASC")
    suspend fun getAllDayRecords(): List<DayRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDayRecords(records: List<DayRecord>)

    @Update
    suspend fun updateDayRecord(record: DayRecord)

    @Query("SELECT * FROM workout_types ORDER BY id ASC")
    fun getAllWorkoutTypesFlow(): Flow<List<WorkoutType>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkoutType(type: WorkoutType)

    @Query("DELETE FROM workout_types WHERE id = :id")
    suspend fun deleteWorkoutTypeById(id: Int)

    @Query("SELECT * FROM cycle_metadata WHERE id = 1 LIMIT 1")
    fun getCycleMetadataFlow(): Flow<CycleMetadata?>

    @Query("SELECT * FROM cycle_metadata WHERE id = 1 LIMIT 1")
    suspend fun getCycleMetadata(): CycleMetadata?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCycleMetadata(metadata: CycleMetadata)
}
