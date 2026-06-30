package com.nichihab.app.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import com.nichihab.app.ui.home.HomeViewModel
import com.nichihab.app.data.local.db.NichiHabDatabase
import com.nichihab.app.data.local.datastore.SettingsDataStore
import com.nichihab.app.domain.model.SessionStatus
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.ExperimentalCoroutinesApi
import com.nichihab.app.data.local.db.entity.SessionEntity
import com.nichihab.app.data.local.db.entity.TaskEntity
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@AndroidEntryPoint
class NichiHabAccessibilityService : AccessibilityService() {

    @Inject lateinit var database: NichiHabDatabase
    @Inject lateinit var settingsDataStore: SettingsDataStore

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var lastOverlayPackage: String? = null
    private var overlayShowing = false
    private var lastOverlayLaunchTime = 0L
    private var cachedStrictnessMode: String = "normal"

    // In-memory cache of blocked packages — eliminates per-event DB queries
    @Volatile
    private var cachedBlockedPackages: Set<String> = emptySet()

    private var screenStateReceiver: BroadcastReceiver? = null

    // Pre-calculated cache
    @Volatile private var cachedSession: SessionEntity? = null
    @Volatile private var cachedTasks: List<TaskEntity> = emptyList()
    @Volatile private var cachedIncompleteCount: Int = 0
    
    

    private val todayFlow = flow {
        while (true) {
            val latest = database.sessionDao().getLatestSession()
            emit(com.nichihab.app.util.TimeUtil.getLogicalDate(latest))
            delay(60_000)
        }
    }.distinctUntilChanged()

    // Packages that Hard Mode should block (System Settings only — to prevent
    // disabling accessibility/overlay permissions during active sessions).
    // Package installers and Play Store are NOT blocked — Device Admin API
    // prevents uninstalling OUR app, while allowing other app uninstalls normally.
    private val hardModeBlockedPackages = setOf(
        // Android system settings
        "com.android.settings",
        // Samsung
        "com.samsung.android.app.settings",
        "com.samsung.android.settings",
        // Xiaomi / MIUI
        "com.miui.securitycenter",
        "com.miui.permcenter",
        // OnePlus / Oppo / Realme
        "com.coloros.safecenter",
        "com.oplus.safecenter",
        // Huawei
        "com.huawei.systemmanager",
        // Vivo
        "com.vivo.permissionmanager",
        // SubSettings (used for App Info deep-links on some OEMs)
        "com.android.settings.SubSettings",
        // LG
        "com.lge.settings",
        // Sony
        "com.sonymobile.settings",
        // Package installers to prevent uninstall
        "com.google.android.packageinstaller",
        "com.android.packageinstaller"
    )

    // Content descriptions no longer needed as we block based on 'NichiHab' keyword directly.

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun onServiceConnected() {
        super.onServiceConnected()
        serviceInfo = serviceInfo.apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                    AccessibilityEvent.TYPE_VIEW_CLICKED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 100
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or 
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                    AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
        }

        // Cache strictness mode
        serviceScope.launch {
            try {
                settingsDataStore.strictnessMode.collect { mode ->
                    cachedStrictnessMode = mode
                }
            } catch (_: Exception) {}
        }

        // Cache blocked packages (reactive — updates automatically when DB changes)
        serviceScope.launch {
            try {
                database.blockedAppDao().getAllBlocked().collect { blockedEntities ->
                    cachedBlockedPackages = blockedEntities.map { it.packageName }.toSet()
                }
            } catch (_: Exception) {}
        }

        // Reactive DB Cache for Sessions and Tasks (Cross-midnight safe)
        serviceScope.launch {
            todayFlow
                .flatMapLatest { todayDate ->
                    database.sessionDao().observeSessionForDate(todayDate)
                }
                .flatMapLatest { session ->
                    cachedSession = session
                    if (session != null) {
                        database.taskDao().getTasksForSession(session.id)
                    } else {
                        flowOf(emptyList())
                    }
                }
                .collect { tasks ->
                    cachedTasks = tasks
                    val latest = database.sessionDao().getLatestSession()
                    val logicalTodayDate = com.nichihab.app.util.TimeUtil.getLogicalDate(latest)
                    val todayDow = LocalDate.parse(logicalTodayDate).dayOfWeek.value.toString()
                    cachedIncompleteCount = tasks.count { task -> 
                        !task.isCompleted && (task.scheduledDays.contains(todayDow) || task.isTodayOnly)
                    }
                    // RE-TRIGGER blocking logic immediately when tasks change
                    val currentSess = cachedSession
                    if (currentSess != null && isSessionActiveNow(currentSess) && cachedIncompleteCount > 0) {
                        val isBlocked = currentActivePackage in cachedBlockedPackages
                        if (isBlocked) {
                            showBlockingOverlay(currentSess.id)
                        }
                    }
                    updateEnforcementJob(cachedSession)
                }
        }

