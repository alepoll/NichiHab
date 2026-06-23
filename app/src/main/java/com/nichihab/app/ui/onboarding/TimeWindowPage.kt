package com.nichihab.app.ui.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nichihab.app.domain.model.TimeWindow
import com.nichihab.app.util.LocalStrings

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TimeWindowPage(
    selectedWindow: TimeWindow?,
    customStartHour: Int,
    customStartMinute: Int,
    customEndHour: Int,
    customEndMinute: Int,
    repeatDaily: Boolean,
    onWindowSelected: (TimeWindow) -> Unit,
    onCustomStartChanged: (Int, Int) -> Unit,
    onCustomEndChanged: (Int, Int) -> Unit,
    onRepeatToggle: () -> Unit
) {
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    val s = LocalStrings.current

    if (showStartTimePicker) {
        TimePickerDialog(
            initialHour = customStartHour,
            initialMinute = customStartMinute,
            onDismiss = { showStartTimePicker = false },
            onConfirm = { h, m ->
                onCustomStartChanged(h, m)
                showStartTimePicker = false
            }
        )
    }

    if (showEndTimePicker) {
        TimePickerDialog(
            initialHour = customEndHour,
            initialMinute = customEndMinute,
            onDismiss = { showEndTimePicker = false },
            onConfirm = { h, m ->
                onCustomEndChanged(h, m)
                showEndTimePicker = false
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text(
            text = s.get("onboarding_time_title"),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = s.get("onboarding_time_subtitle"),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))

        // Time window chips
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            TimeWindow.PRESETS.forEach { window ->
                val isSelected = selectedWindow?.label == window.label
                FilterChip(
                    selected = isSelected,
                    onClick = { onWindowSelected(window) },
                    label = {
                        Text(
                            text = "${window.emoji} ${window.label}",
                            style = MaterialTheme.typography.labelLarge
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        selectedBorderColor = MaterialTheme.colorScheme.primary,
                        enabled = true,
                        selected = isSelected
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Time display card
        if (selectedWindow != null) {
            val isCustom = selectedWindow.isCustom
            val startH = if (isCustom) customStartHour else selectedWindow.startHour
            val startM = if (isCustom) customStartMinute else selectedWindow.startMinute
            val endH = if (isCustom) customEndHour else selectedWindow.endHour
            val endM = if (isCustom) customEndMinute else selectedWindow.endMinute

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = s.get("active_window"),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        TimeDisplay(
                            hour = startH,
                            minute = startM,
                            isCustom = isCustom,
                            onTimeChanged = onCustomStartChanged,
                            onClick = { if (isCustom) showStartTimePicker = true }
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "→",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        TimeDisplay(
                            hour = endH,
                            minute = endM,
                            isCustom = isCustom,
                            onTimeChanged = onCustomEndChanged,
                            onClick = { if (isCustom) showEndTimePicker = true }
                        )
                    }

                    // Custom time picker buttons
                    AnimatedVisibility(
                        visible = isCustom,
                        enter = expandVertically(),
                        exit = shrinkVertically()
                    ) {
                        Column(
                            modifier = Modifier.padding(top = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = s.get("time_adjust_hint"),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                TimeAdjustButton(s.get("start_minus_1h")) {
                                    onCustomStartChanged((customStartHour - 1 + 24) % 24, customStartMinute)
                                }
                                TimeAdjustButton(s.get("start_plus_1h")) {
                                    onCustomStartChanged((customStartHour + 1) % 24, customStartMinute)
                                }
                                TimeAdjustButton(s.get("end_minus_1h")) {
                                    onCustomEndChanged((customEndHour - 1 + 24) % 24, customEndMinute)
                                }
                                TimeAdjustButton(s.get("end_plus_1h")) {
                                    onCustomEndChanged((customEndHour + 1) % 24, customEndMinute)
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Repeat daily toggle
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "🔁 " + s.get("repeat_daily_label"),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = s.get("same_schedule"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = repeatDaily,
                    onCheckedChange = { onRepeatToggle() },
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit
) {
    val s = LocalStrings.current
    val state = rememberTimePickerState(initialHour = initialHour, initialMinute = initialMinute)
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour, state.minute) }) {
                Text(s.get("ok"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(s.get("cancel"))
            }
        },
        text = {
            TimePicker(state = state)
        }
    )
}

@Composable
private fun TimeDisplay(
    hour: Int,
    minute: Int,
    isCustom: Boolean,
    onTimeChanged: (Int, Int) -> Unit,
    onClick: () -> Unit = {}
) {
    val amPm = if (hour < 12) "AM" else "PM"
    val displayHour = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        ),
        enabled = isCustom,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "%d:%02d".format(displayHour, minute),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = amPm,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun TimeAdjustButton(
    label: String,
    onClick: () -> Unit
) {
    val containerColor by animateColorAsState(
        targetValue = MaterialTheme.colorScheme.surfaceVariant,
        label = "adjustBtn"
    )
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
