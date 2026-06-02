package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.CycleMetadata
import com.example.data.DayRecord
import com.example.data.WorkoutType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: TrackerViewModel) {
    val dayRecords by viewModel.allDayRecords.collectAsStateWithLifecycle()
    val workoutTypes by viewModel.allWorkoutTypes.collectAsStateWithLifecycle()
    val cycleMetadata by viewModel.cycleMetadata.collectAsStateWithLifecycle()

    var selectedWeekTab by remember { mutableStateOf(0) }
    
    // Theme setup
    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
    val backgroundColor = if (isDarkMode) Color(0xFF0F172A) else Color(0xFFF3F4F9)
    val cardBackground = if (isDarkMode) Color(0xFF1E293B) else Color.White
    val textColor = if (isDarkMode) Color(0xFFF1F5F9) else Color(0xFF1E293B)
    val subtitleColor = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B)
    val borderColor = if (isDarkMode) Color(0xFF334155) else Color(0xFFE2E8F0)
    val accentColor = if (isDarkMode) Color(0xFFD0BCFF) else Color(0xFF6750A4)
    val accentContainer = if (isDarkMode) Color(0xFF381E72) else Color(0xFFE8DEF8)
    val onAccentContainer = if (isDarkMode) Color(0xFFE8DEF8) else Color(0xFF21005D)

    // Dialog states
    var selectedDayDetailRecord by remember { mutableStateOf<DayRecord?>(null) }
    var showManageWorkoutsDialog by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }
    var showQuickSleepLogDialog by remember { mutableStateOf(false) }

    // Instant sleep quick-log states
    var inlineSleepDayIndex by remember { mutableStateOf(0) }
    var inlineSleepInput by remember { mutableStateOf("8.0") }
    var inlineSleepExpanded by remember { mutableStateOf(false) }

    // Aggregate values
    val totalWorkouts = dayRecords.count { it.isWorkoutDone }
    val totalProgress = if (dayRecords.isNotEmpty()) totalWorkouts / 12f else 0f
    
    // Compute current day of cycle
    val currentDayOfCycle = if (cycleMetadata?.startDateMillis != null) {
        val diff = System.currentTimeMillis() - cycleMetadata!!.startDateMillis
        val calculated = (diff / (24 * 60 * 60 * 1000)).toInt()
        (calculated + 1).coerceIn(1, 28)
    } else {
        1
    }

    // Workouts completed per week (7 days per week)
    val workoutsPerWeek = (0..3).map { weekIndex ->
        val startDay = weekIndex * 7
        val endDay = startDay + 7
        dayRecords.filter { it.dayIndex in startDay until endDay && it.isWorkoutDone }.size
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth().padding(end = 16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "App Icon Logo",
                                tint = accentColor,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Resistance Log",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleLarge,
                                color = textColor
                            )
                        }
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Dark Mode Toggle
                            IconButton(
                                onClick = { viewModel.toggleDarkMode() },
                                modifier = Modifier.testTag("theme_toggle_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "Toggle Theme",
                                    tint = if (isDarkMode) Color(0xFFFBBF24) else Color(0xFF94A3B8)
                                )
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            // Small Quick Reset Button inside action area
                            IconButton(
                                onClick = { showResetConfirmDialog = true },
                                modifier = Modifier.testTag("reset_top_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Reset entire tracker",
                                    tint = subtitleColor
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = cardBackground
                )
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(backgroundColor) // True "High Density" bg style
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // High Density Cycle Progress Bar Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = cardBackground),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                border = BorderStroke(1.dp, borderColor)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text(
                                text = "CYCLE PROGRESS — DAY $currentDayOfCycle/28",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = subtitleColor,
                                letterSpacing = 1.2.sp
                            )
                            Text(
                                text = "Weekly grid target: 3 workouts done",
                                style = MaterialTheme.typography.bodySmall,
                                color = subtitleColor
                            )
                        }
                        Text(
                            text = "${(totalProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = accentColor
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { totalProgress.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = accentColor,
                        trackColor = borderColor
                    )
                }
            }

            // High Density Week Tabs (With checkmark indicator inline)
            TabRow(
                selectedTabIndex = selectedWeekTab,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .padding(bottom = 16.dp),
                containerColor = cardBackground
            ) {
                (0..3).forEach { weekIndex ->
                    val isTargetAchieved = workoutsPerWeek[weekIndex] >= 3
                    Tab(
                        selected = selectedWeekTab == weekIndex,
                        onClick = { selectedWeekTab = weekIndex },
                        modifier = Modifier.testTag("week_tab_$weekIndex"),
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 8.dp)
                            ) {
                                Text(
                                    text = "Week ${weekIndex + 1}",
                                    fontWeight = if (selectedWeekTab == weekIndex) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 13.sp,
                                    color = if (selectedWeekTab == weekIndex) accentColor else subtitleColor
                                )
                                if (isTargetAchieved) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "✓",
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF16A34A),
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    )
                }
            }

            // 7-day grid container styled beautifully with rounded cards
            val weekStartIndex = selectedWeekTab * 7
            val weekEndIndex = weekStartIndex + 7
            val currentWeekRecords = dayRecords.filter { it.dayIndex in weekStartIndex until weekEndIndex }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = cardBackground),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                border = BorderStroke(1.dp, borderColor)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Current Week Grid (Week ${selectedWeekTab + 1})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                        // Light indicators
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            (1..3).forEach { indicatorIdx ->
                                val filled = workoutsPerWeek[selectedWeekTab] >= indicatorIdx
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(if (filled) Color(0xFF16A34A) else borderColor)
                                )
                            }
                        }
                    }

                    // A breathtaking 4-column layout for the 7 days
                    Box(modifier = Modifier.heightIn(max = 220.dp)) {
                        if (currentWeekRecords.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(4),
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(currentWeekRecords) { record ->
                                    val weekdayName = viewModel.getWeekdayForDayIndex(record.dayIndex, cycleMetadata?.startDateMillis)
                                    val dayNum = viewModel.getDayNumberForIndex(record.dayIndex)
                                    
                                    DayGridCellItem(
                                        record = record,
                                        weekdayName = weekdayName,
                                        dayNum = dayNum,
                                        isDarkMode = isDarkMode,
                                        onClick = {
                                            selectedDayDetailRecord = record
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Quick pre-assign routine row matching Tailwind "Workout Selection" quick switch feature!
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = cardBackground),
                border = BorderStroke(1.dp, borderColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Routine Quick Picker",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = subtitleColor
                        )
                        TextButton(
                            onClick = { showManageWorkoutsDialog = true },
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("Edit Routines", style = MaterialTheme.typography.labelSmall, color = accentColor)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(workoutTypes) { type ->
                            InputChip(
                                selected = false,
                                onClick = {
                                    // Easily preassign this label to the first active uncompleted day of the selected week!
                                    val firstUncompleted = currentWeekRecords.filter { !it.isWorkoutDone && !it.isRestDay }.minByOrNull { it.dayIndex }
                                    if (firstUncompleted != null) {
                                        viewModel.updateDayRecord(firstUncompleted.copy(assignedWorkoutType = type.name))
                                    }
                                },
                                label = { Text("Next: ${type.name}", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                colors = InputChipDefaults.inputChipColors(
                                    containerColor = cardBackground,
                                    labelColor = accentColor
                                ),
                                border = BorderStroke(1.dp, if (isDarkMode) Color(0xFF818CF8) else Color(0xFFC7D2FE))
                            )
                        }
                    }
                }
            }

            // Lavender High-Density Inline Sleep Recovery Log Card
            Card(
                colors = CardDefaults.cardColors(containerColor = if (isDarkMode) Color(0xFF2E1E55) else Color(0xFFE8DEF8)),
                border = BorderStroke(1.dp, if (isDarkMode) Color(0xFF4F378B) else Color(0xFFD0BCFF)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Sleep Recovery Log",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall,
                            color = if (isDarkMode) Color(0xFFE8DEF8) else Color(0xFF21005D)
                        )
                        Box(
                            modifier = Modifier
                                .background(if (isDarkMode) Color(0xFF4F378B) else Color(0xFFD0BCFF), shape = RoundedCornerShape(50))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "LAST NIGHT",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = if (isDarkMode) Color(0xFFE8DEF8) else Color(0xFF21005D),
                                fontSize = 8.sp
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Dropdown for target day
                        Box(modifier = Modifier.weight(1.3f)) {
                            ExposedDropdownMenuBox(
                                expanded = inlineSleepExpanded,
                                onExpandedChange = { inlineSleepExpanded = it }
                            ) {
                                val activeLabel = "Day ${viewModel.getDayNumberForIndex(inlineSleepDayIndex)}"
                                OutlinedTextField(
                                    value = activeLabel,
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = inlineSleepExpanded) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = cardBackground,
                                        unfocusedContainerColor = cardBackground,
                                        focusedBorderColor = if (isDarkMode) Color(0xFF818CF8) else Color(0xFFD0BCFF),
                                        unfocusedBorderColor = if (isDarkMode) Color(0xFF818CF8) else Color(0xFFD0BCFF),
                                        focusedTextColor = textColor,
                                        unfocusedTextColor = textColor
                                    ),
                                    textStyle = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    singleLine = true
                                )
                                ExposedDropdownMenu(
                                    expanded = inlineSleepExpanded,
                                    onDismissRequest = { inlineSleepExpanded = false },
                                    modifier = Modifier.height(180.dp)
                                ) {
                                    dayRecords.forEach { record ->
                                        val optLabel = "${viewModel.getWeekdayForDayIndex(record.dayIndex, cycleMetadata?.startDateMillis)} (Day ${viewModel.getDayNumberForIndex(record.dayIndex)})"
                                        DropdownMenuItem(
                                            text = { Text(optLabel, fontSize = 12.sp) },
                                            onClick = {
                                                inlineSleepDayIndex = record.dayIndex
                                                inlineSleepExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Hours sleep numeric input
                        OutlinedTextField(
                            value = inlineSleepInput,
                            onValueChange = { inlineSleepInput = it },
                            placeholder = { Text("5.5") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = cardBackground,
                                unfocusedContainerColor = cardBackground,
                                focusedBorderColor = if (isDarkMode) Color(0xFF818CF8) else Color(0xFFD0BCFF),
                                unfocusedBorderColor = if (isDarkMode) Color(0xFF818CF8) else Color(0xFFD0BCFF),
                                focusedTextColor = textColor,
                                unfocusedTextColor = textColor
                            ),
                            textStyle = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            singleLine = true
                        )

                        // Save Sleep Button
                        Button(
                            onClick = {
                                val sleepHoursFloat = inlineSleepInput.toFloatOrNull() ?: 8f
                                val record = dayRecords.find { it.dayIndex == inlineSleepDayIndex }
                                if (record != null) {
                                    viewModel.updateDayRecord(record.copy(sleepHours = sleepHoursFloat))
                                }
                                inlineSleepInput = "8.0"
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Log", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Note: Sleeping < 6 hrs adds two rest recovery days instead of one.",
                        style = MaterialTheme.typography.bodySmall,
                        color = (if (isDarkMode) Color(0xFFE8DEF8) else Color(0xFF21005D)).copy(alpha = 0.7f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Daily Reminder Settings Section (M3 design Card)
            val reminderEnabled by viewModel.reminderEnabled.collectAsStateWithLifecycle()
            val reminderHour by viewModel.reminderHour.collectAsStateWithLifecycle()
            val reminderMinute by viewModel.reminderMinute.collectAsStateWithLifecycle()

            var editReminderEnabled by remember(reminderEnabled) { mutableStateOf(reminderEnabled) }
            var editHour by remember(reminderHour) { mutableStateOf(reminderHour) }
            var editMinute by remember(reminderMinute) { mutableStateOf(reminderMinute) }

            val context = androidx.compose.ui.platform.LocalContext.current
            val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                if (isGranted) {
                    viewModel.updateReminderSettings(true, editHour, editMinute)
                } else {
                    editReminderEnabled = false
                    viewModel.updateReminderSettings(false, editHour, editMinute)
                    android.widget.Toast.makeText(context, "Notification permission is required for reminders.", android.widget.Toast.LENGTH_SHORT).show()
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = cardBackground),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                border = BorderStroke(1.dp, borderColor)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Reminder Icon",
                                tint = accentColor,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Daily Logging Reminder",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = textColor
                                )
                                Text(
                                    text = if (reminderEnabled) "Scheduled for %02d:%02d".format(reminderHour, reminderMinute) else "Off / Disabled",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = subtitleColor,
                                    fontSize = 11.sp
                                )
                            }
                        }
                        
                        Switch(
                            checked = editReminderEnabled,
                            onCheckedChange = { isChecked ->
                                editReminderEnabled = isChecked
                                if (isChecked) {
                                    if (android.os.Build.VERSION.SDK_INT >= 33) {
                                        val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                                            context,
                                            android.Manifest.permission.POST_NOTIFICATIONS
                                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                        if (hasPermission) {
                                            viewModel.updateReminderSettings(true, editHour, editMinute)
                                        } else {
                                            permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                        }
                                    } else {
                                        viewModel.updateReminderSettings(true, editHour, editMinute)
                                    }
                                } else {
                                    viewModel.updateReminderSettings(false, editHour, editMinute)
                                }
                            }
                        )
                    }

                    // Expandable scheduler config time adjustments
                    AnimatedVisibility(visible = editReminderEnabled) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp)
                        ) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = borderColor)
                            
                            Text(
                                text = "Set Daily Frequency Time:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = subtitleColor
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                // Hour modification buttons
                                Button(
                                    onClick = { editHour = (editHour - 1 + 24) % 24 },
                                    colors = ButtonDefaults.buttonColors(containerColor = accentContainer, contentColor = onAccentContainer),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp),
                                    modifier = Modifier.defaultMinSize(minHeight = 32.dp)
                                ) {
                                    Text("-", fontWeight = FontWeight.Bold)
                                }
                                
                                Text(
                                    text = "%02d".format(editHour),
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Black,
                                    color = textColor,
                                    modifier = Modifier.padding(horizontal = 12.dp)
                                )
                                
                                Button(
                                    onClick = { editHour = (editHour + 1) % 24 },
                                    colors = ButtonDefaults.buttonColors(containerColor = accentContainer, contentColor = onAccentContainer),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp),
                                    modifier = Modifier.defaultMinSize(minHeight = 32.dp)
                                ) {
                                    Text("+", fontWeight = FontWeight.Bold)
                                }
                                
                                Text(
                                    text = ":",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )

                                // Minute modification buttons
                                Button(
                                    onClick = { editMinute = (editMinute - 5 + 60) % 60 },
                                    colors = ButtonDefaults.buttonColors(containerColor = accentContainer, contentColor = onAccentContainer),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp),
                                    modifier = Modifier.defaultMinSize(minHeight = 32.dp)
                                ) {
                                    Text("-5m", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                
                                Text(
                                    text = "%02d".format(editMinute),
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Black,
                                    color = textColor,
                                    modifier = Modifier.padding(horizontal = 12.dp)
                                )
                                
                                Button(
                                    onClick = { editMinute = (editMinute + 5) % 60 },
                                    colors = ButtonDefaults.buttonColors(containerColor = accentContainer, contentColor = onAccentContainer),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp),
                                    modifier = Modifier.defaultMinSize(minHeight = 32.dp)
                                ) {
                                    Text("+5m", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Button(
                                onClick = {
                                    viewModel.updateReminderSettings(true, editHour, editMinute)
                                    android.widget.Toast.makeText(context, "Reminder scheduled successfully!", android.widget.Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Update Alarm Time", fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }

            // Bottom aesthetic footer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "CURRENT CYCLE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = subtitleColor
                    )
                    Text(
                        text = "Local Storage Only (Room)",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                }
                
                // Big FAB Like Master Log Workout trigger
                Button(
                    onClick = {
                        // Triggers detail action on the first uncompleted day
                        val activeDay = currentWeekRecords.firstOrNull { !it.isWorkoutDone && !it.isRestDay } ?: currentWeekRecords.firstOrNull()
                        if (activeDay != null) {
                            selectedDayDetailRecord = activeDay
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = if (isDarkMode) Color(0xFF1E293B) else Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Log Workout", fontWeight = FontWeight.Bold, color = if (isDarkMode) Color(0xFF1E293B) else Color.White, fontSize = 13.sp)
                }
            }
        }
    }

    // --- DAY DETAIL ACTIONS DIALOG ---
    selectedDayDetailRecord?.let { initialRecord ->
        val activeRecord = dayRecords.find { it.dayIndex == initialRecord.dayIndex } ?: initialRecord
        
        DayDetailDialog(
            record = activeRecord,
            workoutTypes = workoutTypes,
            getWeekday = { viewModel.getWeekdayForDayIndex(activeRecord.dayIndex, cycleMetadata?.startDateMillis) },
            getDayNum = { viewModel.getDayNumberForIndex(activeRecord.dayIndex) },
            onDismiss = { selectedDayDetailRecord = null },
            onUpdate = { updated ->
                viewModel.updateDayRecord(updated)
            }
        )
    }

    // --- MANAGE WORKOUTS DIALOG ---
    if (showManageWorkoutsDialog) {
        ManageWorkoutsDialog(
            workoutTypes = workoutTypes,
            onAdd = { viewModel.addWorkoutType(it) },
            onEdit = { viewModel.updateWorkoutType(it) },
            onDelete = { viewModel.deleteWorkoutType(it) },
            onDismiss = { showManageWorkoutsDialog = false }
        )
    }

    // --- RESET CONFIRMATION DIALOG ---
    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = { Text("Reset 28-Day Cycle?") },
            text = { Text("This will erase all marked workouts, sleep logs, rest schedules, and restart a clean 28-day cycle starting today. This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetCycle()
                        showResetConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("Reset Everything")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // --- QUICK SLEEP LOG DIALOG ---
    if (showQuickSleepLogDialog) {
        QuickSleepLogDialog(
            dayRecords = dayRecords,
            getWeekday = { viewModel.getWeekdayForDayIndex(it, cycleMetadata?.startDateMillis) },
            getDayNum = { viewModel.getDayNumberForIndex(it) },
            onLogSleep = { dayIndex, hours ->
                val record = dayRecords.find { it.dayIndex == dayIndex }
                if (record != null) {
                    viewModel.updateDayRecord(record.copy(sleepHours = hours))
                }
                showQuickSleepLogDialog = false
            },
            onDismiss = { showQuickSleepLogDialog = false }
        )
    }
}

// BREATHTAKING COMPACT HIGH DENSITY WEEKLY GRID CELL
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DayGridCellItem(
    record: DayRecord,
    weekdayName: String,
    dayNum: Int,
    isDarkMode: Boolean = false,
    onClick: () -> Unit
) {
    val isDone = record.isWorkoutDone
    val isRest = record.isRestDay
    val isExtraRest = record.isExtraRestDay

    val containerColor = when {
        isDone -> if (isDarkMode) Color(0xFF064E3B) else Color(0xFFF0FDF4)
        isRest -> if (isDarkMode) Color(0xFF1E293B) else Color(0xFFF1F5F9)
        else -> if (isDarkMode) Color(0xFF10172A) else Color.White
    }

    val contentColor = when {
        isDone -> if (isDarkMode) Color(0xFF34D399) else Color(0xFF166534)
        isRest -> if (isDarkMode) Color(0xFF64748B) else Color(0xFF64748B)
        else -> if (isDarkMode) Color(0xFFC7D2FE) else Color(0xFF1E293B)
    }

    val borderStroke = when {
        isDone -> BorderStroke(1.dp, if (isDarkMode) Color(0xFF047857) else Color(0xFFBBF7D0))
        isRest -> BorderStroke(1.dp, if (isDarkMode) Color(0xFF334155) else Color(0xFFE2E8F0))
        else -> BorderStroke(2.dp, if (isDarkMode) Color(0xFF818CF8) else Color(0xFFC7D2FE))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.9f) // High-density vertical compact square feel
            .combinedClickable(
                onClick = onClick,
                onLongClick = onClick
            )
            .testTag("day_card_${record.dayIndex}"),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        border = borderStroke,
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isRest) 0.dp else 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Day label
            Text(
                text = weekdayName.uppercase(),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = contentColor.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )

            // Huge day index number
            Text(
                text = "$dayNum",
                fontSize = 19.sp,
                fontWeight = FontWeight.Black,
                color = contentColor,
                textAlign = TextAlign.Center
            )

            // Tiny indicator label at bottom
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                when {
                    isDone -> {
                        Text(
                            text = (record.completedWorkoutType ?: "DONE").uppercase(),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkMode) Color(0xFF34D399) else Color(0xFF15803D),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    isRest -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            if (isExtraRest) {
                                Text(text = "😴", fontSize = 8.sp)
                                Spacer(modifier = Modifier.width(1.dp))
                            }
                            Text(
                                text = if (isExtraRest) "REC" else "REST",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDarkMode) Color(0xFF64748B) else Color(0xFF94A3B8)
                            )
                        }
                    }
                    else -> {
                        if (record.assignedWorkoutType != null) {
                            Text(
                                text = record.assignedWorkoutType.uppercase(),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isDarkMode) Color(0xFF818CF8) else Color(0xFF6750A4),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else {
                            // Active start action prompt
                            Box(
                                modifier = Modifier
                                    .background(if (isDarkMode) Color(0xFF818CF8) else Color(0xFF6750A4), shape = RoundedCornerShape(50))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "LOG",
                                    fontSize = 7.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isDarkMode) Color(0xFF1E1B4B) else Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- DAY ACTIONS / LOGGING DIALOG ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayDetailDialog(
    record: DayRecord,
    workoutTypes: List<WorkoutType>,
    getWeekday: () -> String,
    getDayNum: () -> Int,
    onDismiss: () -> Unit,
    onUpdate: (DayRecord) -> Unit
) {
    var sleepInput by remember { mutableStateOf(record.sleepHours?.toString() ?: "8.0") }
    var selectedWorkoutType by remember { mutableStateOf(record.completedWorkoutType ?: record.assignedWorkoutType ?: workoutTypes.firstOrNull()?.name ?: "Upper") }
    var dropdownExpanded by remember { mutableStateOf(false) }
    
    // Warn override flag
    var bypassWarning by remember { mutableStateOf(false) }

    val sleepVal = sleepInput.toFloatOrNull() ?: 8f

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "${getWeekday()} (Day ${getDayNum()}) Details",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF6750A4)
                )
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // CURRENT STATUS CARD
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Status: " + when {
                                record.isWorkoutDone -> "Workout Complete! ✅"
                                record.isRestDay -> "Rest Day 😴"
                                else -> "Normal / Unscheduled"
                            },
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        if (record.isWorkoutDone) {
                            Text(
                                text = "Logged workout: ${record.completedWorkoutType}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                // SLEEP MANUAL HOURS INPUT
                Text(
                    text = "Sleep hours night before",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = sleepInput,
                        onValueChange = { sleepInput = it },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        label = { Text("Hours slept") },
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = { sleepInput = "8.0" }) { Text("8h") }
                    TextButton(onClick = { sleepInput = "5.5" }) { Text("<6h") }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // RECOVERY OVERRIDE WARNING
                val isRestAndNotDone = record.isRestDay && !record.isWorkoutDone
                if (isRestAndNotDone && !bypassWarning) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Rest Day Warning!",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Muscles grow and repair during rest days, not workout days. Working out on scheduled rest days impacts physical recovery and significantly increases risk of overtraining or joint injury.",
                                style = MaterialTheme.typography.bodySmall,
                                lineHeight = 16.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(
                                    onClick = { bypassWarning = true },
                                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Text("Dismiss Warning & Unlock")
                                }
                            }
                        }
                    }
                }

                // WORKOUT SELECTOR (Disabled if rest day and bypass warning was not checked)
                val allowLogWorkout = !isRestAndNotDone || bypassWarning
                
                Text(
                    text = "Workout Type",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (allowLogWorkout) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                
                ExposedDropdownMenuBox(
                    expanded = dropdownExpanded && allowLogWorkout,
                    onExpandedChange = { if (allowLogWorkout) dropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedWorkoutType,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        colors = if (allowLogWorkout) OutlinedTextFieldDefaults.colors() else OutlinedTextFieldDefaults.colors(
                            disabledContainerColor = Color.Transparent,
                            disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                            disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        ),
                        enabled = allowLogWorkout
                    )
                    ExposedDropdownMenu(
                        expanded = dropdownExpanded && allowLogWorkout,
                        onDismissRequest = { dropdownExpanded = false }
                    ) {
                        workoutTypes.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.name) },
                                onClick = {
                                    selectedWorkoutType = type.name
                                    dropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ACTION BUTTONS
                if (record.isWorkoutDone) {
                    Button(
                        onClick = {
                            onUpdate(record.copy(sleepHours = sleepVal, completedWorkoutType = selectedWorkoutType))
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save Details")
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedButton(
                        onClick = {
                            onUpdate(record.copy(isWorkoutDone = false, completedWorkoutType = null))
                            onDismiss()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Undo Workout Log")
                    }
                } else {
                    Button(
                        onClick = {
                            onUpdate(record.copy(
                                isWorkoutDone = true,
                                completedWorkoutType = selectedWorkoutType,
                                sleepHours = sleepVal,
                                isRestDay = false
                            ))
                            onDismiss()
                        },
                        enabled = allowLogWorkout,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isRestAndNotDone) MaterialTheme.colorScheme.error else Color(0xFF6750A4)
                        )
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isRestAndNotDone) "Confirm Overridden Workout" else "Mark Workout Done")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = {
                            onUpdate(record.copy(
                                assignedWorkoutType = selectedWorkoutType,
                                sleepHours = sleepVal
                            ))
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Pre-Assign Plan / Save Sleep Only")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("Close")
                }
            }
        }
    }
}

// --- MANAGE WORKOUTS DIALOG ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageWorkoutsDialog(
    workoutTypes: List<WorkoutType>,
    onAdd: (String) -> Unit,
    onEdit: (WorkoutType) -> Unit,
    onDelete: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var newWorkoutName by remember { mutableStateOf("") }
    var editingWorkoutId by remember { mutableStateOf<Int?>(null) }
    var editingWorkoutName by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = "Workout Names",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF6750A4)
                )
                Text(
                    text = "Manage your routine definitions",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // LIST OF WORKOUT TYPES
                Box(
                    modifier = Modifier
                        .height(200.dp)
                        .fillMaxWidth()
                ) {
                    if (workoutTypes.isEmpty()) {
                        Text(
                            text = "No workouts loaded. Add one below.",
                            modifier = Modifier.align(Alignment.Center),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(workoutTypes) { type ->
                                val isEditing = editingWorkoutId == type.id
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (isEditing) {
                                        OutlinedTextField(
                                            value = editingWorkoutName,
                                            onValueChange = { editingWorkoutName = it },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f).padding(end = 8.dp),
                                            textStyle = MaterialTheme.typography.bodyMedium
                                        )
                                        Row {
                                            IconButton(
                                                onClick = {
                                                    if (editingWorkoutName.isNotBlank()) {
                                                        onEdit(type.copy(name = editingWorkoutName.trim()))
                                                        editingWorkoutId = null
                                                    }
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "Save Edit",
                                                    tint = Color(0xFF16A34A)
                                                )
                                            }
                                            IconButton(onClick = { editingWorkoutId = null }) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Cancel Edit",
                                                    tint = MaterialTheme.colorScheme.outline
                                                )
                                            }
                                        }
                                    } else {
                                        Text(
                                            text = type.name,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Row {
                                            IconButton(
                                                onClick = {
                                                    editingWorkoutId = type.id
                                                    editingWorkoutName = type.name
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = "Edit Work Type",
                                                    tint = Color(0xFF6750A4)
                                                )
                                            }
                                            IconButton(
                                                onClick = { onDelete(type.id) },
                                                enabled = workoutTypes.size > 1
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete Work Type",
                                                    tint = if (workoutTypes.size > 1) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // NEW WORKOUT WORD INPUT
                OutlinedTextField(
                    value = newWorkoutName,
                    onValueChange = { newWorkoutName = it },
                    label = { Text("New Workout Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        if (newWorkoutName.isNotBlank()) {
                            onAdd(newWorkoutName)
                            newWorkoutName = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Workout Type")
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("Done")
                }
            }
        }
    }
}

// --- QUICK SLEEP SHORTCUT LOG DIALOG ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickSleepLogDialog(
    dayRecords: List<DayRecord>,
    getWeekday: (Int) -> String,
    getDayNum: (Int) -> Int,
    onLogSleep: (dayIndex: Int, sleepHours: Float) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedDayIndex by remember { mutableStateOf(0) }
    var sleepInput by remember { mutableStateOf("8.0") }
    var expanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = "Quick Sleep Logger",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF6750A4)
                )
                Text(
                    text = "Log sleep for any day in this cycle",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                Text(
                    text = "Select Day",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    val activeLabel = "${getWeekday(selectedDayIndex)} (Day ${getDayNum(selectedDayIndex)})"
                    OutlinedTextField(
                        value = activeLabel,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.height(200.dp)
                    ) {
                        dayRecords.forEach { record ->
                            val label = "${getWeekday(record.dayIndex)} (Day ${getDayNum(record.dayIndex)})"
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    selectedDayIndex = record.dayIndex
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Hours Slept",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = sleepInput,
                        onValueChange = { sleepInput = it },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = { sleepInput = "8.0" }) { Text("8h") }
                    TextButton(onClick = { sleepInput = "5.5" }) { Text("<6h") }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        val hours = sleepInput.toFloatOrNull() ?: 8f
                        onLogSleep(selectedDayIndex, hours)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save Sleep Log")
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}
