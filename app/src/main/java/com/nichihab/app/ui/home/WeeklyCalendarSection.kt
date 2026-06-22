package com.nichihab.app.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nichihab.app.domain.model.Task
import com.nichihab.app.ui.theme.StatusVerified
import com.nichihab.app.ui.onboarding.NeonBlue
import com.nichihab.app.util.LocalStrings

/**
 * Weekly calendar strip (Mon–Sun) with time-block task visualization.
 */
@Composable
fun WeeklyCalendarSection(
    selectedDay: Int,
    todayDow: Int,
    tasks: List<Task>,
    onDaySelected: (Int) -> Unit,
    onTaskTapped: (Task) -> Unit
) {
    val s = LocalStrings.current
    val dayKeys = listOf("day_mon", "day_tue", "day_wed", "day_thu", "day_fri", "day_sat", "day_sun")
    val dayShort = listOf(
        s.get("day_mon_short"), s.get("day_tue_short"), s.get("day_wed_short"),
        s.get("day_thu_short"), s.get("day_fri_short"), s.get("day_sat_short"), s.get("day_sun_short")
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Spacer(modifier = Modifier.height(4.dp))

        // Day strip
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            for (day in 1..7) {
                val isSelected = day == selectedDay
                val isToday = day == todayDow
                val hasTasks = tasks.any { day in it.scheduledDays }
                val bgColor by animateColorAsState(
                    targetValue = when {
                        isSelected -> MaterialTheme.colorScheme.primary
                        isToday -> MaterialTheme.colorScheme.primaryContainer
                        else -> MaterialTheme.colorScheme.surfaceContainer
                    },
                    animationSpec = tween(250),
                    label = "dayBg$day"
                )
                val textColor by animateColorAsState(
                    targetValue = when {
                        isSelected -> MaterialTheme.colorScheme.onPrimary
                        isToday -> MaterialTheme.colorScheme.onPrimaryContainer
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    animationSpec = tween(250),
                    label = "dayText$day"
                )

                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(bgColor)
                        .clickable { onDaySelected(day) }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = dayShort[day - 1],
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                        color = textColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    // Activity indicator dot
                    if (hasTasks) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                                    else NeonBlue,
                                    CircleShape
                                )
                        )
                    } else {
                        Spacer(modifier = Modifier.size(6.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Time blocks for selected day
        val dayTasks = tasks
            .filter { selectedDay in it.scheduledDays }
            .sortedBy { (it.scheduledStartHour ?: 0) * 60 + (it.scheduledStartMinute ?: 0) }

        if (dayTasks.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = s.get("no_tasks_for_day"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            // Scrollable time blocks
            val scrollState = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                dayTasks.forEach { task ->
                    TimeBlockCard(
                        task = task,
                        onTap = { onTaskTapped(task) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TimeBlockCard(
    task: Task,
    onTap: () -> Unit
) {
    Card(
        modifier = Modifier
            .height(56.dp)
            .clickable { onTap() },
        colors = CardDefaults.cardColors(
            containerColor = if (task.isCompleted)
                StatusVerified.copy(alpha = 0.1f)
            else
                MaterialTheme.colorScheme.surfaceContainer
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box {
                Text(task.emoji, fontSize = 24.sp)
                if (task.isCompleted) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp).align(Alignment.BottomEnd),
                        tint = StatusVerified
                    )
                }
            }
            Text(
                text = task.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
