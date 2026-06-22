package com.nichihab.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nichihab.app.data.local.datastore.SettingsDataStore
import com.nichihab.app.domain.model.SessionStatus
import com.nichihab.app.domain.repository.SessionRepository
import com.nichihab.app.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.delay
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class SettingsUiState(
    val apiProvider: String = Constants.PROVIDER_GEMINI,
    val themeMode: String = "system",
    val strictnessMode: String = "normal",
    val userName: String = "",
    val notificationsEnabled: Boolean = false,
    val language: String = "auto",
    val isSessionActive: Boolean = false,
    val allTasksComplete: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
    private val sessionRepository: SessionRepository
) : ViewModel() {

    private val _localState = MutableStateFlow(SettingsUiState())

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

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsDataStore.apiProvider,
        settingsDataStore.apiKey,
        settingsDataStore.themeMode,
        settingsDataStore.strictnessMode,
        settingsDataStore.userName,
        settingsDataStore.notificationsEnabled,
        settingsDataStore.language,
        sessionFlow,
        _localState
    ) { args: Array<Any?> ->
        val provider = args[0] as String
        val apiKey = args[1] as String
        val theme = args[2] as String
        val strictness = args[3] as String
        val name = args[4] as String
        val notifications = args[5] as Boolean
        val lang = args[6] as String
        @Suppress("UNCHECKED_CAST")
        val session = args[7] as? com.nichihab.app.domain.model.Session
        val local = args[8] as SettingsUiState

        // Determine if session is currently active (inside time window and LOCKED)
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

        local.copy(
            apiProvider = provider,
            themeMode = theme,
            strictnessMode = strictness,
            userName = name,
            notificationsEnabled = notifications,
            language = lang,
            isSessionActive = isActive,
            allTasksComplete = isVerified
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )



    fun onThemeModeChanged(mode: String) {
        viewModelScope.launch {
            settingsDataStore.setThemeMode(mode)
        }
    }

    fun onStrictnessModeChanged(mode: String) {
        // Block toggling OFF hard mode during an active session
        val currentState = uiState.value
        if (currentState.isSessionActive && currentState.strictnessMode == "hard" && mode == "normal") {
            return // Can't disable hard mode mid-session
        }
        viewModelScope.launch {
            settingsDataStore.setStrictnessMode(mode)
        }
    }

    fun onUserNameChanged(name: String) {
        viewModelScope.launch {
            settingsDataStore.setUserName(name)
        }
    }

    fun onNotificationsToggled(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setNotificationsEnabled(enabled)
        }
    }

    fun onLanguageChanged(lang: String) {
        viewModelScope.launch {
            settingsDataStore.setLanguage(lang)
        }
    }
}

