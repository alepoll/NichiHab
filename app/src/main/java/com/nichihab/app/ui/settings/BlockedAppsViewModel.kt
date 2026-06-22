package com.nichihab.app.ui.settings

import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nichihab.app.data.local.db.dao.BlockedAppDao
import com.nichihab.app.data.local.db.entity.BlockedAppEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.delay
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import com.nichihab.app.data.local.datastore.SettingsDataStore
import com.nichihab.app.domain.repository.SessionRepository
import com.nichihab.app.domain.model.SessionStatus

data class InstalledApp(
    val packageName: String,
    val appName: String,
    val isBlocked: Boolean = false,
    val icon: Drawable? = null,
    val isSuggested: Boolean = false
)

data class BlockedAppsUiState(
    val apps: List<InstalledApp> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val blockedCount: Int = 0,
    val isHardLocked: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class BlockedAppsViewModel @Inject constructor(
    private val blockedAppDao: BlockedAppDao,
    private val settingsDataStore: SettingsDataStore,
    private val sessionRepository: SessionRepository,
    @ApplicationContext private val context: android.content.Context
) : ViewModel() {

    private val _localState = MutableStateFlow(BlockedAppsUiState())
    
    private val sessionFlow = flow {
        while (true) {
            val latest = sessionRepository.getLatestSession()
            val logicalDate = com.nichihab.app.util.TimeUtil.getLogicalDateDomain(latest)
            emit(logicalDate)
            delay(60_000)
        }
    }
    .distinctUntilChanged()
    .flatMapLatest { logicalDate ->
        sessionRepository.observeSessionForDate(logicalDate)
    }

    val uiState: StateFlow<BlockedAppsUiState> = combine(
        _localState,
        settingsDataStore.strictnessMode,
        sessionFlow
    ) { local, strictness, session ->
        val isVerified = session?.status == SessionStatus.VERIFIED
        val isActive = if (session != null && !isVerified) {
            val now = LocalTime.now()
            val nowSec = now.toSecondOfDay()
            val startSec = session.startTimeHour * 3600 + session.startTimeMinute * 60
            val endSec = session.endTimeHour * 3600 + session.endTimeMinute * 60
            if (endSec > startSec) nowSec >= startSec && nowSec < endSec
            else if (endSec < startSec) nowSec >= startSec || nowSec < endSec
            else session.status == SessionStatus.LOCKED
        } else false

        val nowMs = System.currentTimeMillis()
        val gracePeriodActive = session?.editGracePeriodEndsAt?.let { it > nowMs } ?: false
        
        // Block editing if: Hard mode is ON AND (session is ACTIVE or grace period has EXPIRED)
        // This mirrors the task lockdown rule.
        val locked = strictness == "hard" && isActive && !gracePeriodActive && !isVerified

        local.copy(isHardLocked = locked)
    }.stateIn(
        scope = viewModelScope,
        started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
        initialValue = BlockedAppsUiState()
    )

    // Default suggestions
    private val suggestedPackages = setOf(
        "com.instagram.android",
        "com.zhiliaoapp.musically", // TikTok
        "com.google.android.youtube",
        "com.twitter.android",
        "com.reddit.frontpage",
        "com.snapchat.android",
        "com.whatsapp",
        "com.facebook.katana",
        "com.facebook.orca"
    )

    // In-memory cache of resolved app metadata (package → name)
    // Icons are loaded lazily to avoid main-thread jank
    private var cachedAppNames: Map<String, String> = emptyMap()

    init {
        loadApps()
    }

    /**
     * Two-phase loading:
     * Phase 1 (IO): Query installed packages, resolve names on background thread
     * Phase 2 (Main): Observe blocked status from Room and merge with cached names
     *
     * Icons are NOT loaded in Phase 1. They are loaded lazily by the UI via [getAppIcon].
     */
    private fun loadApps() {
        viewModelScope.launch {
            // Phase 1: Heavy PackageManager work on IO dispatcher
            val appEntries = withContext(Dispatchers.IO) {
                val pm = context.packageManager
                val mainIntent = android.content.Intent(android.content.Intent.ACTION_MAIN, null)
                mainIntent.addCategory(android.content.Intent.CATEGORY_LAUNCHER)

                val resolvedInfos = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    pm.queryIntentActivities(mainIntent, PackageManager.ResolveInfoFlags.of(0))
                } else {
                    pm.queryIntentActivities(mainIntent, 0)
                }

                val launcherPackages = resolvedInfos.map { it.activityInfo.packageName }.toSet()

                val installedApps = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    pm.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0))
                } else {
                    pm.getInstalledApplications(0)
                }
                    .filter { it.packageName in launcherPackages }
                    .filter { it.packageName != context.packageName }

                // Build name cache on IO thread (avoids main thread)
                installedApps.associate { appInfo ->
                    appInfo.packageName to pm.getApplicationLabel(appInfo).toString()
                }
            }

            // Cache for later use
            cachedAppNames = appEntries

            // Phase 2: Observe blocked status from Room (reactive) and merge
            blockedAppDao.getAll().collect { blockedEntities ->
                val blockedPackages = blockedEntities.filter { it.isBlocked }.map { it.packageName }.toSet()

                val appsList = cachedAppNames.map { (pkg, name) ->
                    InstalledApp(
                        packageName = pkg,
                        appName = name,
                        isBlocked = pkg in blockedPackages,
                        icon = null, // Icons loaded lazily via getAppIcon()
                        isSuggested = pkg in suggestedPackages
                    )
                }

                // Put blocked first, then suggested, then alphabetical
                val sorted = appsList.sortedWith(
                    compareByDescending<InstalledApp> { it.isBlocked }
                        .thenByDescending { it.isSuggested }
                        .thenBy { it.appName }
                )

                _localState.update {
                    it.copy(
                        apps = sorted,
                        isLoading = false,
                        blockedCount = blockedPackages.size
                    )
                }
            }
        }
    }

    /** Lazy icon loading — called by the UI for visible items only */
    fun getAppIcon(packageName: String): Drawable? {
        return try {
            context.packageManager.getApplicationIcon(packageName)
        } catch (_: Exception) { null }
    }

    fun toggleApp(packageName: String, appName: String) {
        viewModelScope.launch {
            val currentlyBlocked = blockedAppDao.isBlocked(packageName)
            if (currentlyBlocked) {
                blockedAppDao.delete(packageName)
            } else {
                blockedAppDao.insert(BlockedAppEntity(packageName = packageName, appName = appName, isBlocked = true))
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _localState.update { it.copy(searchQuery = query) }
    }
}
