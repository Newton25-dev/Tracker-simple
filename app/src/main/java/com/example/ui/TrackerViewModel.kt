package com.example.ui

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.AlarmReceiver
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

@kotlinx.coroutines.ExperimentalCoroutinesApi
class TrackerViewModel(application: Application) : AndroidViewModel(application) {
    private val sharedPrefs = application.getSharedPreferences("resistance_log_prefs", Context.MODE_PRIVATE)

    private val _activeProfile = MutableStateFlow(sharedPrefs.getString("active_profile", "Default") ?: "Default")
    val activeProfile: StateFlow<String> = _activeProfile.asStateFlow()

    private val _profiles = MutableStateFlow(
        sharedPrefs.getStringSet("profiles_list", setOf("Default"))?.toList() ?: listOf("Default")
    )
    val profiles: StateFlow<List<String>> = _profiles.asStateFlow()

    private val currentRepository: StateFlow<TrackerRepository> = _activeProfile.map { profile ->
        val database = AppDatabase.getDatabase(application, viewModelScope, profile)
        val rep = TrackerRepository(database.trackerDao())
        viewModelScope.launch {
            rep.ensureDatabaseInitialized()
        }
        rep
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        TrackerRepository(AppDatabase.getDatabase(application, viewModelScope, _activeProfile.value).trackerDao())
    )

    val allDayRecords: StateFlow<List<DayRecord>> = currentRepository.flatMapLatest { rep ->
        rep.allDayRecords
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allWorkoutTypes: StateFlow<List<WorkoutType>> = currentRepository.flatMapLatest { rep ->
        rep.allWorkoutTypes
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cycleMetadata: StateFlow<CycleMetadata?> = currentRepository.flatMapLatest { rep ->
        rep.cycleMetadata
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _isDarkMode = MutableStateFlow(sharedPrefs.getBoolean("dark_mode_enabled", false))
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _reminderEnabled = MutableStateFlow(sharedPrefs.getBoolean("reminder_enabled", false))
    val reminderEnabled: StateFlow<Boolean> = _reminderEnabled.asStateFlow()

    private val _reminderHour = MutableStateFlow(sharedPrefs.getInt("reminder_hour", 20))
    val reminderHour: StateFlow<Int> = _reminderHour.asStateFlow()

    private val _reminderMinute = MutableStateFlow(sharedPrefs.getInt("reminder_minute", 0))
    val reminderMinute: StateFlow<Int> = _reminderMinute.asStateFlow()

    fun addNewProfile(profileName: String) {
        val trimmed = profileName.trim()
        if (trimmed.isNotBlank() && !_profiles.value.contains(trimmed)) {
            val newList = _profiles.value + trimmed
            _profiles.value = newList
            sharedPrefs.edit().putStringSet("profiles_list", newList.toSet()).apply()
            switchProfile(trimmed)
        }
    }

    fun switchProfile(profileName: String) {
        if (_profiles.value.contains(profileName)) {
            _activeProfile.value = profileName
            sharedPrefs.edit().putString("active_profile", profileName).apply()
        }
    }

    // Save/Update Day Record & Recalculate Rest Status for the entire cycle
    fun updateDayRecord(dayRecord: DayRecord) {
        viewModelScope.launch {
            val rep = currentRepository.value
            val currentRecords = rep.getDayRecordsDirect()
            val updatedList = currentRecords.map {
                if (it.dayIndex == dayRecord.dayIndex) dayRecord else it
            }
            val recalculatedList = recalculateCycleRestDays(updatedList)
            rep.insertDayRecords(recalculatedList)
        }
    }

    fun toggleDarkMode() {
        val nextMode = !_isDarkMode.value
        _isDarkMode.value = nextMode
        sharedPrefs.edit().putBoolean("dark_mode_enabled", nextMode).apply()
    }

    fun addWorkoutType(name: String) {
        viewModelScope.launch {
            if (name.isNotBlank()) {
                currentRepository.value.addWorkoutType(name.trim())
            }
        }
    }

    fun updateWorkoutType(workoutType: WorkoutType) {
        viewModelScope.launch {
            if (workoutType.name.isNotBlank()) {
                currentRepository.value.updateWorkoutType(workoutType)
            }
        }
    }

    fun deleteWorkoutType(id: Int) {
        viewModelScope.launch {
            currentRepository.value.deleteWorkoutType(id)
        }
    }

    fun resetCycle() {
        viewModelScope.launch {
            currentRepository.value.resetCycle()
        }
    }

    fun updateReminderSettings(enabled: Boolean, hour: Int, minute: Int) {
        val context = getApplication<Application>()
        sharedPrefs.edit()
            .putBoolean("reminder_enabled", enabled)
            .putInt("reminder_hour", hour)
            .putInt("reminder_minute", minute)
            .apply()

        _reminderEnabled.value = enabled
        _reminderHour.value = hour
        _reminderMinute.value = minute

        if (enabled) {
            setupAlarmReal(context, hour, minute)
        } else {
            cancelAlarmReal(context)
        }
    }

    private fun setupAlarmReal(context: Context, hour: Int, minute: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }

    private fun cancelAlarmReal(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    // Pure propagation engine to calculate rest days across the 28-day cycle based on rules:
    // 1. Every workout day enforces a rest day on the following day.
    // 2. If sleep was < 6 hour before a workout, the next TWO days are rest days (second one is extra sleep-rest 😴).
    private fun recalculateCycleRestDays(allDays: List<DayRecord>): List<DayRecord> {
        val updatedDays = allDays.map { it.copy(isRestDay = false, isExtraRestDay = false) }.toMutableList()
        
        for (i in 0 until 28) {
            if (updatedDays[i].isWorkoutDone) {
                // Following day is automatically a rest day
                val nextDayIndex = i + 1
                if (nextDayIndex < 28) {
                    updatedDays[nextDayIndex] = updatedDays[nextDayIndex].copy(isRestDay = true)
                    
                    // If the user slept < 6 hours before this workout, add another rest day on Day + 2
                    val sleep = updatedDays[i].sleepHours
                    if (sleep != null && sleep < 6.0f) {
                        val extraRestIndex = i + 2
                        if (extraRestIndex < 28) {
                            updatedDays[extraRestIndex] = updatedDays[extraRestIndex].copy(
                                isRestDay = true,
                                isExtraRestDay = true
                            )
                        }
                    }
                }
            }
        }
        return updatedDays
    }

    // Safe helper to dynamically map DayIndex into specific Calendar dates and weekday abbreviations
    fun getWeekdayForDayIndex(dayIndex: Int, startDateMillis: Long?): String {
        if (startDateMillis == null) return "Day ${dayIndex + 1}"
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = startDateMillis
        calendar.add(Calendar.DAY_OF_YEAR, dayIndex)
        return when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SUNDAY -> "Sun"
            Calendar.MONDAY -> "Mon"
            Calendar.TUESDAY -> "Tue"
            Calendar.WEDNESDAY -> "Wed"
            Calendar.THURSDAY -> "Thu"
            Calendar.FRIDAY -> "Fri"
            Calendar.SATURDAY -> "Sat"
            else -> "Mon"
        }
    }

    fun getDayNumberForIndex(dayIndex: Int): Int {
        return dayIndex + 1
    }
}
