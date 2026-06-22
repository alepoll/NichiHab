package com.nichihab.app.ui.home

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nichihab.app.data.local.datastore.SettingsDataStore
import com.nichihab.app.data.local.db.dao.BlockedAppDao
import com.nichihab.app.domain.model.Session
import com.nichihab.app.domain.model.SessionStatus
import com.nichihab.app.domain.model.Task
import com.nichihab.app.domain.repository.SessionRepository
import com.nichihab.app.domain.repository.TaskRepository
import com.nichihab.app.domain.repository.FirebaseUserRepository
import com.nichihab.app.service.worker.NotificationScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import android.widget.Toast
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class HomeUiState(
    val userName: String = "",
    val greeting: String = "Good morning",
    val currentStreak: Int = 0,
    val session: Session? = null,
    val nextSessionStartTime: String? = null,
    val tasks: List<Task> = emptyList(),         // Filtered to selected calendar day
    val allSessionTasks: List<Task> = emptyList(), // All tasks for this session (for calendar)
    val sessionStatus: SessionStatus = SessionStatus.FREE,
    val sessionStartTimestamp: Long = 0,
    val sessionEndTimestamp: Long = 0,
    val isLoading: Boolean = true,
    // Earned unlock time
    val earnedSeconds: Long = 0,
    val usedSeconds: Long = 0,
    val unlockStartedAt: Long? = null,
    val remainingUnlockSeconds: Long = 0,
    val isUnlockActive: Boolean = false,
    val allTasksComplete: Boolean = false,
    // Task editing
    val isTaskEditable: Boolean = false,
    // Weekly calendar
    val selectedCalendarDay: Int = java.time.LocalDate.now().dayOfWeek.value
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val taskRepository: TaskRepository,
    private val settingsDataStore: SettingsDataStore,
    private val blockedAppDao: BlockedAppDao,
    private val firebaseUserRepository: FirebaseUserRepository,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    companion object {
        const val ACTION_SESSION_LOCKED = "com.nichihab.app.ACTION_SESSION_LOCKED"
    }

    private val logicalTodayFlow = flow {
        while (true) {
            val latest = sessionRepository.getLatestSession()
            emit(com.nichihab.app.util.TimeUtil.getLogicalDateDomain(latest))
            delay(60_000)
        }
    }.distinctUntilChanged()
     .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LocalDate.now().toString())

    private val sessionFlow = logicalTodayFlow
        .flatMapLatest { logicalDate ->
            sessionRepository.observeSessionForDate(logicalDate)
        }

    private val tasksFlow = sessionFlow.flatMapLatest { session ->
        if (session != null) {
            taskRepository.getTasksForSession(session.id).map { tasks ->
                val latestSession = sessionRepository.getLatestSession()
                val logicalTodayStr = com.nichihab.app.util.TimeUtil.getLogicalDateDomain(latestSession)
                tasks.filter { task ->
                    // Filter out expired today-only tasks (created on a past date)
                    !(task.isTodayOnly && task.createdDate != null && task.createdDate != logicalTodayStr)
                }
            }
        } else {
            flowOf(emptyList())
        }
    }

    private val _streak = MutableStateFlow(0)
    private val _selectedCalendarDay = MutableStateFlow(LocalDate.now().dayOfWeek.value)
    private var lastEnsuredDate: String = ""

    val uiState: StateFlow<HomeUiState> = combine(
        sessionFlow,
        tasksFlow,
        settingsDataStore.userName,
        logicalTodayFlow,
        _streak,
        _selectedCalendarDay
    ) { args: Array<Any?> ->
        val session = args[0] as Session?
        val tasks = args[1] as List<Task>
        val userName = args[2] as String
        val logicalToday = args[3] as String
        val streak = args[4] as Int
        val selectedDay = args[5] as Int

        val currentTimeMs = System.currentTimeMillis()
        val now = LocalTime.now()
        val logicalDow = LocalDate.parse(logicalToday).dayOfWeek.value
        val displayTasks = if (selectedDay == logicalDow) {
            tasks
        } else {
            tasks.filter { it.scheduledDays.contains(selectedDay) || it.isTodayOnly }
        }
        val greeting = when (now.hour) {
            in 5..11 -> "greeting_morning"
            in 12..16 -> "greeting_afternoon"
            in 17..20 -> "greeting_evening"
            else -> "greeting_night"
        }

        var nextStartTime: String? = null
        var sessionStartTimestamp = 0L
        var sessionEndTimestamp = 0L
        var earnedSeconds = 0L
        var usedSeconds = 0L
        var unlockStartedAt: Long? = null
        var editGracePeriodEndsAt: Long? = null
        var remainingUnlockSeconds = 0L
        var isUnlockActive = false
        val allTasksComplete = tasks.isNotEmpty() && tasks.all { it.isCompleted }

        var isTaskEditable = false

        val status = if (session != null) {
            editGracePeriodEndsAt = session.editGracePeriodEndsAt
            earnedSeconds = session.earnedSeconds
            usedSeconds = session.usedSeconds
            unlockStartedAt = session.unlockStartedAt

            if (session.unlockStartedAt != null) {
                val elapsedSinceStart = (currentTimeMs - session.unlockStartedAt) / 1000
                val totalUsed = usedSeconds + elapsedSinceStart
                remainingUnlockSeconds = (earnedSeconds - totalUsed).coerceAtLeast(0)
                isUnlockActive = true
            } else {
                remainingUnlockSeconds = (earnedSeconds - usedSeconds).coerceAtLeast(0)
                isUnlockActive = session.status == SessionStatus.UNLOCKED
            }

            val nowSec = now.toSecondOfDay()
            val startSec = session.startTimeHour * 3600 + session.startTimeMinute * 60
            val endSec = session.endTimeHour * 3600 + session.endTimeMinute * 60

            val isInsideWindow = if (tasks.isEmpty()) {
                false // If NO tasks exist, don't block even if in window
            } else if (endSec > startSec) {
                // Normal window: start < end (e.g. 9:00 -> 11:00)
                nowSec >= startSec && nowSec < endSec
            } else if (endSec < startSec) {
                // Cross-midnight: end < start (e.g. 22:00 -> 06:00)
                nowSec >= startSec || nowSec < endSec
            } else {
                session.status == SessionStatus.LOCKED || session.status == SessionStatus.UNLOCKED
            }

            val isUpcoming = if (endSec > startSec) {
                nowSec < startSec
            } else if (endSec < startSec) {
                nowSec < startSec && nowSec >= endSec
            } else false

            if (isUpcoming) {
                nextStartTime = "%02d:%02d".format(session.startTimeHour, session.startTimeMinute)
            }

            val nowMs = System.currentTimeMillis()
            val gracePeriodRemaining = if (session.editGracePeriodEndsAt != null) (session.editGracePeriodEndsAt - nowMs).coerceAtLeast(0) else 0L
            isTaskEditable = !isInsideWindow || allTasksComplete || gracePeriodRemaining > 0

            if (isInsideWindow) {
                val logicalDayStartMs = LocalDate.parse(logicalToday)
                    .atStartOfDay(java.time.ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()
                val stTime = LocalTime.of(session.startTimeHour, session.startTimeMinute)
                val enTime = LocalTime.of(session.endTimeHour, session.endTimeMinute)
                sessionStartTimestamp = logicalDayStartMs + stTime.toSecondOfDay() * 1000L
                sessionEndTimestamp = logicalDayStartMs + enTime.toSecondOfDay() * 1000L
                if (enTime.isBefore(stTime)) {
                    sessionEndTimestamp += 24 * 60 * 60 * 1000L
                }
            }

            when {
                session.status == SessionStatus.VERIFIED || allTasksComplete -> SessionStatus.VERIFIED
                isUnlockActive && remainingUnlockSeconds > 0 -> SessionStatus.UNLOCKED
                isInsideWindow -> SessionStatus.LOCKED
                else -> SessionStatus.FREE
            }
        } else SessionStatus.FREE

        HomeUiState(
            userName = userName.ifBlank { null }?.let { ", $it" } ?: "",
            greeting = greeting,
            currentStreak = streak,
            session = session,
            nextSessionStartTime = nextStartTime,
            tasks = displayTasks,
            allSessionTasks = tasks,
            sessionStatus = status,
            sessionStartTimestamp = sessionStartTimestamp,
            sessionEndTimestamp = sessionEndTimestamp,
            isLoading = false,
            earnedSeconds = earnedSeconds,
            usedSeconds = usedSeconds,
            unlockStartedAt = unlockStartedAt,
            remainingUnlockSeconds = remainingUnlockSeconds,
            isUnlockActive = isUnlockActive,
            allTasksComplete = allTasksComplete,
            isTaskEditable = isTaskEditable,
            selectedCalendarDay = selectedDay
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )

    init {
        viewModelScope.launch {
            logicalTodayFlow.collect { logicalToday ->
                if (logicalToday != lastEnsuredDate) {
                    lastEnsuredDate = logicalToday
                    ensureSessionForDate(logicalToday)
                }
            }
        }
        calculateStreak()
    }

    private fun calculateStreak() {
        viewModelScope.launch {
            try {
                val todayDate = LocalDate.now()
                val startDate = todayDate.minusDays(60)
                val sessions = sessionRepository.getSessionsInRange(
                    startDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
                    todayDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
                )
                val verifiedDates = sessions
                    .filter { it.status == SessionStatus.VERIFIED }
                    .map { it.date }
                    .toSet()
                var streak = 0
                var checkDate = todayDate
                
                if (verifiedDates.contains(checkDate.format(DateTimeFormatter.ISO_LOCAL_DATE))) {
                    streak++
                    checkDate = checkDate.minusDays(1)
                } else {
                    checkDate = checkDate.minusDays(1)
                }

                while (true) {
                    val dateStr = checkDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
                    if (verifiedDates.contains(dateStr)) {
                        streak++
                        checkDate = checkDate.minusDays(1)
                    } else break
                }
                _streak.value = streak
            } catch (_: Exception) { }
        }
    }

    private fun ensureSessionForDate(dateStr: String) {
        viewModelScope.launch {
            try {
                val session = sessionRepository.getSessionForDate(dateStr)
                if (session == null) {
                    val latest = sessionRepository.getLatestSession()
                    if (latest != null && latest.isRepeating) {
                        val newSession = latest.copy(
                            id = 0, date = dateStr,
                            status = SessionStatus.LOCKED,
                            tasks = emptyList(),
                            earnedSeconds = 0, usedSeconds = 0, unlockStartedAt = null,
                            editGracePeriodEndsAt = System.currentTimeMillis() + 60 * 1000L
                        )
                        val newId = sessionRepository.insert(newSession)
                        val oldTasks = taskRepository.getTasksForSessionOnce(latest.id)
                        taskRepository.insertAll(oldTasks.map {
                            it.copy(id = 0, sessionId = newId, isCompleted = false, completedAt = null)
                        })
                        
                        val createdSession = sessionRepository.getSessionForDate(dateStr)
                        if (createdSession != null) {
                            NotificationScheduler.scheduleSessionNotifications(
                                appContext, 
                                createdSession.id, 
                                createdSession.date, 
                                createdSession.startTimeHour, 
                                createdSession.startTimeMinute
                            )
                        }
                    }
                } else if (session.editGracePeriodEndsAt == null) {
                    // Session was auto-created by background worker.
                    // Grant the 1-time daily 5-minute grace period upon first app open.
                    sessionRepository.setEditGracePeriod(session.id, System.currentTimeMillis() + 60 * 1000L)
                }
            } catch (_: Exception) { }
        }
    }

    fun addTask(
        title: String,
        emoji: String = "✅",
        scheduledDays: List<Int> = listOf(1, 2, 3, 4, 5, 6, 7),
        startHour: Int? = null,
        startMinute: Int? = null,
        endHour: Int? = null,
        endMinute: Int? = null
    ) {
        viewModelScope.launch {
            // FIREBASE LIMIT CHECK
            val isPro = com.nichihab.app.ui.commercial.CommercialTierManager.isProUser
            val limit = com.nichihab.app.ui.commercial.CommercialTierManager.FREE_TIER_DAILY_TASK_LIMIT
            val canAdd = firebaseUserRepository.canAddTask(isPro, limit)
            if (!canAdd) {
                Toast.makeText(appContext, "Daily task limit reached. Please upgrade to Pro.", Toast.LENGTH_LONG).show()
                return@launch
            }

            val latestSession = sessionRepository.getLatestSession()
            val logicalToday = com.nichihab.app.util.TimeUtil.getLogicalDateDomain(latestSession)
            var session = sessionRepository.getSessionForDate(logicalToday)
            
            if (session == null) {
                if (latestSession != null) {
                    val newSession = latestSession.copy(
                        id = 0, date = logicalToday,
                        status = SessionStatus.LOCKED,
                        tasks = emptyList(),
                        earnedSeconds = 0, usedSeconds = 0, unlockStartedAt = null
                    )
                    sessionRepository.insert(newSession)
                } else {
                    val defaultSession = Session(
                        date = logicalToday,
                        startTimeHour = 9, startTimeMinute = 0,
                        endTimeHour = 17, endTimeMinute = 0,
                        status = SessionStatus.LOCKED
                    )
                    sessionRepository.insert(defaultSession)
                }
                session = sessionRepository.getSessionForDate(logicalToday)
            }
            if (session == null) return@launch
            taskRepository.insert(
                Task(
                    title = title,
                    emoji = emoji,
                    sessionId = session.id,
                    scheduledDays = scheduledDays,
                    scheduledStartHour = startHour,
                    scheduledStartMinute = startMinute,
                    scheduledEndHour = endHour,
                    scheduledEndMinute = endMinute
                )
            )

            val isLocked = session.status == SessionStatus.LOCKED
            val nowMs = System.currentTimeMillis()
            val graceEndsAt = if (!isLocked) nowMs + 60 * 1000L else session.editGracePeriodEndsAt

            val newStartHour = startHour ?: session.startTimeHour
            val newStartMin = startMinute ?: session.startTimeMinute
            val newEndHour = if (startHour == null && endHour == null) 23 else (endHour ?: session.endTimeHour)
            val newEndMin = if (startHour == null && endHour == null) 59 else (endMinute ?: session.endTimeMinute)

            sessionRepository.updateTimeWindow(
                session.id, 
                newStartHour, newStartMin, newEndHour, newEndMin, 
                if (session.status == SessionStatus.VERIFIED) SessionStatus.LOCKED.value else session.status.value
            )
            sessionRepository.setEditGracePeriod(session.id, graceEndsAt)
            
            // Broadcast to update AccessibilityService if needed
            appContext.sendBroadcast(Intent(ACTION_SESSION_LOCKED).setPackage(appContext.packageName))

            // Increment Firebase limit count
            firebaseUserRepository.incrementTaskCount()
        }
    }



    /** Add a task scheduled for a specific future date */
    fun addScheduledTask(
        title: String,
        emoji: String = "✅",
        scheduledDate: String,
        scheduledDays: List<Int> = listOf(1, 2, 3, 4, 5, 6, 7),
        startHour: Int? = null,
        startMinute: Int? = null,
        endHour: Int? = null,
        endMinute: Int? = null,
        isTodayOnly: Boolean = false
    ) {
        viewModelScope.launch {
            // FIREBASE LIMIT CHECK
            val isPro = com.nichihab.app.ui.commercial.CommercialTierManager.isProUser
            val limit = com.nichihab.app.ui.commercial.CommercialTierManager.FREE_TIER_DAILY_TASK_LIMIT
            val canAdd = firebaseUserRepository.canAddTask(isPro, limit)
            if (!canAdd) {
                Toast.makeText(appContext, "Daily task limit reached. Please upgrade to Pro.", Toast.LENGTH_LONG).show()
                return@launch
            }

            // Check if a session exists for the target date
            var session = sessionRepository.getSessionForDate(scheduledDate)
            if (session == null) {
                // Create a session for the future date based on the latest template
                val latest = sessionRepository.getLatestSession()
                if (latest != null) {
                    val newSession = latest.copy(
                        id = 0, date = scheduledDate,
                        status = SessionStatus.LOCKED,
                        tasks = emptyList(),
                        earnedSeconds = 0, usedSeconds = 0, unlockStartedAt = null
                    )
                    val newId = sessionRepository.insert(newSession)
                    session = sessionRepository.getSessionForDate(scheduledDate)
                    if (session != null) {
                        NotificationScheduler.scheduleSessionNotifications(
                            appContext, 
                            session.id, 
                            session.date, 
                            session.startTimeHour, 
                            session.startTimeMinute
                        )
                    }
                }
            }
            if (session != null) {
                taskRepository.insert(
                    Task(
                        title = title,
                        emoji = emoji,
                        sessionId = session.id,
                        scheduledDays = scheduledDays,
                        scheduledStartHour = startHour,
                        scheduledStartMinute = startMinute,
                        scheduledEndHour = endHour,
                        scheduledEndMinute = endMinute,
                        isTodayOnly = isTodayOnly,
                        createdDate = if (isTodayOnly) scheduledDate else null
                    )
                )

                val isLocked = session.status == SessionStatus.LOCKED
                val nowMs = System.currentTimeMillis()
                val graceEndsAt = if (!isLocked) nowMs + 60 * 1000L else session.editGracePeriodEndsAt

                val newStartHour = startHour ?: session.startTimeHour
                val newStartMin = startMinute ?: session.startTimeMinute
                val newEndHour = if (startHour == null && endHour == null) 23 else (endHour ?: session.endTimeHour)
                val newEndMin = if (startHour == null && endHour == null) 59 else (endMinute ?: session.endTimeMinute)

                sessionRepository.updateTimeWindow(
                    session.id, 
                    newStartHour, newStartMin, newEndHour, newEndMin, 
                    if (session.status == SessionStatus.VERIFIED) SessionStatus.LOCKED.value else session.status.value
                )
                sessionRepository.setEditGracePeriod(session.id, graceEndsAt)
                
                // Broadcast to update AccessibilityService if needed
                appContext.sendBroadcast(Intent(ACTION_SESSION_LOCKED).setPackage(appContext.packageName))

                // Increment Firebase limit count
                firebaseUserRepository.incrementTaskCount()
            }
        }
    }

    fun updateTask(
        taskId: Long,
        title: String,
        emoji: String,
        scheduledDays: List<Int>,
        startHour: Int? = null,
        startMinute: Int? = null,
        endHour: Int? = null,
        endMinute: Int? = null
    ) {
        viewModelScope.launch {
            val existing = taskRepository.getById(taskId) ?: return@launch
            
            // Backend validation: Block update if it's a today task and 5 mins passed
            val todayDow = java.time.LocalDate.now().dayOfWeek.value
            val isForToday = existing.scheduledDays.contains(todayDow) || existing.isTodayOnly
            if (isForToday) {
                val ageMs = System.currentTimeMillis() - existing.createdAt
                if (ageMs > 60 * 1000) {
                    Toast.makeText(appContext, "Cannot edit today's tasks after 1 minute.", Toast.LENGTH_SHORT).show()
                    return@launch
                }
            }

            taskRepository.update(
                existing.copy(
                    title = title,
                    emoji = emoji,
                    scheduledDays = scheduledDays,
                    scheduledStartHour = startHour,
                    scheduledStartMinute = startMinute,
                    scheduledEndHour = endHour,
                    scheduledEndMinute = endMinute
                )
            )
        }
    }

    /** Delete a task by ID */
    fun deleteTask(taskId: Long) {
        viewModelScope.launch {
            val existing = taskRepository.getById(taskId) ?: return@launch
            
            // Backend validation for delete
            val todayDow = java.time.LocalDate.now().dayOfWeek.value
            val isForToday = existing.scheduledDays.contains(todayDow) || existing.isTodayOnly
            if (isForToday) {
                val ageMs = System.currentTimeMillis() - existing.createdAt
                if (ageMs > 60 * 1000) {
                    Toast.makeText(appContext, "Cannot delete today's tasks after 1 minute.", Toast.LENGTH_SHORT).show()
                    return@launch
                }
            }

            taskRepository.deleteById(taskId)
        }
    }

    /** User taps "Use Unlock Time" → start spending earned minutes */
    fun completeTask(taskId: Long) {
        viewModelScope.launch {
            val task = taskRepository.getById(taskId)
            if (task != null && !task.isCompleted) {
                taskRepository.markCompleted(taskId)
                val session = sessionRepository.getById(task.sessionId)
                if (session != null) {
                    sessionRepository.addEarnedSeconds(session.id, task.rewardMinutes * 60L)
                }
            }
        }
    }

    fun startUnlockTime() {
        viewModelScope.launch {
            val latestSession = sessionRepository.getLatestSession()
            val logicalToday = com.nichihab.app.util.TimeUtil.getLogicalDateDomain(latestSession)
            val session = sessionRepository.getSessionForDate(logicalToday) ?: return@launch
            val available = session.earnedSeconds - session.usedSeconds
            if (available > 0) {
                val now = System.currentTimeMillis()
                sessionRepository.startUnlock(session.id, now)
                sessionRepository.pauseUnlockTimer(session.id, now)
            }
        }
    }

    /** Stop spending unlock time (manual or auto-expired) */
    fun stopUnlockTime() {
        viewModelScope.launch {
            val latestSession = sessionRepository.getLatestSession()
            val logicalToday = com.nichihab.app.util.TimeUtil.getLogicalDateDomain(latestSession)
            val session = sessionRepository.getSessionForDate(logicalToday) ?: return@launch
            val elapsed = if (session.unlockStartedAt != null) {
                (System.currentTimeMillis() - session.unlockStartedAt) / 1000
            } else 0L
            sessionRepository.stopUnlock(session.id, elapsed)
        }
    }

    fun startCurrentSessionNow() {
        viewModelScope.launch {
            val blockedApps = blockedAppDao.getAllBlocked().first()
            if (blockedApps.isEmpty()) {
                Toast.makeText(appContext, "Please select at least one app to block first.", Toast.LENGTH_LONG).show()
                return@launch
            }

            val latestSession = sessionRepository.getLatestSession()
            val logicalToday = com.nichihab.app.util.TimeUtil.getLogicalDateDomain(latestSession)
            val session = sessionRepository.getSessionForDate(logicalToday) ?: return@launch
            val now = LocalTime.now()
            val origStartMin = session.startTimeHour * 60 + session.startTimeMinute
            val origEndMin = session.endTimeHour * 60 + session.endTimeMinute
            val durationMinutes = if (origEndMin > origStartMin) origEndMin - origStartMin
            else if (origEndMin < origStartMin) 1440 - origStartMin + origEndMin
            else 120
            val endTime = now.plusMinutes(durationMinutes.toLong())
            sessionRepository.updateTimeWindow(
                sessionId = session.id,
                startHour = now.hour, startMin = now.minute,
                endHour = endTime.hour, endMin = endTime.minute,
                status = SessionStatus.LOCKED.value
            )
        }
    }

    fun selectCalendarDay(day: Int) {
        _selectedCalendarDay.value = day
    }

    suspend fun getBlockedAppsCount(): Int {
        return blockedAppDao.getAllBlocked().first().size
    }

    fun reorderTasks(reorderedTasks: List<Task>) {
        viewModelScope.launch {
            val baseTime = System.currentTimeMillis()
            val updatedTasks = reorderedTasks.mapIndexed { index, task ->
                task.copy(createdAt = baseTime + index)
            }
            taskRepository.insertAll(updatedTasks)
        }
    }
}
