package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [DayRecord::class, WorkoutType::class, CycleMetadata::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun trackerDao(): TrackerDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "resistance_tracker_database"
                )
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Prepopulate on background coroutine
                        scope.launch(Dispatchers.IO) {
                            val database = getDatabase(context, scope)
                            val dao = database.trackerDao()
                            
                            // Initialize 28 blank days
                            val initialDays = (0..27).map { DayRecord(dayIndex = it) }
                            dao.insertDayRecords(initialDays)
                            
                            // Insert default workout types
                            dao.insertWorkoutType(WorkoutType(name = "Upper"))
                            dao.insertWorkoutType(WorkoutType(name = "Lower"))
                            dao.insertWorkoutType(WorkoutType(name = "Full Body"))
                            
                            // Start cycle today
                            dao.insertCycleMetadata(CycleMetadata(startDateMillis = System.currentTimeMillis()))
                        }
                    }
                })
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
