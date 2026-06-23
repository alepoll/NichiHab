package com.nichihab.app.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nichihab.app.data.local.datastore.SettingsDataStore
import com.nichihab.app.domain.model.GoalType
import com.nichihab.app.domain.model.Session
import com.nichihab.app.domain.model.Task
import com.nichihab.app.domain.model.TimeWindow
import com.nichihab.app.domain.repository.SessionRepository
import com.nichihab.app.domain.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import com.nichihab.app.util.AppStrings

data class OnboardingUiState(
    val currentPage: Int = 0,
    val selectedGoal: GoalType? = null,
    val tasks: List<TaskInput> = emptyList(),
    val editingTaskIndex: Int? = null,
    val customTaskText: String = "",
    val selectedTimeWindow: TimeWindow? = null,
    val customStartHour: Int = 9,
    val customStartMinute: Int = 0,
    val customEndHour: Int = 11,
    val customEndMinute: Int = 0,
    val repeatDaily: Boolean = true,
    val isCompleting: Boolean = false,
    // System Permissions (mandatory)
    val overlayGranted: Boolean = false,
    val accessibilityGranted: Boolean = false,
    val batteryOptimizationIgnored: Boolean = false,
    // Privacy
    val privacyAccepted: Boolean = false
)

data class TaskInput(
    val title: String,
    val emoji: String = "✅",
    val estimatedMinutes: Int = 5,
    val isSelected: Boolean = false,
    val isCustom: Boolean = false
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val sessionRepository: SessionRepository,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    /** Cached language for localized task suggestions */
    private var cachedLanguage: String = "en"

    init {
        viewModelScope.launch {
            cachedLanguage = settingsDataStore.language.first()
        }
    }

    fun selectGoal(goal: GoalType) {
        _uiState.update {
            it.copy(
                selectedGoal = goal,
                tasks = getSuggestedTasks(goal)
            )
        }
    }

    fun nextPage() {
        _uiState.update { it.copy(currentPage = (it.currentPage + 1).coerceAtMost(5)) }
    }

    fun previousPage() {
        _uiState.update { it.copy(currentPage = (it.currentPage - 1).coerceAtLeast(0)) }
    }

    fun toggleTaskSelection(index: Int) {
        _uiState.update { state ->
            val selectedCount = state.tasks.count { it.isSelected }
            val task = state.tasks[index]
            val newTasks = state.tasks.toMutableList()
            if (task.isSelected) {
                newTasks[index] = task.copy(isSelected = false)
            } else if (selectedCount < 3) {
                newTasks[index] = task.copy(isSelected = true)
            }
            state.copy(tasks = newTasks)
        }
    }

    fun startEditingTask(index: Int) {
        _uiState.update {
            it.copy(editingTaskIndex = index, customTaskText = it.tasks[index].title)
        }
    }

    fun updateEditingText(text: String) {
        _uiState.update { it.copy(customTaskText = text) }
    }

    fun saveEditedTask() {
        _uiState.update { state ->
            val index = state.editingTaskIndex ?: return@update state
            val newTasks = state.tasks.toMutableList()
            newTasks[index] = newTasks[index].copy(title = state.customTaskText, isCustom = true)
            state.copy(tasks = newTasks, editingTaskIndex = null, customTaskText = "")
        }
    }

    fun cancelEditing() {
        _uiState.update { it.copy(editingTaskIndex = null, customTaskText = "") }
    }

    fun addCustomTask() {
        _uiState.update { state ->
            if (state.tasks.size >= 6) return@update state
            val newTask = TaskInput(
                title = "Custom task",
                emoji = "✏️",
                isCustom = true,
                isSelected = state.tasks.count { it.isSelected } < 3
            )
            state.copy(tasks = state.tasks + newTask)
        }
    }

    fun removeTask(index: Int) {
        _uiState.update { state ->
            val newTasks = state.tasks.toMutableList()
            if (index in newTasks.indices) newTasks.removeAt(index)
            state.copy(tasks = newTasks)
        }
    }

    fun selectTimeWindow(window: TimeWindow) {
        _uiState.update {
            it.copy(
                selectedTimeWindow = window,
                customStartHour = if (!window.isCustom) window.startHour else it.customStartHour,
                customStartMinute = if (!window.isCustom) window.startMinute else it.customStartMinute,
                customEndHour = if (!window.isCustom) window.endHour else it.customEndHour,
                customEndMinute = if (!window.isCustom) window.endMinute else it.customEndMinute
            )
        }
    }

    fun updateCustomStartTime(hour: Int, minute: Int) {
        _uiState.update { it.copy(customStartHour = hour, customStartMinute = minute) }
    }

    fun updateCustomEndTime(hour: Int, minute: Int) {
        _uiState.update { it.copy(customEndHour = hour, customEndMinute = minute) }
    }

    fun toggleRepeatDaily() {
        _uiState.update { it.copy(repeatDaily = !it.repeatDaily) }
    }

    fun completeOnboarding(onComplete: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isCompleting = true) }
            val state = _uiState.value
            val goal = state.selectedGoal ?: GoalType.CUSTOM
            val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

            // Create session
            val startH = state.selectedTimeWindow?.let {
                if (it.isCustom) state.customStartHour else it.startHour
            } ?: state.customStartHour
            val startM = state.selectedTimeWindow?.let {
                if (it.isCustom) state.customStartMinute else it.startMinute
            } ?: state.customStartMinute
            val endH = state.selectedTimeWindow?.let {
                if (it.isCustom) state.customEndHour else it.endHour
            } ?: state.customEndHour
            val endM = state.selectedTimeWindow?.let {
                if (it.isCustom) state.customEndMinute else it.endMinute
            } ?: state.customEndMinute

            val sessionId = sessionRepository.insert(
                Session(
                    date = today,
                    startTimeHour = startH,
                    startTimeMinute = startM,
                    endTimeHour = endH,
                    endTimeMinute = endM,
                    goalType = goal,
                    isRepeating = state.repeatDaily
                )
            )

            // Create selected tasks
            val selectedTasks = state.tasks.filter { it.isSelected }
            val domainTasks = selectedTasks.map { input ->
                Task(
                    title = input.title,
                    emoji = input.emoji,
                    goalType = goal,
                    isCustom = input.isCustom,
                    estimatedMinutes = input.estimatedMinutes,
                    sessionId = sessionId
                )
            }
            taskRepository.insertAll(domainTasks)

            settingsDataStore.acceptPrivacyPolicy()
            settingsDataStore.setOnboardingCompleted(true)
            _uiState.update { it.copy(isCompleting = false) }
            onComplete()
        }
    }

    /** Skip onboarding with sensible defaults (no tasks added) */
    fun skipOnboarding(onComplete: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isCompleting = true) }
            
            // Just finalize without creating any session or tasks
            // The HomeViewModel will auto-create a session upon first open

            settingsDataStore.acceptPrivacyPolicy()
            settingsDataStore.setOnboardingCompleted(true)
            _uiState.update { it.copy(isCompleting = false) }
            onComplete()
        }
    }

    fun setPrivacyAccepted(accepted: Boolean) {
        _uiState.update { it.copy(privacyAccepted = accepted) }
    }

    fun setOverlayGranted(granted: Boolean) {
        _uiState.update { it.copy(overlayGranted = granted) }
    }

    fun setAccessibilityGranted(granted: Boolean) {
        _uiState.update { it.copy(accessibilityGranted = granted) }
    }

    fun setBatteryOptimizationIgnored(granted: Boolean) {
        _uiState.update { it.copy(batteryOptimizationIgnored = granted) }
    }

    val allSystemPermissionsGranted: Boolean
        get() = _uiState.value.let {
            it.overlayGranted && it.accessibilityGranted && it.batteryOptimizationIgnored
        }

    private fun getSuggestedTasks(goal: GoalType): List<TaskInput> {
        val s = AppStrings(cachedLanguage)
        return when (goal) {
            GoalType.MORNING_ROUTINE -> listOf(
                TaskInput(s.get("task_make_bed"), "🛏️", 2),
                TaskInput(s.get("task_drink_water"), "💧", 1),
                TaskInput(s.get("task_journaling"), "📝", 10)
            )
            GoalType.DEEP_WORK -> listOf(
                TaskInput(s.get("task_clear_desk"), "🧹", 3),
                TaskInput(s.get("task_write_priorities"), "📋", 5),
                TaskInput(s.get("task_put_phone_away"), "📱", 1)
            )
            GoalType.FITNESS -> listOf(
                TaskInput(s.get("task_pushups"), "💪", 2),
                TaskInput(s.get("task_stretching"), "🧘", 5),
                TaskInput(s.get("task_walk"), "🚶", 10)
            )
            GoalType.CUSTOM -> listOf(
                TaskInput(s.get("task_read"), "📖", 10),
                TaskInput(s.get("task_meditate"), "🧘", 5),
                TaskInput(s.get("task_tidy"), "✨", 5)
            )
        }
    }
}
