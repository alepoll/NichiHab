package com.nichihab.app.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nichihab.app.domain.model.SessionStatus
import com.nichihab.app.domain.model.Task
import com.nichihab.app.ui.theme.StatusLocked
import com.nichihab.app.ui.theme.StatusVerified
import com.nichihab.app.ui.theme.StatusFreeTime
import com.nichihab.app.ui.theme.StreakGold
import com.nichihab.app.ui.onboarding.NeonBlue
import com.nichihab.app.util.LocalStrings

import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Composable
fun HomeScreen(
    onNavigateToCamera: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToBlockedApps: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val s = LocalStrings.current
     val context = LocalContext.current
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var showBlockedAppsWarning by remember { mutableStateOf(false) }

    // Check for blocked apps on start
    LaunchedEffect(state.sessionStatus) {
        if (state.sessionStatus == SessionStatus.LOCKED) {
            val blocked = viewModel.getBlockedAppsCount()
            if (blocked == 0) {
                showBlockedAppsWarning = true
            }
        }
    }

    if (showBlockedAppsWarning) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showBlockedAppsWarning = false },
            title = { Text(s.get("no_apps_blocked_title"), color = MaterialTheme.colorScheme.error) },
            text = { Text(s.get("no_apps_blocked_desc")) },
            confirmButton = {
                Button(onClick = {
                    showBlockedAppsWarning = false
                    onNavigateToBlockedApps()
                }) {
                    Text(s.get("blocked_apps_manage"))
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showBlockedAppsWarning = false }) {
                    Text(s.get("cancel"))
                }
            }
        )
    }
    var editingTask by remember { mutableStateOf<Task?>(null) }

    var showPaywall by remember { mutableStateOf(false) }
    if (showPaywall) {
        com.nichihab.app.ui.commercial.PaywallScreen(onDismiss = { showPaywall = false })
    }

    if (showAddTaskDialog) {
        AddTaskDialog(
            onDismiss = { showAddTaskDialog = false },
            onShowPaywall = { showPaywall = true },
            onConfirm = { title, emoji, days, sH, sM, eH, eM ->
                viewModel.addTask(title, emoji, days, sH, sM, eH, eM)
                showAddTaskDialog = false
            },
            onSchedule = { title, emoji, date, days, sH, sM, eH, eM, isOneTimeTask ->
                viewModel.addScheduledTask(title, emoji, date, days, sH, sM, eH, eM, isOneTimeTask)
                showAddTaskDialog = false
            }
        )
    }

    editingTask?.let { task ->
        EditTaskDialog(
            task = task,
            onDismiss = { editingTask = null },
            onConfirm = { title, emoji, days, sH, sM, eH, eM ->
                viewModel.updateTask(task.id, title, emoji, days, sH, sM, eH, eM)
                editingTask = null
            },
            onDelete = {
                viewModel.deleteTask(task.id)
                editingTask = null
            }
        )
    }

    // Helper to evaluate editability
    val isTaskFullyEditable: (com.nichihab.app.domain.model.Task) -> Boolean = remember(state.sessionStatus) {
        { task ->
            if (task.isCompleted) false
            else {
                val todayDow = java.time.LocalDate.now().dayOfWeek.value
                val isScheduledForToday = task.scheduledDays.contains(todayDow) || task.isTodayOnly
                
                if (isScheduledForToday) {
                    val ageMillis = System.currentTimeMillis() - task.createdAt
                    // Puente Comercial: 2 Minutos de edición
                    val editWindowMinutes = if (com.nichihab.app.ui.commercial.CommercialSecurityConfig.ENABLE_2MIN_EDIT_WINDOW) 2 else 1
                    val passed1Min = ageMillis >= editWindowMinutes * 60 * 1000
                    val isActuallyInSession = state.sessionStatus == SessionStatus.LOCKED || state.sessionStatus == SessionStatus.UNLOCKED
                    !passed1Min && !isActuallyInSession
                } else {
                    true // Past or future days are always free to edit
                }
            }
        }
    }

    val statusColor by animateColorAsState(
        targetValue = when (state.sessionStatus) {
            SessionStatus.LOCKED -> StatusLocked
            SessionStatus.UNLOCKED -> StatusVerified.copy(alpha = 0.8f)
            SessionStatus.VERIFIED -> StatusVerified
            SessionStatus.FREE -> StatusFreeTime
        },
        label = "statusColor"
    )

    var currentTasks by remember(state.tasks) { mutableStateOf(state.tasks) }
    var draggingTaskIndex by remember { mutableStateOf<Int?>(null) }
    var draggingTaskOffset by remember { mutableStateOf(0f) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── Greeting + Streak ──
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(id = com.nichihab.app.R.drawable.logo),
                    contentDescription = "Logo",
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${s.get(state.greeting)}${state.userName} 👋",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = when (state.sessionStatus) {
                            SessionStatus.LOCKED -> s.get("status_complete_tasks")
                            SessionStatus.UNLOCKED -> s.get("status_unlock_active")
                            SessionStatus.VERIFIED -> s.get("status_all_complete")
                            SessionStatus.FREE -> s.get("status_no_session")
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                // Streak badge — show always, even at 0
                if (state.currentStreak >= 0) {
                    val isStreakBroken = state.currentStreak == 0
                    Card(
                        modifier = Modifier.graphicsLayer {
                            if (isStreakBroken) {
                                alpha = 0.5f
                            }
                        },
                        colors = CardDefaults.cardColors(
                            containerColor = StreakGold.copy(alpha = 0.15f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "\uD83D\uDD25", fontSize = 18.sp)  // 🔥
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${state.currentStreak}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = StreakGold
                            )
                        }
                    }
                }
            }
        }

        // ── Countdown Ring ──
        item {
            if (state.session != null) {
                when (state.sessionStatus) {
                    SessionStatus.LOCKED -> {
                        val activeTask = state.tasks.firstOrNull { !it.isCompleted }
                        CountdownRingSection(
                            startTimestamp = state.sessionStartTimestamp,
                            endTimestamp = state.sessionEndTimestamp,
                            statusColor = statusColor,
                            isTimerVisible = activeTask == null || activeTask.scheduledEndHour != null
                        )
                    }
                    SessionStatus.UNLOCKED -> {
                        UnlockActiveSection(
                            earnedSeconds = state.earnedSeconds,
                            usedSeconds = state.usedSeconds,
                            unlockStartedAt = state.unlockStartedAt,
                            onStop = { viewModel.stopUnlockTime() }
                        )
                    }
                    SessionStatus.VERIFIED -> {
                        VerifiedSection()
                    }
                    SessionStatus.FREE -> {
                        val nextStart = state.nextSessionStartTime
                        if (nextStart != null) {
                            UpcomingSessionSection(
                                startTime = nextStart,
                                onStartNow = { viewModel.startCurrentSessionNow() }
                            )
                        } else {
                            EmptyStateSection()
                        }
                    }
                }
            } else {
                EmptyStateSection()
            }
        }

        // ── Unlock Time Bank ──
        if (state.sessionStatus == SessionStatus.LOCKED && state.earnedSeconds > state.usedSeconds) {
            item {
                val availableSeconds = state.remainingUnlockSeconds
                val mins = availableSeconds / 60
                val secs = availableSeconds % 60
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = StatusVerified.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🎁", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = s.get("unlock_time_available"),
                                style = MaterialTheme.typography.titleSmall,
                                color = StatusVerified,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "%d:%02d".format(mins, secs),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = StatusVerified
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                viewModel.startUnlockTime()
                                val intent = android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
                                    addCategory(android.content.Intent.CATEGORY_HOME)
                                    flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(intent)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = StatusVerified),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.LockOpen, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(s.get("use_unlock_time"), fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // ── Weekly Calendar (Collapsible) ──
        item {
            var weeklyPlannerExpanded by remember { mutableStateOf(false) }
            val chevronRotation by androidx.compose.animation.core.animateFloatAsState(
                targetValue = if (weeklyPlannerExpanded) 90f else 0f,
                label = "chevron"
            )

            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            if (com.nichihab.app.ui.commercial.CommercialTierManager.canUseWeeklyCalendar()) {
                                weeklyPlannerExpanded = !weeklyPlannerExpanded
                            } else {
                                showPaywall = true
                            }
                        }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = s.get("weekly_calendar"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f)
                    )
                    if (!com.nichihab.app.ui.commercial.CommercialTierManager.canUseWeeklyCalendar()) {
                        Text(
                            text = "PRO",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = androidx.compose.ui.graphics.Color(0xFFFFA500),
                            modifier = Modifier
                                .background(androidx.compose.ui.graphics.Color(0xFFFFA500).copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = "▶",
                        modifier = Modifier.graphicsLayer { rotationZ = chevronRotation }.alpha(0.6f),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                AnimatedVisibility(
                    visible = weeklyPlannerExpanded,
                    enter = expandVertically() + androidx.compose.animation.fadeIn(),
                    exit = shrinkVertically() + androidx.compose.animation.fadeOut()
                ) {
                    WeeklyCalendarSection(
                        selectedDay = state.selectedCalendarDay,
                        todayDow = java.time.LocalDate.now().dayOfWeek.value,
                        tasks = state.allSessionTasks,
                        onDaySelected = { viewModel.selectCalendarDay(it) },
                        onTaskTapped = { 
                            if (isTaskFullyEditable(it)) {
                                editingTask = it 
                            }
                        }
                    )
                }
            }
        }

        // ── Today's Tasks Header ──
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = s.get("todays_tasks"),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    // Puente Comercial: Contador de tareas
                val currentCount = state.tasks.size
                val limit = com.nichihab.app.ui.commercial.CommercialTierManager.FREE_TIER_DAILY_TASK_LIMIT
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "$currentCount/$limit",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (currentCount >= limit) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .background(
                            if (currentCount >= limit) MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
                }
                TextButton(onClick = {
                    val todayTasks = state.tasks.size
                    if (com.nichihab.app.ui.commercial.CommercialTierManager.canAddTask(todayTasks)) {
                        showAddTaskDialog = true
                    } else {
                        showPaywall = true
                    }
                }) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(s.get("add_task"))
                }
            }
        }

        // ── Today's Tasks List ──
        if (currentTasks.isNotEmpty()) {
            items(currentTasks, key = { it.id }) { task ->
                val index = currentTasks.indexOf(task)
                val isDragging = draggingTaskIndex == index
                val offsetY = if (isDragging) draggingTaskOffset else 0f
                val zIndex = if (isDragging) 1f else 0f
                
                // Allow reorder only if session is free
                val canReorder = state.sessionStatus == SessionStatus.FREE

                TaskRow(
                    task = task,
                    modifier = Modifier
                        .animateItem()
                        .zIndex(zIndex)
                        .graphicsLayer { translationY = offsetY }
                        .then(
                            if (canReorder) {
                                Modifier.pointerInput(task) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = { draggingTaskIndex = index },
                                        onDragEnd = {
                                            draggingTaskIndex = null
                                            draggingTaskOffset = 0f
                                            viewModel.reorderTasks(currentTasks)
                                        },
                                        onDragCancel = {
                                            draggingTaskIndex = null
                                            draggingTaskOffset = 0f
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            draggingTaskOffset += dragAmount.y
                                            
                                            // Simple swap logic based on offset threshold
                                            val itemHeight = 80.dp.toPx()
                                            if (draggingTaskOffset > itemHeight && index < currentTasks.size - 1) {
                                                val newTasks = currentTasks.toMutableList()
                                                java.util.Collections.swap(newTasks, index, index + 1)
                                                currentTasks = newTasks
                                                draggingTaskIndex = index + 1
                                                draggingTaskOffset -= itemHeight
                                            } else if (draggingTaskOffset < -itemHeight && index > 0) {
                                                val newTasks = currentTasks.toMutableList()
                                                java.util.Collections.swap(newTasks, index, index - 1)
                                                currentTasks = newTasks
                                                draggingTaskIndex = index - 1
                                                draggingTaskOffset += itemHeight
                                            }
                                        }
                                    )
                                }
                            } else Modifier
                        ),
                    isEditable = isTaskFullyEditable(task),
                    onEdit = { editingTask = task },
                    onComplete = { viewModel.completeTask(task.id) }
                )
            }
        } else {
            item {
                Text(
                    text = s.get("no_tasks_today_hint") ?: "Your schedule is clear. Set a task to define your focus.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }
        }

        // ── CTA Button ──
        if (state.sessionStatus == SessionStatus.LOCKED || state.sessionStatus == SessionStatus.UNLOCKED) {
            item {
                // Puente Comercial: Botón siempre activo con API key global
                val hasGlobalKey = com.nichihab.app.ui.commercial.CommercialSecurityConfig.GLOBAL_API_KEY.isNotBlank()
                Button(
                    onClick = onNavigateToCamera,
                    enabled = hasGlobalKey,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = s.get("start_verification"),
                        style = MaterialTheme.typography.labelLarge,
                        fontSize = 16.sp
                    )
                }
            }
        }

        // ── Blocked Apps Quick Access ──
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToBlockedApps() },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(NeonBlue.copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("\uD83D\uDEAB", fontSize = 20.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = s.get("blocked_apps_card_title"),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = s.get("manage_blocked_apps_sub"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = s.get("blocked_apps_manage"),
                        style = MaterialTheme.typography.labelMedium,
                        color = NeonBlue,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // ── Status Banner ──
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = statusColor.copy(alpha = 0.12f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = when (state.sessionStatus) {
                            SessionStatus.LOCKED -> Icons.Default.Lock
                            SessionStatus.UNLOCKED -> Icons.Default.LockOpen
                            SessionStatus.VERIFIED -> Icons.Default.LockOpen
                            SessionStatus.FREE -> Icons.Default.Schedule
                        },
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when (state.sessionStatus) {
                            SessionStatus.LOCKED -> s.get("status_locked")
                            SessionStatus.UNLOCKED -> s.get("status_unlocked")
                            SessionStatus.VERIFIED -> s.get("status_verified")
                            SessionStatus.FREE -> s.get("status_free")
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = statusColor
                    )
                }
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun AddTaskDialog(
    onDismiss: () -> Unit,
    onShowPaywall: () -> Unit = {},
    onConfirm: (String, String, List<Int>, Int?, Int?, Int?, Int?) -> Unit,
    onSchedule: (String, String, String, List<Int>, Int?, Int?, Int?, Int?, Boolean) -> Unit = { _, _, _, _, _, _, _, _, _ -> }
) {
    var title by remember { mutableStateOf("") }
    var emoji by remember { mutableStateOf("✅") }
    // Days: 1=Mon..7=Sun, all selected by default
    var selectedDays by remember { mutableStateOf(listOf(1, 2, 3, 4, 5, 6, 7)) }
    var useCustomTime by remember { mutableStateOf(false) }
    var startHour by remember { mutableStateOf(9) }
    var startMinute by remember { mutableStateOf(0) }
    var endHour by remember { mutableStateOf(10) }
    var endMinute by remember { mutableStateOf(0) }
    // Scheduling
    var scheduleForLater by remember { mutableStateOf(false) }
    var selectedYear by remember { mutableStateOf(java.time.LocalDate.now().year) }
    var selectedMonth by remember { mutableStateOf(java.time.LocalDate.now().monthValue) }
    var selectedDay by remember { mutableStateOf(java.time.LocalDate.now().dayOfMonth + 1) }
    // Today-only toggle
    var isOneTimeTask by remember { mutableStateOf(false) }

    val s = LocalStrings.current
    val dayLabels = listOf(
        s.get("day_mon_short"), s.get("day_tue_short"), s.get("day_wed_short"),
        s.get("day_thu_short"), s.get("day_fri_short"), s.get("day_sat_short"), s.get("day_sun_short")
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(s.get("add_task_title")) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                // Task description
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = com.nichihab.app.ui.commercial.InputGuard.sanitizeTaskDescription(it) },
                    label = { Text(s.get("task_description")) },
                    placeholder = { Text(s.get("task_placeholder")) },
                    modifier = Modifier.fillMaxWidth(),
                    isError = com.nichihab.app.ui.commercial.InputGuard.containsInjectionPattern(title)
                )

                // Emoji picker
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(s.get("emoji_label"), style = MaterialTheme.typography.bodyMedium)
                    listOf("✅", "📖", "🧘", "💪", "🧹", "📝").forEach { e ->
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    if (emoji == e) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { emoji = e },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(e)
                        }
                    }
                }

                // ── One-time task toggle ──
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isOneTimeTask = !isOneTimeTask }
                        .background(
                            if (isOneTimeTask) NeonBlue.copy(alpha = 0.08f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            RoundedCornerShape(10.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("⚡", fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = s.get("today_only_toggle"),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = if (isOneTimeTask) "ON" else "OFF",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isOneTimeTask) NeonBlue
                        else MaterialTheme.colorScheme.outline
                    )
                }

                // Day-of-week picker (hidden when one-time task)
                if (!isOneTimeTask) {
                Text(
                    text = s.get("schedule_days"),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    dayLabels.forEachIndexed { index, label ->
                        val day = index + 1 // 1=Mon..7=Sun
                        val isSelected = day in selectedDays
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    selectedDays = if (isSelected) {
                                        selectedDays - day
                                    } else {
                                        selectedDays + day
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                } // End if (!isOneTimeTask)

                // Time picker toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { useCustomTime = !useCustomTime }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = s.get("set_time"),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = if (useCustomTime) "ON" else "OFF",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (useCustomTime) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline
                    )
                }

                // Time pickers (shown when toggled on)
                if (useCustomTime) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Start time
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = s.get("start_time"),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            TimePickerCompact(
                                hour = startHour,
                                minute = startMinute,
                                onHourChange = { startHour = it },
                                onMinuteChange = { startMinute = it }
                            )
                        }

                        Text(
                            text = "→",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // End time
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = s.get("end_time"),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            TimePickerCompact(
                                hour = endHour,
                                minute = endMinute,
                                onHourChange = { endHour = it },
                                onMinuteChange = { endMinute = it }
                            )
                        }
                    }
                }

                // ── Schedule for later (Premium only) ──
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (com.nichihab.app.ui.commercial.CommercialTierManager.canScheduleForLater()) {
                                scheduleForLater = !scheduleForLater
                            } else {
                                onShowPaywall()
                            }
                        }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = s.get("schedule_for_later"),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    if (!com.nichihab.app.ui.commercial.CommercialTierManager.canScheduleForLater()) {
                        Text(
                            text = "PRO",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = androidx.compose.ui.graphics.Color(0xFFFFA500),
                            modifier = Modifier
                                .background(androidx.compose.ui.graphics.Color(0xFFFFA500).copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = if (scheduleForLater) "ON" else "OFF",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (scheduleForLater) MaterialTheme.colorScheme.secondary
                        else MaterialTheme.colorScheme.outline
                    )
                }

                if (scheduleForLater) {
                    // Simple date picker with steppers
                    val maxDay = try {
                        java.time.YearMonth.of(selectedYear, selectedMonth).lengthOfMonth()
                    } catch (_: Exception) { 28 }
                    if (selectedDay > maxDay) selectedDay = maxDay

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Year stepper
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("▲",
                                modifier = Modifier.clickable { selectedYear++ },
                                color = MaterialTheme.colorScheme.secondary, fontSize = 14.sp)
                            Text("%04d".format(selectedYear),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold)
                            Text("▼",
                                modifier = Modifier.clickable {
                                    if (selectedYear > java.time.LocalDate.now().year) selectedYear--
                                },
                                color = MaterialTheme.colorScheme.secondary, fontSize = 14.sp)
                        }
                        Text("/", style = MaterialTheme.typography.titleMedium)
                        // Month stepper
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("▲",
                                modifier = Modifier.clickable {
                                    selectedMonth = if (selectedMonth < 12) selectedMonth + 1 else 1
                                },
                                color = MaterialTheme.colorScheme.secondary, fontSize = 14.sp)
                            Text("%02d".format(selectedMonth),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold)
                            Text("▼",
                                modifier = Modifier.clickable {
                                    selectedMonth = if (selectedMonth > 1) selectedMonth - 1 else 12
                                },
                                color = MaterialTheme.colorScheme.secondary, fontSize = 14.sp)
                        }
                        Text("/", style = MaterialTheme.typography.titleMedium)
                        // Day stepper
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("▲",
                                modifier = Modifier.clickable {
                                    selectedDay = if (selectedDay < maxDay) selectedDay + 1 else 1
                                },
                                color = MaterialTheme.colorScheme.secondary, fontSize = 14.sp)
                            Text("%02d".format(selectedDay),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold)
                            Text("▼",
                                modifier = Modifier.clickable {
                                    selectedDay = if (selectedDay > 1) selectedDay - 1 else maxDay
                                },
                                color = MaterialTheme.colorScheme.secondary, fontSize = 14.sp)
                        }
                    }

                    // Show formatted date
                    val dateStr = "%04d-%02d-%02d".format(selectedYear, selectedMonth, selectedDay)
                    Text(
                        text = s.get("scheduled_for").replace("%s", dateStr),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        if (isOneTimeTask) {
                            val targetDate = if (scheduleForLater) {
                                "%04d-%02d-%02d".format(selectedYear, selectedMonth, selectedDay)
                            } else {
                                java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
                            }
                            val targetDow = try {
                                java.time.LocalDate.parse(targetDate).dayOfWeek.value
                            } catch (e: Exception) {
                                java.time.LocalDate.now().dayOfWeek.value
                            }
                            onSchedule(
                                title, emoji, targetDate, listOf(targetDow),
                                if (useCustomTime) startHour else null,
                                if (useCustomTime) startMinute else null,
                                if (useCustomTime) endHour else null,
                                if (useCustomTime) endMinute else null,
                                true // isOneTimeTask
                            )
                        } else {
                            if (scheduleForLater) {
                                val dateStr = "%04d-%02d-%02d".format(selectedYear, selectedMonth, selectedDay)
                                onSchedule(
                                    title, emoji, dateStr, selectedDays.sorted(),
                                    if (useCustomTime) startHour else null,
                                    if (useCustomTime) startMinute else null,
                                    if (useCustomTime) endHour else null,
                                    if (useCustomTime) endMinute else null,
                                    false
                                )
                            } else if (selectedDays.isNotEmpty()) {
                                onConfirm(
                                    title, emoji, selectedDays.sorted(),
                                    if (useCustomTime) startHour else null,
                                    if (useCustomTime) startMinute else null,
                                    if (useCustomTime) endHour else null,
                                    if (useCustomTime) endMinute else null
                                )
                            }
                        }
                    }
                },
                enabled = title.isNotBlank() && (isOneTimeTask || selectedDays.isNotEmpty())
            ) {
                Text(if (isOneTimeTask && !scheduleForLater) s.get("add") else if (scheduleForLater) s.get("save") else s.get("add"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(s.get("cancel"))
            }
        }
    )
}

