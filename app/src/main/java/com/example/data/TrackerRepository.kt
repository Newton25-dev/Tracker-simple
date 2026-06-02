package com.example.data

import kotlinx.coroutines.flow.Flow

class TrackerRepository(private val trackerDao: TrackerDao) {
    val allDayRecords: Flow<List<DayRecord>> = trackerDao.getAllDayRecordsFlow()
    val allWorkoutTypes: Flow<List<WorkoutType>> = trackerDao.getAllWorkoutTypesFlow()
    val cycleMetadata: Flow<CycleMetadata?> = trackerDao.getCycleMetadataFlow()

    suspend fun updateDayRecord(record: DayRecord) {
        trackerDao.updateDayRecord(record)
    }

    suspend fun insertDayRecords(records: List<DayRecord>) {
        trackerDao.insertDayRecords(records)
    }

    suspend fun addWorkoutType(name: String) {
        trackerDao.insertWorkoutType(WorkoutType(name = name))
    }

    suspend fun updateWorkoutType(type: WorkoutType) {
        trackerDao.insertWorkoutType(type)
    }

    suspend fun deleteWorkoutType(id: Int) {
        trackerDao.deleteWorkoutTypeById(id)
    }

    suspend fun updateCycleMetadata(metadata: CycleMetadata) {
        trackerDao.insertCycleMetadata(metadata)
    }

    suspend fun getDayRecordsDirect(): List<DayRecord> {
        return trackerDao.getAllDayRecords()
    }

    suspend fun ensureDatabaseInitialized() {
        val days = trackerDao.getAllDayRecords()
        if (days.isEmpty()) {
            val initialDays = (0..27).map { DayRecord(dayIndex = it) }
            trackerDao.insertDayRecords(initialDays)
        }
        val meta = trackerDao.getCycleMetadata()
        if (meta == null) {
            trackerDao.insertCycleMetadata(CycleMetadata(startDateMillis = System.currentTimeMillis()))
        }
    }

    suspend fun resetCycle() {
        val blankDays = (0..27).map { DayRecord(dayIndex = it) }
        trackerDao.insertDayRecords(blankDays)
        trackerDao.insertCycleMetadata(CycleMetadata(startDateMillis = System.currentTimeMillis()))
    }
}
