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
        private val INSTANCES = java.util.concurrent.ConcurrentHashMap<String, AppDatabase>()

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return getDatabase(context, scope, "Default")
        }

        fun getDatabase(context: Context, scope: CoroutineScope, profileName: String): AppDatabase {
            val dbName = if (profileName == "Default" || profileName.isBlank()) {
                "resistance_tracker_database"
            } else {
                "resistance_tracker_database_${profileName.trim().lowercase().replace(Regex("[^a-z0-9_]"), "_")}"
            }
            return INSTANCES.getOrPut(dbName) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    dbName
                )
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Prepopulate on background coroutine
                        scope.launch(Dispatchers.IO) {
                            val database = getDatabase(context, scope, profileName)
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
            }
        }
    }
}
