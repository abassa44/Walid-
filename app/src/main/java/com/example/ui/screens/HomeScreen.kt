package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.StudyAppointment
import com.example.data.StudyTask
import com.example.data.StudySession
import com.example.ui.viewmodel.StudyViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: StudyViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) }
    
    // UI Dialog flags
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var showAddApptDialog by remember { mutableStateOf(false) }
    
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val appointments by viewModel.appointments.collectAsStateWithLifecycle()
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    
    // Focus screen overlay states
    val isTimerRunning by viewModel.isTimerRunning.collectAsStateWithLifecycle()
    var isFocusOverlayVisible by remember { mutableStateOf(false) }
    
    // Automatically open distraction-free focus screen when a session starts
    LaunchedEffect(isTimerRunning) {
        if (isTimerRunning) {
            isFocusOverlayVisible = true
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.MenuBook,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "منظم الدراسة والمهام اليومية",
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            },
            bottomBar = {
                // M3 Standard Dynamic Bottom Navigation Bar
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Outlined.Timer, contentDescription = "المؤقت") },
                        label = { Text("المؤقت والتركيز", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                        modifier = Modifier.testTag("tab_timer")
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Outlined.Assignment, contentDescription = "المهام") },
                        label = { Text("الجدول والمهام", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                        modifier = Modifier.testTag("tab_tasks")
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = { Icon(Icons.Outlined.AutoAwesome, contentDescription = "المستشار") },
                        label = { Text("المستشار والمساعد", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                        modifier = Modifier.testTag("tab_ai")
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.background,
                                MaterialTheme.colorScheme.background.copy(alpha = 0.95f),
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    )
            ) {
                // Screen content switching based on selectedTab
                when (selectedTab) {
                    0 -> TimerTab(
                        viewModel = viewModel,
                        sessions = sessions,
                        onEnterFocusMode = { isFocusOverlayVisible = true }
                    )
                    1 -> ScheduleTab(
                        tasks = tasks,
                        appointments = appointments,
                        onTaskCheckChanged = { task -> viewModel.updateTask(task.copy(isCompleted = !task.isCompleted)) },
                        onDeleteTask = { viewModel.deleteTask(it) },
                        onDeleteAppt = { viewModel.deleteAppointment(it) },
                        onAddTaskClick = { showAddTaskDialog = true },
                        onAddApptClick = { showAddApptDialog = true }
                    )
                    2 -> AiAdviseTab(viewModel)
                }
            }
        }
        
        // Fullscreen Distraction-Free Focus Overlay Screen
        AnimatedVisibility(
            visible = isFocusOverlayVisible && isTimerRunning,
            enter = fadeIn(animationSpec = tween(500)) + slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(500, easing = EaseOutCubic)
            ),
            exit = fadeOut(animationSpec = tween(400)) + slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(400, easing = EaseInCubic)
            )
        ) {
            DistractionFreeFocusScreen(
                viewModel = viewModel,
                onMinimize = { isFocusOverlayVisible = false }
            )
        }
    }
    
    // Dialogs overlay
    if (showAddTaskDialog) {
        AddTaskDialog(
            onDismiss = { showAddTaskDialog = false },
            onSave = { title, subject, priority, note, relativeDays ->
                val dueDateTimestamp = System.currentTimeMillis() + (relativeDays * 86400000L)
                viewModel.insertTask(
                    StudyTask(
                        title = title,
                        subject = subject,
                        dueDate = dueDateTimestamp,
                        priority = priority,
                        notes = note
                    )
                )
                showAddTaskDialog = false
                Toast.makeText(context, "تمت إضافة المهمة بنجاح!", Toast.LENGTH_SHORT).show()
            }
        )
    }
    
    if (showAddApptDialog) {
        AddAppointmentDialog(
            onDismiss = { showAddApptDialog = false },
            onSave = { title, subject, location, notes, relativeDays ->
                val appointmentTimestamp = System.currentTimeMillis() + (relativeDays * 86400000L)
                viewModel.insertAppointment(
                    StudyAppointment(
                        title = title,
                        subject = subject,
                        dateTime = appointmentTimestamp,
                        location = location,
                        notes = notes
                    )
                )
                showAddApptDialog = false
                Toast.makeText(context, "تمت جدولة الموعد بنجاح!", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

// ==================== TAB 0: POMODORO TIMER ====================
@Composable
fun TimerTab(
    viewModel: StudyViewModel,
    sessions: List<StudySession>,
    onEnterFocusMode: () -> Unit
) {
    val secondsRemaining by viewModel.timerSecondsRemaining.collectAsStateWithLifecycle()
    val totalSeconds by viewModel.timerTotalSeconds.collectAsStateWithLifecycle()
    val isRunning by viewModel.isTimerRunning.collectAsStateWithLifecycle()
    val isBreak by viewModel.isBreak.collectAsStateWithLifecycle()
    val activeSubject by viewModel.activeSubject.collectAsStateWithLifecycle()
    
    // Quick Study session setup inputs
    var inputSubject by remember { mutableStateOf("") }
    var inputMinutes by remember { mutableStateOf(25) }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            // Dynamic greeting in Arabic based on current time
            Text(
                text = getArabicGreeting(),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 12.dp)
            )
            
            Text(
                text = "ركز بعمق ونظم وقتك لمذاكرة مثمرة وجديرة بالنجاح",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 24.dp)
            )
        }
        
        item {
            // Beautiful Interactive circular Pomodoro widget
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Badge for State (Studying vs Rest)
                    Box(
                        modifier = Modifier
                            .background(
                                if (isBreak) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                RoundedCornerShape(50.dp)
                            )
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (isBreak) "⏸️ استراحة سريعة" else "✏️ جلسة تركيز للمذاكرة",
                            color = if (isBreak) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Timer display
                    val formattedMinutes = String.format("%02d", secondsRemaining / 60)
                    val formattedSeconds = String.format("%02d", secondsRemaining % 60)
                    
                    Text(
                        text = "$formattedMinutes:$formattedSeconds",
                        fontSize = 58.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        letterSpacing = 2.sp
                    )
                    
                    if (activeSubject.isNotEmpty()) {
                        Text(
                            text = "المادة النشطة: $activeSubject",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Progress Bar
                    val progressFraction = if (totalSeconds > 0) {
                        secondsRemaining.toFloat() / totalSeconds.toFloat()
                    } else 0f
                    
                    LinearProgressIndicator(
                        progress = { progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = if (isBreak) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                    )
                    
                    if (secondsRemaining > 0 && isRunning && !isBreak) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onEnterFocusMode,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "دخول بيئة التركيز المريحة ✨",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Timer Controls Actions
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (secondsRemaining > 0) {
                            IconButton(
                                onClick = { viewModel.resetTimer() },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("timer_reset")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.RestartAlt,
                                    contentDescription = "إعادة الضبط",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(24.dp))
                            
                            FloatingActionButton(
                                onClick = {
                                    if (isRunning) viewModel.pauseTimer() else viewModel.resumeTimer()
                                },
                                containerColor = if (isRunning) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                                shape = CircleShape,
                                modifier = Modifier
                                    .size(56.dp)
                                    .testTag("timer_play_pause")
                            ) {
                                Icon(
                                    imageVector = if (isRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    contentDescription = "مؤقت",
                                    tint = MaterialTheme.colorScheme.background,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        } else {
                            // Setup section if timer not active
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                OutlinedTextField(
                                    value = inputSubject,
                                    onValueChange = { inputSubject = it },
                                    label = { Text("المادة للمذاكرة (مثال: رياضيات)") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("timer_subject_input"),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    listOf(15, 25, 45, 60).forEach { mins ->
                                        FilterChip(
                                            selected = inputMinutes == mins,
                                            onClick = { inputMinutes = mins },
                                            label = { Text("$mins د") },
                                            modifier = Modifier.testTag("timer_chip_$mins")
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                Button(
                                    onClick = { viewModel.startTimer(inputSubject, inputMinutes) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("timer_start"),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Filled.PlayCircle, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("ابدأ جلسة التركيز الآن", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
        
        item {
            SoundManagerCard(viewModel = viewModel)
        }
        
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                Text(
                    text = "📈 إنجازاتك وجلساتك السابقة",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        if (sessions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "لا توجد جلسات تركيز مسجلة اليوم. ابدأ وسجل نجاحك!",
                        modifier = Modifier.padding(24.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            items(sessions.take(5)) { session ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "جلسة: ${session.subject}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "المدة: ${session.completedMinutes} دقيقة • تم الإنجاز بنجاح",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                        Text(
                            text = "⭐️ 5/5",
                            color = MaterialTheme.colorScheme.tertiary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

// ==================== TAB 1: SCHEDULE & TASKS ====================
@Composable
fun ScheduleTab(
    tasks: List<StudyTask>,
    appointments: List<StudyAppointment>,
    onTaskCheckChanged: (StudyTask) -> Unit,
    onDeleteTask: (StudyTask) -> Unit,
    onDeleteAppt: (StudyAppointment) -> Unit,
    onAddTaskClick: () -> Unit,
    onAddApptClick: () -> Unit
) {
    var showTasksOnly by remember { mutableStateOf(true) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Toggle view buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = { showTasksOnly = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (showTasksOnly) MaterialTheme.colorScheme.primary else Color.Transparent,
                    contentColor = if (showTasksOnly) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("view_tasks_button")
            ) {
                Icon(Icons.Filled.ListAlt, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("المهام اليومية (${tasks.size})", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            
            Button(
                onClick = { showTasksOnly = false },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (!showTasksOnly) MaterialTheme.colorScheme.primary else Color.Transparent,
                    contentColor = if (!showTasksOnly) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("view_appointments_button")
            ) {
                Icon(Icons.Filled.CalendarMonth, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("الامتحانات والمواعيد (${appointments.size})", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
        
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (showTasksOnly) {
                // Tasks List Group
                if (tasks.isEmpty()) {
                    EmptyStateCard(
                        title = "لا توجد مهام دراسية حالياً!",
                        description = "أضف بعض الواجبات المنزلية، المشاريع الفردية، أو فصول القراءة لتبدأ المتابعة المنظمة.",
                        onAddClick = onAddTaskClick,
                        buttonText = "أضف مهمتك الأولى"
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(tasks) { task ->
                            StudyTaskItem(
                                task = task,
                                onCheckChanged = { onTaskCheckChanged(task) },
                                onDelete = { onDeleteTask(task) }
                            )
                        }
                        item {
                            Spacer(modifier = Modifier.height(80.dp)) // height padding for bottom action
                        }
                    }
                    
                    // FAB to add task
                    FloatingActionButton(
                        onClick = onAddTaskClick,
                        containerColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp)
                            .testTag("fab_add_task")
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "إضافة مهمة")
                    }
                }
            } else {
                // Appointments / Exams List Group
                if (appointments.isEmpty()) {
                    EmptyStateCard(
                        title = "لم تجدول أي مواعيد أو امتحانات قادمة!",
                        description = "إن إضافة مواعيد الاختبارات وجلسات المذاكرة الجماعية والمحاضرات يدعم توازن وقتك بكفاءة.",
                        onAddClick = onAddApptClick,
                        buttonText = "جدول موعداً جديداً"
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(appointments) { appt ->
                            StudyAppointmentItem(
                                appt = appt,
                                onDelete = { onDeleteAppt(appt) }
                            )
                        }
                        item {
                            Spacer(modifier = Modifier.height(80.dp))
                        }
                    }
                    
                    // FAB to add appointment
                    FloatingActionButton(
                        onClick = onAddApptClick,
                        containerColor = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp)
                            .testTag("fab_add_appt")
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "جدولة موعد")
                    }
                }
            }
        }
    }
}

@Composable
fun StudyTaskItem(
    task: StudyTask,
    onCheckChanged: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("task_item_${task.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = { onCheckChanged() },
                modifier = Modifier.testTag("task_checkbox_${task.id}")
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = if (task.isCompleted) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    // Badge Subject
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = task.subject,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    // Badge Priority
                    val priorityColor = when (task.priority) {
                        "High" -> Color(0xFFEF4444)
                        "Medium" -> MaterialTheme.colorScheme.tertiary
                        else -> Color(0xFF22C55E)
                    }
                    val priorityArabic = when (task.priority) {
                        "High" -> "هام عاجل"
                        "Medium" -> "متوسط"
                        else -> "منخفض"
                    }
                    
                    Text(
                        text = "• $priorityArabic",
                        fontSize = 12.sp,
                        color = priorityColor,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                if (task.notes.isNotEmpty()) {
                    Text(
                        text = task.notes,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.padding(top = 6.dp),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                Text(
                    text = "تاريخ الاستحقاق: ${formatArabicDate(task.dueDate)}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
            
            IconButton(
                onClick = onDelete,
                modifier = Modifier.testTag("task_delete_${task.id}")
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "حذف المهمة",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }
        }
    }
}

@Composable
fun StudyAppointmentItem(
    appt: StudyAppointment,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("appt_item_${appt.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.EventNote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = appt.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(
                        text = "المادة: ${appt.subject}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                if (appt.location.isNotEmpty()) {
                    Text(
                        text = "📍 القاعة/المكان: ${appt.location}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                
                if (appt.notes.isNotEmpty()) {
                    Text(
                        text = appt.notes,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                
                Text(
                    text = "🗓️ الموعد: ${formatArabicDate(appt.dateTime)}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
            
            IconButton(
                onClick = onDelete,
                modifier = Modifier.testTag("appt_delete_${appt.id}")
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "حذف الموعد",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }
        }
    }
}

// ==================== TAB 2: AI STUDY ADVISOR ====================
@Composable
fun AiAdviseTab(viewModel: StudyViewModel) {
    val aiAdvice by viewModel.aiAdvice.collectAsStateWithLifecycle()
    val isAiLoading by viewModel.isAiLoading.collectAsStateWithLifecycle()
    val aiNotifications by viewModel.aiNotifications.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(42.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "المساعد الدراسي بنظام الذكاء الاصطناعي",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "يتولى المساعد فحص واجباتك والامتحانات القادمة وصياغة خطة تنبيهات ذكية مخصصة لضمان التحصيل العالي والمذاكرة المنظمة.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = { viewModel.fetchSmartScheduleAndAdvice() },
                        enabled = !isAiLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("fetch_ai_advice_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        if (isAiLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("تحليل وتهيئة الخطة والتنبيهات الذكية", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        
        // Smart notifications list generated by AI
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                Text(
                    text = "🔔 التنبيهات الذكية والحلول الموصى بها",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
        
        val alertsToShow = aiNotifications.ifEmpty {
            listOf(
                "خطط ليومك: جهز خطة المذاكرة واقض على التسويف فوراً.",
                "نصيحة الصباح: المذاكرة الصباحية تزيد الحفظ بأكثر من 30%!",
                "بومودورو: لا تنسَ شرب المياه وأخذ قسط من الراحة كل 25 دقيقة."
            )
        }
        
        items(alertsToShow) { alert ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        Toast.makeText(context, "إشعار تفاعلي: $alert", Toast.LENGTH_LONG).show()
                    },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.NotificationsActive,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = alert,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Filled.ChevronLeft,
                        contentDescription = "جرب التنبيه",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                }
            }
        }
        
        // Detailed advice markdown layout
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                Text(
                    text = "📋 الخطة الزمنية للمذاكرة وتحليل الوقت",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
        
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ai_advice_container"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    if (aiAdvice != null) {
                        Text(
                            text = aiAdvice!!,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 22.sp
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Lightbulb,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "اضغط على زر التحليل في الأعلى لإحصاء المهام والمواعيد وصياغة الجداول والتنبيهات المناسبة لك عبر الذكاء الاصطناعي.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==================== COMMON DECORATIVE / SUB-COMPOSABLES ====================

@Composable
fun EmptyStateCard(
    title: String,
    description: String,
    onAddClick: () -> Unit,
    buttonText: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
        )
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Outlined.ContentPasteOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
            Spacer(modifier = Modifier.height(18.dp))
            Button(
                onClick = onAddClick,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text(buttonText, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ==================== DIALOGS ====================

@Composable
fun AddTaskDialog(
    onDismiss: () -> Unit,
    onSave: (title: String, subject: String, priority: String, notes: String, relativeDays: Int) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("Medium") }
    var notes by remember { mutableStateOf("") }
    var selectedDaysIndex by remember { mutableStateOf(0) } // 0 = today, 1 = tomorrow, 2 = next week (7 days)
    
    val daysOptions = listOf("اليوم", "غداً", "خلال أسبوع")
    val daysValues = listOf(0, 1, 7)
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("dialog_add_task")
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = "إضافة مهمة دراسية جديدة",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("اسم المهمة (مثال: واجب الرياضيات صـ ٤)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_task_title_input"),
                    shape = RoundedCornerShape(12.dp)
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text("المادة الدراسية") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_task_subject_input"),
                    shape = RoundedCornerShape(12.dp)
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Priority Selector Chips
                Text(
                    text = "الأولوية ودرجة الأهمية",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf("High" to "عالية", "Medium" to "متوسطة", "Low" to "عادية").forEach { (level, name) ->
                        FilterChip(
                            selected = priority == level,
                            onClick = { priority = level },
                            label = { Text(name) },
                            modifier = Modifier.testTag("priority_chip_$level")
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Relatives due days Selector Chips
                Text(
                    text = "موعد الاستحقاق والتسليم",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    daysOptions.forEachIndexed { idx, option ->
                        FilterChip(
                            selected = selectedDaysIndex == idx,
                            onClick = { selectedDaysIndex = idx },
                            label = { Text(option) },
                            modifier = Modifier.testTag("days_chip_$idx")
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات إضافية") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .testTag("add_task_notes_input"),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 3
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("إلغاء", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (title.trim().isNotEmpty() && subject.trim().isNotEmpty()) {
                                onSave(title, subject, priority, notes, daysValues[selectedDaysIndex])
                            }
                        },
                        enabled = title.trim().isNotEmpty() && subject.trim().isNotEmpty(),
                        modifier = Modifier.testTag("add_task_save_button")
                    ) {
                        Text("حفظ المهمة", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun AddAppointmentDialog(
    onDismiss: () -> Unit,
    onSave: (title: String, subject: String, location: String, notes: String, relativeDays: Int) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var selectedDaysIndex by remember { mutableStateOf(0) } // 0 = today, 1 = tomorrow, 2 = next week (7 days)
    
    val daysOptions = listOf("اليوم", "غداً", "الأسبوع الدراسي القادم")
    val daysValues = listOf(0, 1, 7)
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("dialog_add_appt")
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = "جدولة موعد أو امتحان جديد",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("اسم الموعد (مثال: اختبار الشهر الأول)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_appt_title_input"),
                    shape = RoundedCornerShape(12.dp)
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text("المادة الدراسية") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_appt_subject_input"),
                    shape = RoundedCornerShape(12.dp)
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("قاعة الامتحان أو رابط الجلسة المباشرة (اختياري)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_appt_location_input"),
                    shape = RoundedCornerShape(12.dp)
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Relatives due days Selector Chips
                Text(
                    text = "تاريخ الموعد المقدر",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    daysOptions.forEachIndexed { idx, option ->
                        FilterChip(
                            selected = selectedDaysIndex == idx,
                            onClick = { selectedDaysIndex = idx },
                            label = { Text(option) },
                            modifier = Modifier.testTag("appt_days_chip_$idx")
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات أو مواضيع الامتحان المقررة") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .testTag("add_appt_notes_input"),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 3
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("إلغاء", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (title.trim().isNotEmpty() && subject.trim().isNotEmpty()) {
                                onSave(title, subject, location, notes, daysValues[selectedDaysIndex])
                            }
                        },
                        enabled = title.trim().isNotEmpty() && subject.trim().isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                        modifier = Modifier.testTag("add_appt_save_button")
                    ) {
                        Text("حفظ الموعد", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiary)
                    }
                }
            }
        }
    }
}

// ==================== UTILS ====================

private fun getArabicGreeting(): String {
    val cal = Calendar.getInstance()
    val hour = cal.get(Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..11 -> "☀️ صباح الخير والتركيز"
        in 12..16 -> "☕️ طاب يومك الدراسي بكل نشاط وحيوية"
        in 17..22 -> "🌙 مساء السعي والجد والاجتهاد"
        else -> "🦉 وقت طيب للدراسة والمثابرة"
    }
}

private fun formatArabicDate(timestamp: Long): String {
    val date = Date(timestamp)
    val sdf = SimpleDateFormat("EEEE، d MMMM yyyy", Locale("ar"))
    return sdf.format(date)
}

@Composable
fun SoundManagerCard(
    viewModel: StudyViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isSoundEnabled by viewModel.isSoundEnabled.collectAsStateWithLifecycle()
    val selectedSoundId by viewModel.selectedSoundId.collectAsStateWithLifecycle()
    val customSounds by viewModel.customSounds.collectAsStateWithLifecycle()

    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.addCustomSound(uri)
            Toast.makeText(context, "تمت إضافة الصوت المخصص بنجاح!", Toast.LENGTH_SHORT).show()
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.MusicNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "أصوات الخلفية وموسيقى التركيز",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Switch(
                    checked = isSoundEnabled,
                    onCheckedChange = { viewModel.setSoundEnabled(it) }
                )
            }

            Text(
                text = "سيتم تشغيل الصوت تلقائياً عند بدء المذاكرة لإبقائك في أعلى مستويات التركيز.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(vertical = 8.dp)
            )

            if (isSoundEnabled) {
                Spacer(modifier = Modifier.height(8.dp))

                // Built-in Sound Grid
                Text(
                    text = "الأصوات والمؤثرات المدمجة المتاحة:",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    viewModel.builtInSounds.forEach { (id, label) ->
                        val isSelected = selectedSoundId == id
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.selectSound(id) },
                            label = { Text(label, fontSize = 12.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Custom audio files uploaded from phone
                Text(
                    text = "📂 أصوات التركيز المضافة من هاتفك:",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (customSounds.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "لم تقم بإضافة أصوات مخصصة من هاتفك بعد.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        customSounds.forEach { (filePath, name) ->
                            val isSelected = selectedSoundId == filePath
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.selectSound(filePath) },
                                shape = RoundedCornerShape(12.dp),
                                border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (isSelected) Icons.Filled.VolumeUp else Icons.Filled.MusicNote,
                                            contentDescription = null,
                                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = name,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    IconButton(
                                        onClick = { viewModel.deleteCustomSound(filePath) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Delete,
                                            contentDescription = "حذف الصوت",
                                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Selector button
                OutlinedButton(
                    onClick = { audioPickerLauncher.launch("audio/*") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("استيراد / إضافة ملف صوتي من الهاتف", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun DistractionFreeFocusScreen(
    viewModel: StudyViewModel,
    onMinimize: () -> Unit,
    modifier: Modifier = Modifier
) {
    val secondsRemaining by viewModel.timerSecondsRemaining.collectAsStateWithLifecycle()
    val totalSeconds by viewModel.timerTotalSeconds.collectAsStateWithLifecycle()
    val isRunning by viewModel.isTimerRunning.collectAsStateWithLifecycle()
    val isBreak by viewModel.isBreak.collectAsStateWithLifecycle()
    val activeSubject by viewModel.activeSubject.collectAsStateWithLifecycle()
    val isSoundEnabled by viewModel.isSoundEnabled.collectAsStateWithLifecycle()
    val selectedSoundId by viewModel.selectedSoundId.collectAsStateWithLifecycle()
    val customSounds by viewModel.customSounds.collectAsStateWithLifecycle()

    // Breathing Animation Pulse Loop: Exhale/Inhale guide sequence
    val infiniteTransition = rememberInfiniteTransition(label = "breathing")
    val breathingProgress by infiniteTransition.animateFloat(
        initialValue = 0.72f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(4500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathing_pulse"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F172A), // Deep Slate Blue
                        Color(0xFF020617), // Deep Dark Obsidian
                        Color(0xFF1E1F38)  // Peaceful Indigo Depth
                    )
                )
            )
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        // Soft ambient pulsing color aura behind elements to create depth
        Box(
            modifier = Modifier
                .size(450.dp)
                .align(Alignment.Center)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF6366F1).copy(alpha = 0.12f * breathingProgress),
                            Color(0xFF0EA5E9).copy(alpha = 0.04f * breathingProgress),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Bar controls: Exit & Sound toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onMinimize,
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.08f), CircleShape)
                        .size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.FullscreenExit,
                        contentDescription = "تصغير شاشة التركيز",
                        tint = Color.White
                    )
                }

                Text(
                    text = "مساحة تركيز هادئة ومريحة",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )

                IconButton(
                    onClick = { viewModel.setSoundEnabled(!isSoundEnabled) },
                    modifier = Modifier
                        .background(
                            if (isSoundEnabled) Color(0xFF10B981).copy(alpha = 0.15f)
                            else Color.White.copy(alpha = 0.08f),
                            CircleShape
                        )
                        .size(44.dp)
                ) {
                    Icon(
                        imageVector = if (isSoundEnabled) Icons.Filled.VolumeUp else Icons.Filled.VolumeOff,
                        contentDescription = "كتم/تفعيل الصوت",
                        tint = if (isSoundEnabled) Color(0xFF34D399) else Color.White
                    )
                }
            }

            // Interactive Breathing Guide Text
            val breathingStateText = when {
                breathingProgress > 0.98f -> "احبس النَفَس بلطف ونقاء... 🧘"
                breathingProgress > 0.82f -> "شهيق عميق... املأ صدرك بالهدوء والنشاط 🍃"
                else -> "زفير مريح... تخلص من كل ضغوط الدراسة 🌊"
            }

            Text(
                text = breathingStateText,
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 16.dp)
            )

            // Dynamic Countdown and Ring Widget
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                // Background visual glowing ring matching breathing rhythm
                Box(
                    modifier = Modifier
                        .size(240.dp * breathingProgress)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF818CF8).copy(alpha = 0.03f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // Immersive circular countdown indicator
                CircularProgressIndicator(
                    progress = { if (totalSeconds > 0) secondsRemaining.toFloat() / totalSeconds.toFloat() else 0f },
                    modifier = Modifier.size(220.dp),
                    color = if (isBreak) Color(0xFF2DD4BF) else Color(0xFF6366F1),
                    strokeWidth = 6.dp,
                    trackColor = Color.White.copy(alpha = 0.05f)
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val formattedMinutes = String.format("%02d", secondsRemaining / 60)
                    val formattedSeconds = String.format("%02d", secondsRemaining % 60)

                    Text(
                        text = "$formattedMinutes:$formattedSeconds",
                        fontSize = 52.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (isBreak) "⏸️ استراحة قصيرة" else "🔥 جلسة عمل وتركيز عميق",
                        fontSize = 12.sp,
                        color = if (isBreak) Color(0xFF2DD4BF) else Color(0xFF818CF8),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Subject Tag & Wisdom Core
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (activeSubject.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White.copy(alpha = 0.06f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.MenuBook,
                                contentDescription = null,
                                tint = Color(0xFFFBBF24),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "أنت تدرس الآن: $activeSubject",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Randomizing quotes by minutes
                val comfortingQuotes = listOf(
                    "سر النجاح هو الحفاظ على ذهن صافٍ والتركيز على خطوتك القادمة فقط.",
                    "التركيز لا يعني قول نعم لشيء واحد، بل يعني قول لا للمئات من المشتتات.",
                    "جلسة دراسة هادئة بتركيز كامل تعدل أياماً من القراءة المتشتتة.",
                    "العقل الهادئ ينبثق منه الإبداع وسرعة الحفظ، استمر أنت تبسط الصعاب الآن.",
                    "كل دقيقة تقضيها بتركيز تفرز نتائج باهرة تفخر بها غداً.",
                    "التقدم البسيط المستمر هو ما يصنع الفارق الحقيقي، ثق بنفسك وبقدرتك."
                )
                val quoteIdx = if (totalSeconds > 0) (secondsRemaining / 60) % comfortingQuotes.size else 0
                val selectedQuote = comfortingQuotes[quoteIdx]

                Text(
                    text = "« $selectedQuote »",
                    color = Color.White.copy(alpha = 0.65f),
                    fontSize = 13.sp,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Integrated focus soundtrack changer
            if (isSoundEnabled) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.05f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.MusicNote, contentDescription = null, tint = Color(0xFF818CF8), modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("أصوات التركيز النشطة وسريعة التغيير:", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val allOptions = viewModel.builtInSounds + customSounds.map { it.first to "📂 " + it.second }
                            allOptions.forEach { (id, label) ->
                                val isActive = selectedSoundId == id
                                Surface(
                                    shape = RoundedCornerShape(50.dp),
                                    color = if (isActive) Color(0xFF6366F1) else Color.White.copy(alpha = 0.08f),
                                    border = BorderStroke(1.dp, if (isActive) Color(0xFF818CF8) else Color.White.copy(alpha = 0.1f)),
                                    modifier = Modifier.clickable { viewModel.selectSound(id) }
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        color = if (isActive) Color.White else Color.White.copy(alpha = 0.8f),
                                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Row: Play/Pause, Stop Study
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledIconButton(
                    onClick = {
                        if (isRunning) viewModel.pauseTimer() else viewModel.resumeTimer()
                    },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (isRunning) Color(0xFFD97706) else Color(0xFF10B981)
                    ),
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        imageVector = if (isRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = "تحكم بالمؤقت",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(24.dp))

                FilledIconButton(
                    onClick = { viewModel.resetTimer() },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = Color(0xFFEF4444)
                    ),
                    modifier = Modifier.size(46.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Stop,
                        contentDescription = "إنهاء جلسة المذكارة",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
