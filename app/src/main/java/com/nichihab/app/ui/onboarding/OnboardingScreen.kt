package com.nichihab.app.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nichihab.app.util.LocalStrings

private const val TOTAL_PAGES = 6

/**
 * Onboarding flow: Welcome → System Permissions (mandatory) → Goal → Tasks → Time Window
 *
 * Page 1 (SystemPermissions) blocks navigation until all 3 system permissions are granted.
 * Pages 2-4 follow the original goal/tasks/time flow.
 * The skip button is hidden on the permissions page.
 */
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val s = LocalStrings.current

    // Whether we are on the mandatory permissions page or privacy page
    val isPermissionsPage = state.currentPage == 2
    val isPrivacyPage = state.currentPage == 1
    val allPermsGranted = state.overlayGranted && state.accessibilityGranted && state.batteryOptimizationIgnored

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top bar: progress + skip button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LinearProgressIndicator(
                progress = { (state.currentPage + 1) / TOTAL_PAGES.toFloat() },
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(CircleShape),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Spacer(modifier = Modifier.width(12.dp))
            // Hide skip button until after privacy and permissions (mandatory)
            if (state.currentPage > 2) {
                TextButton(
                    onClick = { viewModel.skipOnboarding(onComplete) },
                    enabled = !state.isCompleting
                ) {
                    Text(
                        text = s.get("onboarding_skip"),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // Placeholder to maintain layout
                Spacer(modifier = Modifier.width(56.dp))
            }
        }

        // Page indicators
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(TOTAL_PAGES) { index ->
                Box(
                    modifier = Modifier
                        .size(if (index == state.currentPage) 10.dp else 8.dp)
                        .clip(CircleShape)
                        .background(
                            if (index == state.currentPage) MaterialTheme.colorScheme.primary
                            else if (index < state.currentPage) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                )
                if (index < TOTAL_PAGES - 1) Spacer(modifier = Modifier.width(8.dp))
            }
        }

        // Content
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            AnimatedContent(
                targetState = state.currentPage,
                transitionSpec = {
                    if (targetState > initialState) {
                        slideInHorizontally { it } + fadeIn() togetherWith
                                slideOutHorizontally { -it } + fadeOut()
                    } else {
                        slideInHorizontally { -it } + fadeIn() togetherWith
                                slideOutHorizontally { it } + fadeOut()
                    }
                },
                label = "onboarding_page"
            ) { page ->
                when (page) {
                    0 -> WelcomeTutorialPage()
                    1 -> PrivacyDisclosurePage()
                    2 -> SystemPermissionsPage(
                        onAllGranted = {
                            viewModel.setOverlayGranted(true)
                            viewModel.setAccessibilityGranted(true)
                            viewModel.setBatteryOptimizationIgnored(true)
                        }
                    )
                    3 -> GoalPickerPage(
                        selectedGoal = state.selectedGoal,
                        onGoalSelected = { viewModel.selectGoal(it) }
                    )
                    4 -> TaskSetupPage(
                        tasks = state.tasks,
                        editingIndex = state.editingTaskIndex,
                        editingText = state.customTaskText,
                        onToggleTask = { viewModel.toggleTaskSelection(it) },
                        onEditTask = { viewModel.startEditingTask(it) },
                        onUpdateEditText = { viewModel.updateEditingText(it) },
                        onSaveEdit = { viewModel.saveEditedTask() },
                        onCancelEdit = { viewModel.cancelEditing() },
                        onAddCustom = { viewModel.addCustomTask() },
                        onRemoveTask = { viewModel.removeTask(it) }
                    )
                    5 -> TimeWindowPage(
                        selectedWindow = state.selectedTimeWindow,
                        customStartHour = state.customStartHour,
                        customStartMinute = state.customStartMinute,
                        customEndHour = state.customEndHour,
                        customEndMinute = state.customEndMinute,
                        repeatDaily = state.repeatDaily,
                        onWindowSelected = { viewModel.selectTimeWindow(it) },
                        onCustomStartChanged = { h, m -> viewModel.updateCustomStartTime(h, m) },
                        onCustomEndChanged = { h, m -> viewModel.updateCustomEndTime(h, m) },
                        onRepeatToggle = { viewModel.toggleRepeatDaily() }
                    )
                }
            }
        }

        // Navigation buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back button (hidden on page 0, also hidden on permissions/privacy pages to prevent bypass)
            AnimatedVisibility(visible = state.currentPage > 0 && !isPermissionsPage && !isPrivacyPage) {
                IconButton(
                    onClick = { viewModel.previousPage() },
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            CircleShape
                        )
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (state.currentPage == 0 || isPermissionsPage || isPrivacyPage) Spacer(modifier = Modifier.width(48.dp))

            // Next / Complete button
            val canProceed = when (state.currentPage) {
                0 -> true // Welcome page — always can proceed
                1 -> true // Privacy page — acceptance is implied by continuing
                2 -> allPermsGranted // Permissions page — must grant all
                3 -> state.selectedGoal != null
                4 -> state.tasks.any { it.isSelected }
                5 -> state.selectedTimeWindow != null
                else -> false
            }

            val isLastPage = state.currentPage == TOTAL_PAGES - 1

            WithPulsingGuide(
                showGuide = state.currentPage == 0,
                dotOffset = Pair(4.dp, (-4).dp)
            ) {
                Button(
                    onClick = {
                        if (!isLastPage) {
                            viewModel.nextPage()
                        } else {
                            viewModel.completeOnboarding(onComplete)
                        }
                    },
                    enabled = canProceed && !state.isCompleting,
                    modifier = Modifier.height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isPermissionsPage && canProceed) NeonBlue
                        else MaterialTheme.colorScheme.primary
                    ),
                    shape = MaterialTheme.shapes.large
                ) {
                    if (state.isCompleting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        val buttonText = when {
                            isLastPage -> s.get("onboarding_get_started")
                            isPrivacyPage -> s.get("onboarding_continue_accept_terms")
                            else -> s.get("onboarding_continue")
                        }
                        Text(
                            text = buttonText,
                            style = MaterialTheme.typography.labelLarge
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