        // Listen for Screen ON/OFF to handle smart timer earned time preservation
        val screenFilter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        screenStateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    Intent.ACTION_SCREEN_OFF -> {
                        serviceScope.launch {
                            val latest = database.sessionDao().getLatestSession()
                            val today = com.nichihab.app.util.TimeUtil.getLogicalDate(latest)
                            val session = database.sessionDao().getSessionForDate(today)
                            if (session != null && session.status == "unlocked" && session.unlockStartedAt != null) {
                                // Freeze earned time drain while screen is off
                                smartTimerJob?.cancel()
                                database.sessionDao().pauseUnlockTimer(session.id, System.currentTimeMillis())
                            }
                        }
                    }
                    Intent.ACTION_SCREEN_ON -> {
                        serviceScope.launch {
                            val latest = database.sessionDao().getLatestSession()
                            val today = com.nichihab.app.util.TimeUtil.getLogicalDate(latest)
                            val session = database.sessionDao().getSessionForDate(today)
                            val isBlocked = currentActivePackage in cachedBlockedPackages
                            if (session != null && session.status == "unlocked" && session.unlockStartedAt == null && isBlocked) {
                                database.sessionDao().resumeUnlockTimer(session.id, System.currentTimeMillis())
                            }
                            // Force check state immediately on screen wake
                            handleWindowChange(currentActivePackage, null, packageChanged = false)
                        }
                    }
                }
            }
        }
        registerReceiver(screenStateReceiver, screenFilter)
    }

    private fun updateEnforcementJob(session: SessionEntity?) {
        enforcementJob?.cancel()
        if (session == null || session.status == SessionStatus.VERIFIED.value) return

        enforcementJob = serviceScope.launch {
            while (true) {
                val nowMs = System.currentTimeMillis()
                val nowSec = LocalTime.now().toSecondOfDay()
                
                val startSec = session.startTimeHour * 3600 + session.startTimeMinute * 60
                val endSec = session.endTimeHour * 3600 + session.endTimeMinute * 60
                
                var delayMs = Long.MAX_VALUE
                
                // 1. Check Grace Period
                if (session.editGracePeriodEndsAt != null && session.editGracePeriodEndsAt > nowMs) {
                    val diff = session.editGracePeriodEndsAt - nowMs
                    if (diff < delayMs) delayMs = diff
                }
                
                // 2. Check Window Start
                if (startSec > nowSec) {
                    val diff = (startSec - nowSec) * 1000L
                    if (diff < delayMs) delayMs = diff
                }
                
                // 3. Check Window End
                if (endSec > nowSec) {
                    val diff = (endSec - nowSec) * 1000L
                    if (diff < delayMs) delayMs = diff
                }
                
                if (delayMs == Long.MAX_VALUE) {
                    // Midnight boundary
                    val msToMidnight = (86400 - nowSec) * 1000L
                    delayMs = if (msToMidnight > 0) msToMidnight else 60_000L
                }
                
                delay(delayMs + 100) // Wait for boundary to pass
                
                if (isSessionActiveNow(session) && cachedIncompleteCount > 0) {
                    val isBlocked = currentActivePackage in cachedBlockedPackages
                    if (isBlocked) {
                        showBlockingOverlay(session.id)
                    }
                }
            }
        }
    }

    private var currentActivePackage: String = ""
    private var smartTimerJob: kotlinx.coroutines.Job? = null
    private var enforcementJob: kotlinx.coroutines.Job? = null

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!::database.isInitialized) return

        val packageName = event?.packageName?.toString() ?: return

        // Handle both window state changes AND clicks (to detect navigation)
        val isSignificantEvent = event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || 
                                event.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED
                                
        val packageChanged = if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val changed = (packageName != currentActivePackage)
            if (changed) currentActivePackage = packageName
            changed
        } else false

        if (!isSignificantEvent) return

        if (packageName == applicationContext.packageName) {
            overlayShowing = false // Reset here to allow re-blocking when user leaves our app
            if (packageChanged) {
                serviceScope.launch { processSmartTimerExitOnly() }
            }
            return
        }

        // If we are in System UI or Launcher, allow it
        if (packageName.startsWith("com.android.systemui") ||
            packageName == "com.android.launcher" ||
            packageName.contains("launcher") ||
            packageName == "android") {

            if (packageChanged) {
                serviceScope.launch { processSmartTimerExitOnly() }
            }

            overlayShowing = false
            return
        }

        // Detect navigation to NichiHab-related screens within settings (Hard Mode only)
        if (packageName in hardModeBlockedPackages && cachedStrictnessMode == "hard") {
            val text = event.text?.joinToString(" ")?.lowercase() ?: ""
            val desc = event.contentDescription?.toString()?.lowercase() ?: ""
            if (text.contains("nichihab") || text.contains("focus lock") || desc.contains("nichihab") || desc.contains("focus lock")) {
                serviceScope.launch {
                    val session = cachedSession
                    if (session != null && isSessionActiveNow(session) && cachedIncompleteCount > 0) {
                        lastOverlayLaunchTime = System.currentTimeMillis()
                        showBlockingOverlay(session.id)
                    }
                }
                return
            }
        }
        
        handleWindowChange(packageName, event, packageChanged)
    }

    /**
     * Checks if a session is currently in its active time window.
     */
    private fun isSessionActiveNow(session: com.nichihab.app.data.local.db.entity.SessionEntity): Boolean {
        if (session.status == SessionStatus.VERIFIED.value) return false
        if (session.status == "unlocked") return false
        val nowMs = System.currentTimeMillis()
        val nowSec = LocalTime.now().toSecondOfDay()
        val startSec = session.startTimeHour * 3600 + session.startTimeMinute * 60
        val endSec = session.endTimeHour * 3600 + session.endTimeMinute * 60
        
        val isInsideWindow = if (endSec > startSec) nowSec >= startSec && nowSec < endSec
        else if (endSec < startSec) nowSec >= startSec || nowSec < endSec
        else session.status == "locked"

        return isInsideWindow
    }

    private suspend fun processSmartTimerExitOnly() {
        val latest = database.sessionDao().getLatestSession()
        val today = com.nichihab.app.util.TimeUtil.getLogicalDate(latest)
        val session = database.sessionDao().getSessionForDate(today) ?: return
        if (session.status == "unlocked") {
            smartTimerJob?.cancel()
            if (session.unlockStartedAt != null) {
                database.sessionDao().pauseUnlockTimer(session.id, System.currentTimeMillis())
            }
        }
    }

    private fun handleWindowChange(packageName: String, event: AccessibilityEvent?, packageChanged: Boolean) {
        // Debounce: don't spam overlay launches, but allow if package actually changed
        val now = System.currentTimeMillis()
        if (now - lastOverlayLaunchTime < 500 && !packageChanged) return

        serviceScope.launch {
            try {
                var session = cachedSession

                // AUTO-CREATE session if missing but repeat is on
                if (session == null) {
                    val latest = database.sessionDao().getLatestSession()
                    val today = com.nichihab.app.util.TimeUtil.getLogicalDate(latest)
                    session = database.sessionDao().getSessionForDate(today)
                    if (session == null) {
                        if (latest != null && latest.isRepeating) {
                            val newId = database.sessionDao().insert(latest.copy(id = 0, date = today, status = "locked", verifiedAt = null))
                            val tasks = database.taskDao().getTasksForSessionOnce(latest.id)
                            database.taskDao().insertAll(tasks.map { it.copy(id = 0, sessionId = newId, isCompleted = false, completedAt = null) })
                            session = database.sessionDao().getById(newId)
                        }
                    }
                }

                if (session == null) return@launch

                // If verified (all tasks done), don't block for rest of day
                if (session.status == SessionStatus.VERIFIED.value) {
                    overlayShowing = false
                    return@launch
                }

                // (Removed unconditional hard mode block for settings apps to allow general navigation)

                val isBlockedApp = packageName in cachedBlockedPackages
                val currentTimeMillis = System.currentTimeMillis()

                // ── Smart Timer Logic ──
                if (session.status == "unlocked") {
                    if (isBlockedApp) {
                        // User entered a blocked app -> Resume if needed
                        if (packageChanged && session.unlockStartedAt == null) {
                            database.sessionDao().resumeUnlockTimer(session.id, currentTimeMillis)
                            session = database.sessionDao().getById(session.id) ?: session
                        }

                        val elapsedSec = if (session.unlockStartedAt != null) (currentTimeMillis - session.unlockStartedAt!!) / 1000 else 0
                        val totalUsed = session.usedSeconds + elapsedSec
                        val remaining = session.earnedSeconds - totalUsed

                        if (remaining > 0) {
                            if (packageChanged) {
                                smartTimerJob?.cancel()
                                smartTimerJob = launch {
                                    kotlinx.coroutines.delay(remaining * 1000L)
                                    val expireTime = System.currentTimeMillis()
                                    val finalSession = database.sessionDao().getById(session.id)
                                    if (finalSession?.status == "unlocked" && finalSession.unlockStartedAt != null) {
                                        val finalElapsed = (expireTime - finalSession.unlockStartedAt!!) / 1000
                                        database.sessionDao().stopUnlock(session.id, finalElapsed)
                                        showBlockingOverlay(session.id)
                                    }
                                }
                            }
                            overlayShowing = false
                            return@launch
                        } else {
                            // Expired
                            smartTimerJob?.cancel()
                            if (session.unlockStartedAt != null) {
                                database.sessionDao().stopUnlock(session.id, elapsedSec)
                            } else {
                                database.sessionDao().stopUnlock(session.id, 0)
                            }
                            session = database.sessionDao().getById(session.id) ?: session
                        }
                    } else {
                        // User entered an unblocked app -> Pause timer
                        if (packageChanged) {
                            smartTimerJob?.cancel()
                            if (session.unlockStartedAt != null) {
                                database.sessionDao().pauseUnlockTimer(session.id, currentTimeMillis)
                            }
                        }
                        // We are in an unblocked app, so no overlay needed (unless it was explicitly blocked above)
                        overlayShowing = false
                        return@launch
                    }
                }

                val currentTime = LocalTime.now()
                val windowStart = LocalTime.of(session.startTimeHour, session.startTimeMinute)
                val windowEnd = LocalTime.of(session.endTimeHour, session.endTimeMinute)

                val nowSec = currentTime.toSecondOfDay()
                val startSec = windowStart.toSecondOfDay()
                val endSec = windowEnd.toSecondOfDay()

                // Handle cross-midnight windows (e.g. 22:00 -> 06:00)
                var isInsideWindow = if (endSec > startSec) {
                    // Normal window: start < end (e.g. 9:00 -> 11:00)
                    nowSec >= startSec && nowSec < endSec
                } else if (endSec < startSec) {
                    // Cross-midnight: end < start (e.g. 22:00 -> 06:00)
                    nowSec >= startSec || nowSec < endSec
                } else {
                    // start == end — treat as active if DB says locked
                    session.status == "locked"
                }

                // --- Grace Period Check (removed to allow immediate overlay) ---
                // if (session.editGracePeriodEndsAt != null && currentTimeMillis < session.editGracePeriodEndsAt) {
                //     isInsideWindow = false
                // }

                if (isInsideWindow) {
                    // --- Normal blocked apps check (uses in-memory cache) ---
                    val isBlocked = packageName in cachedBlockedPackages

                    if (!isBlocked) {
                        overlayShowing = false
                        return@launch
                    }

                    if (cachedIncompleteCount > 0) {
                        lastOverlayPackage = packageName
                        overlayShowing = true
                        lastOverlayLaunchTime = System.currentTimeMillis()
                        showBlockingOverlay(session.id)
                    } else {
                        overlayShowing = false
                    }
                } else {
                    overlayShowing = false
                }
            } catch (e: Exception) {
                // Silently handle DB errors
            }
        }
    }

    private fun showBlockingOverlay(sessionId: Long) {
        val intent = Intent(this, BlockingOverlayActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
            addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
            putExtra(BlockingOverlayActivity.EXTRA_SESSION_ID, sessionId)
        }
        startActivity(intent)
    }

    override fun onInterrupt() {
        overlayShowing = false
    }

    override fun onDestroy() {
        super.onDestroy()
        screenStateReceiver?.let { try { unregisterReceiver(it) } catch (_: Exception) {} }
        serviceScope.cancel()
    }
}