@Composable
private fun TimePickerCompact(
    hour: Int,
    minute: Int,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        // Hour stepper
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "▲",
                modifier = Modifier.clickable { onHourChange((hour + 1) % 24) },
                color = MaterialTheme.colorScheme.primary,
                fontSize = 14.sp
            )
            Text(
                text = "%02d".format(hour),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "▼",
                modifier = Modifier.clickable { onHourChange(if (hour > 0) hour - 1 else 23) },
                color = MaterialTheme.colorScheme.primary,
                fontSize = 14.sp
            )
        }
        Text(
            text = ":",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        // Minute stepper (steps of 5)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "▲",
                modifier = Modifier.clickable { onMinuteChange((minute + 5) % 60) },
                color = MaterialTheme.colorScheme.primary,
                fontSize = 14.sp
            )
            Text(
                text = "%02d".format(minute),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "▼",
                modifier = Modifier.clickable { onMinuteChange(if (minute >= 5) minute - 5 else 55) },
                color = MaterialTheme.colorScheme.primary,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun CountdownRingSection(
    startTimestamp: Long, 
    endTimestamp: Long, 
    statusColor: androidx.compose.ui.graphics.Color,
    isTimerVisible: Boolean = true
) {
    var secondsRemaining by remember { androidx.compose.runtime.mutableStateOf(0L) }

    androidx.compose.runtime.LaunchedEffect(startTimestamp, endTimestamp, isTimerVisible) {
        if (isTimerVisible) {
            while (true) {
                val now = System.currentTimeMillis()
                secondsRemaining = ((endTimestamp - now) / 1000).coerceAtLeast(0)
                kotlinx.coroutines.delay(1000)
            }
        }
    }

    val hours = secondsRemaining / 3600
    val minutes = (secondsRemaining % 3600) / 60
    val secs = secondsRemaining % 60
    val timeString = "%d:%02d:%02d".format(hours, minutes, secs)
    
    val s = LocalStrings.current
    val titleText = if (isTimerVisible) s.get("active_focus_control") else s.get("blocked_apps_title")
    val subtitleText = if (isTimerVisible) {
        "$timeString ${s.get("remaining")}"
    } else {
        s.get("waiting_for_verification")
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                    )
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = titleText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitleText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun UnlockActiveSection(earnedSeconds: Long, usedSeconds: Long, unlockStartedAt: Long?, onStop: () -> Unit) {
    var remainingSeconds by remember { androidx.compose.runtime.mutableStateOf((earnedSeconds - usedSeconds).coerceAtLeast(0)) }

    androidx.compose.runtime.LaunchedEffect(earnedSeconds, usedSeconds, unlockStartedAt) {
        if (unlockStartedAt != null) {
            while (true) {
                val elapsedSinceStart = (System.currentTimeMillis() - unlockStartedAt) / 1000
                val totalUsed = usedSeconds + elapsedSinceStart
                remainingSeconds = (earnedSeconds - totalUsed).coerceAtLeast(0)
                if (remainingSeconds <= 0) break
                kotlinx.coroutines.delay(1000)
            }
        } else {
            remainingSeconds = (earnedSeconds - usedSeconds).coerceAtLeast(0)
        }
    }

    val pulse = rememberInfiniteTransition(label = "unlockPulse")
    val scale by pulse.animateFloat(1f, 1.03f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "uScale")
    val mins = remainingSeconds / 60
    val secs = remainingSeconds % 60

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(160.dp)
                .scale(scale)
                .background(
                    Brush.radialGradient(
                        colors = listOf(StatusVerified.copy(alpha = 0.25f), StatusVerified.copy(alpha = 0.05f))
                    ),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(36.dp), tint = StatusVerified)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "%d:%02d".format(mins, secs),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = StatusVerified
                )
                Text(
                    text = LocalStrings.current.get("unlock_remaining"),
                    style = MaterialTheme.typography.bodySmall,
                    color = StatusVerified.copy(alpha = 0.7f)
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        androidx.compose.material3.OutlinedButton(
            onClick = onStop,
            modifier = Modifier.height(40.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Lock, null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(LocalStrings.current.get("stop_relock"))
        }
    }
}

@Composable
private fun VerifiedSection() {
    val pulse = rememberInfiniteTransition(label = "vPulse")
    val scale by pulse.animateFloat(1f, 1.05f, infiniteRepeatable(tween(1200), RepeatMode.Reverse), label = "vScale")

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.size(120.dp).scale(scale).background(StatusVerified.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Check, contentDescription = "Verified", modifier = Modifier.size(56.dp), tint = StatusVerified)
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(LocalStrings.current.get("all_tasks_verified"), style = MaterialTheme.typography.titleLarge, color = StatusVerified)
    }
}

@Composable
private fun UpcomingSessionSection(startTime: String, onStartNow: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.size(120.dp).background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(LocalStrings.current.get("next_session_at").replace("%s", startTime), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
        Text(LocalStrings.current.get("get_ready"), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = onStartNow,
            modifier = Modifier.height(48.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Lock, null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(LocalStrings.current.get("start_now"))
        }
    }
}

@Composable
private fun EmptyStateSection() {
    val pulse = rememberInfiniteTransition(label = "ePulse")
    val alpha by pulse.animateFloat(0.3f, 0.7f, infiniteRepeatable(tween(1500, easing = LinearEasing), RepeatMode.Reverse), label = "eAlpha")

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.size(100.dp).alpha(alpha).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = LocalStrings.current.get("no_active_session"),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = LocalStrings.current.get("session_appears_here"),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TaskRow(task: Task, modifier: Modifier = Modifier, isEditable: Boolean = false, onEdit: () -> Unit = {}, onComplete: () -> Unit = {}) {
    val s = LocalStrings.current
    val dayLabels = listOf(
        s.get("day_mon_short"), s.get("day_tue_short"), s.get("day_wed_short"),
        s.get("day_thu_short"), s.get("day_fri_short"), s.get("day_sat_short"), s.get("day_sun_short")
    )
    val allDays = listOf(1, 2, 3, 4, 5, 6, 7)
    val hasCustomDays = task.scheduledDays.sorted() != allDays
    val hasCustomTime = task.scheduledStartHour != null

    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (isEditable) Modifier.combinedClickable(
                onClick = {}, // No action on simple tap to avoid accidental opening
                onLongClick = { onEdit() },
                onLongClickLabel = s.get("edit_task")
            ) else Modifier),
        colors = CardDefaults.cardColors(
            containerColor = if (task.isCompleted) StatusVerified.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceContainer
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text = task.emoji, fontSize = 24.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title, style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                // Show schedule subtitle if non-default
                if (task.isTodayOnly) {
                    Text(
                        text = "⚡ ${s.get("today_only_label")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = com.nichihab.app.ui.onboarding.NeonBlue
                    )
                } else if (hasCustomDays || hasCustomTime) {
                    val parts = mutableListOf<String>()
                    if (hasCustomDays) {
                        parts.add(task.scheduledDays.map { dayLabels.getOrElse(it - 1) { "?" } }.joinToString(""))
                    }
                    if (hasCustomTime) {
                        parts.add("%02d:%02d–%02d:%02d".format(
                            task.scheduledStartHour ?: 0, task.scheduledStartMinute ?: 0,
                            task.scheduledEndHour ?: 0, task.scheduledEndMinute ?: 0
                        ))
                    }
                    Text(
                        text = parts.joinToString(" • "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                // Show reward minutes
                if (!task.isCompleted) {
                    Text(
                        text = "🎁 ${task.rewardMinutes} min",
                        style = MaterialTheme.typography.labelSmall,
                        color = StatusVerified
                    )
                }
            }
            if (task.isCompleted) {
                AnimatedCompletableCheckmark(
                    isCompleted = true,
                    onComplete = {}
                )
            } else {
                Text("⏳", fontSize = 16.sp)
            }
        }
    }
}

@Composable
private fun AnimatedCompletableCheckmark(
    isCompleted: Boolean,
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    val s = LocalStrings.current
    val vibrator = androidx.compose.runtime.remember { context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? android.os.Vibrator }
    var isAnimating by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isAnimating) 1.3f else 1f,
        animationSpec = if (isAnimating) androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioHighBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
        ) else androidx.compose.animation.core.spring(),
        label = "bounce"
    )

    val ringAlpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isAnimating) 0f else 1f,
        animationSpec = tween(300, easing = LinearEasing),
        label = "ringAlpha"
    )
    val ringScale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isAnimating) 2.5f else 1f,
        animationSpec = tween(300, easing = androidx.compose.animation.core.LinearOutSlowInEasing),
        label = "ringScale"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(32.dp)
            .clickable(
                interactionSource = androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null
            ) {
                if (!isCompleted && !isAnimating) {
                    try {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                            vibrator?.vibrate(android.os.VibrationEffect.createOneShot(100, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
                        } else {
                            @Suppress("DEPRECATION")
                            vibrator?.vibrate(100)
                        }
                    } catch (e: Exception) {
                        // Proper error handling/default fallback for vibrator exceptions
                    }
                    isAnimating = true
                }
            }
    ) {
        if (isAnimating) {
            Canvas(modifier = Modifier.size(24.dp).graphicsLayer {
                scaleX = ringScale
                scaleY = ringScale
                alpha = ringAlpha
            }) {
                drawCircle(
                    color = androidx.compose.ui.graphics.Color(0xFFFFD700),
                    style = Stroke(width = 4.dp.toPx() * ringAlpha)
                )
            }
        }

        Box(
            modifier = Modifier
                .size(24.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .background(
                    if (isCompleted || isAnimating) StatusVerified else androidx.compose.ui.graphics.Color.Transparent,
                    CircleShape
                )
                .border(
                    width = 2.dp,
                    color = if (isCompleted || isAnimating) StatusVerified else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isCompleted || isAnimating) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = s.get("done"),
                    tint = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }

    androidx.compose.runtime.LaunchedEffect(isAnimating) {
        if (isAnimating) {
            kotlinx.coroutines.delay(300)
            isAnimating = false
            onComplete()
        }
    }
}

@Composable
private fun EditTaskDialog(
    task: Task,
    onDismiss: () -> Unit,
    onConfirm: (String, String, List<Int>, Int?, Int?, Int?, Int?) -> Unit,
    onDelete: () -> Unit
) {
    var title by remember { mutableStateOf(task.title) }
    var emoji by remember { mutableStateOf(task.emoji) }
    var selectedDays by remember { mutableStateOf(task.scheduledDays) }
    var useCustomTime by remember { mutableStateOf(task.scheduledStartHour != null) }
    var startHour by remember { mutableStateOf(task.scheduledStartHour ?: 9) }
    var startMinute by remember { mutableStateOf(task.scheduledStartMinute ?: 0) }
    var endHour by remember { mutableStateOf(task.scheduledEndHour ?: 10) }
    var endMinute by remember { mutableStateOf(task.scheduledEndMinute ?: 0) }

    val s = LocalStrings.current
    val dayLabels = listOf(
        s.get("day_mon_short"), s.get("day_tue_short"), s.get("day_wed_short"),
        s.get("day_thu_short"), s.get("day_fri_short"), s.get("day_sat_short"), s.get("day_sun_short")
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(s.get("edit_task_title")) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = title, onValueChange = { title = it },
                    label = { Text(s.get("task_description")) },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(s.get("emoji_label"), style = MaterialTheme.typography.bodyMedium)
                    listOf("✅", "📖", "🧘", "💪", "🧹", "📝").forEach { e ->
                        Box(
                            modifier = Modifier.size(36.dp).background(
                                if (emoji == e) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(8.dp)
                            ).clickable { emoji = e },
                            contentAlignment = Alignment.Center
                        ) { Text(e) }
                    }
                }
                Text(s.get("schedule_days"), style = MaterialTheme.typography.labelLarge)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    dayLabels.forEachIndexed { index, label ->
                        val day = index + 1
                        val isSelected = day in selectedDays
                        Box(
                            modifier = Modifier.weight(1f).height(36.dp).background(
                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(8.dp)
                            ).clickable { selectedDays = if (isSelected) selectedDays - day else selectedDays + day },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { useCustomTime = !useCustomTime }.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Schedule, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(s.get("set_time"), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Text(if (useCustomTime) "ON" else "OFF", style = MaterialTheme.typography.labelSmall,
                        color = if (useCustomTime) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
                }
                if (useCustomTime) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(s.get("start_time"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            TimePickerCompact(startHour, startMinute, { startHour = it }, { startMinute = it })
                        }
                        Text("→", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(s.get("end_time"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            TimePickerCompact(endHour, endMinute, { endHour = it }, { endMinute = it })
                        }
                    }
                }
                // Delete button
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onDelete,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Delete, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(s.get("delete"))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && selectedDays.isNotEmpty()) {
                        onConfirm(title, emoji, selectedDays.sorted(),
                            if (useCustomTime) startHour else null, if (useCustomTime) startMinute else null,
                            if (useCustomTime) endHour else null, if (useCustomTime) endMinute else null)
                    }
                },
                enabled = title.isNotBlank() && selectedDays.isNotEmpty()
            ) { Text(s.get("save_key")) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(s.get("cancel")) } }
    )
}
