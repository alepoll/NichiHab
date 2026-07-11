package com.nichihab.app.ui.stats

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Process
import android.provider.Settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Whatshot
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nichihab.app.ui.theme.StatusVerified
import com.nichihab.app.ui.theme.StreakGold
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import com.nichihab.app.util.LocalStrings
import com.nichihab.app.ui.onboarding.NeonBlue
import com.nichihab.app.ui.onboarding.PulsingDot
import com.nichihab.app.ui.commercial.CommercialTierManager
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextButton
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.runtime.DisposableEffect

@Composable
fun StatsScreen(
    viewModel: StatsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val s = LocalStrings.current
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var showUsageDialog by remember { mutableStateOf(false) }
    var usageStatsGranted by remember { mutableStateOf(hasUsageStatsPermission(context)) }
    var hasDismissedDialog by remember { mutableStateOf(false) }
    var showPaywall by remember { mutableStateOf(false) }
    if (showPaywall) {
        com.nichihab.app.ui.commercial.PaywallScreen(onDismiss = { showPaywall = false })
    }

    // Re-check on resume
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                usageStatsGranted = hasUsageStatsPermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Show dialog on first visit if permission not granted
    if (!usageStatsGranted && !hasDismissedDialog) {
        showUsageDialog = true
    }

    // Usage stats permission dialog
    if (showUsageDialog && !usageStatsGranted) {
        AlertDialog(
            onDismissRequest = { 
                showUsageDialog = false 
                hasDismissedDialog = true
            },
            title = { Text(s.get("usage_stats_dialog_title")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = s.get("usage_stats_dialog_desc"),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Row(
                        modifier = androidx.compose.ui.Modifier
                            .fillMaxWidth()
                            .background(
                                NeonBlue.copy(alpha = 0.1f),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PulsingDot(size = 10.dp)
                        Text(
                            text = s.get("usage_stats_dialog_guide"),
                            style = MaterialTheme.typography.bodySmall,
                            color = NeonBlue,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonBlue)
                ) {
                    Text(s.get("usage_stats_grant_btn"))
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showUsageDialog = false 
                    hasDismissedDialog = true
                }) {
                    Text(s.get("cancel"))
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = s.get("stats_title"),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Stat cards row 1
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(Modifier.weight(1f), s.get("current_streak"), "${state.currentStreak}", "🔥")
                StatCard(Modifier.weight(1f), s.get("longest_streak"), "${state.longestStreak}", "🏆")
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Stat cards row 2
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    Modifier.weight(1f), s.get("completion_rate"),
                    if (state.totalSessions > 0) "${(state.completionRate * 100).toInt()}%" else "—",
                    "📊"
                )
                StatCard(
                    Modifier.weight(1f), s.get("time_saved"),
                    formatTimeSaved(state.estimatedTimeSavedMinutes),
                    "⏱️"
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Heatmap
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = s.get("last_30_days"),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f)
                )
                if (!com.nichihab.app.ui.commercial.CommercialTierManager.canViewFullHistory()) {
                    Box(
                        modifier = Modifier
                            .clickable { showPaywall = true }
                            .background(Color(0xFFFFA500).copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            "PRO",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFA500)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            if (com.nichihab.app.ui.commercial.CommercialTierManager.canViewFullHistory()) {
                HeatmapCalendar(heatmapData = state.heatmapData)
                Spacer(modifier = Modifier.height(8.dp))
                // Legend
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(s.get("less"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    Spacer(modifier = Modifier.width(4.dp))
                    listOf(0, 1, 2, 3).forEach { level ->
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(heatmapColor(level))
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(s.get("more"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showPaywall = true }
                        .background(Color(0xFFFFA500).copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            s.get("nichihab_pro"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFA500)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            s.get("unlock_potential"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
}

@Composable
private fun HeatmapCalendar(heatmapData: Map<String, Int>) {
    val today = LocalDate.now()
    val days = (0..29).map { today.minusDays(29L - it) }
    val s = LocalStrings.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Day of week labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf(
                    s.get("day_mon_short"), s.get("day_tue_short"), s.get("day_wed_short"),
                    s.get("day_thu_short"), s.get("day_fri_short"), s.get("day_sat_short"), s.get("day_sun_short")
                ).forEach {
                    Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline, modifier = Modifier.width(32.dp), textAlign = TextAlign.Center)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))

            // Grid of day cells (5 rows x 7 cols for ~30 days, some empty)
            val formatter = DateTimeFormatter.ISO_LOCAL_DATE
            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                modifier = Modifier.fillMaxWidth().height(180.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                userScrollEnabled = false
            ) {
                // Pad the first row with empty cells based on first day's day-of-week
                val firstDayOfWeek = days.first().dayOfWeek.value // 1=Mon, 7=Sun
                val paddingCells = firstDayOfWeek - 1
                items(paddingCells) {
                    Box(modifier = Modifier.aspectRatio(1f))
                }
                items(days) { date ->
                    val dateStr = date.format(formatter)
                    val level = heatmapData[dateStr] ?: 0
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(4.dp))
                            .background(heatmapColor(level)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (date == today) {
                            Text(
                                text = "${date.dayOfMonth}",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 8.sp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun heatmapColor(level: Int) = when (level) {
    0 -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    1 -> StatusVerified.copy(alpha = 0.25f)
    2 -> StatusVerified.copy(alpha = 0.55f)
    3 -> StatusVerified
    else -> MaterialTheme.colorScheme.surfaceVariant
}


@Composable
private fun StatCard(modifier: Modifier, label: String, value: String, emoji: String) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(emoji, style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun formatTimeSaved(minutes: Long): String = when {
    minutes < 60 -> "${minutes}m"
    minutes < 1440 -> "${minutes / 60}h ${minutes % 60}m"
    else -> "${minutes / 1440}d ${(minutes % 1440) / 60}h"
}

/** Check if the app has usage stats permission */
private fun hasUsageStatsPermission(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager
        ?: return false
    val mode = appOps.unsafeCheckOpNoThrow(
        AppOpsManager.OPSTR_GET_USAGE_STATS,
        Process.myUid(),
        context.packageName
    )
    return mode == AppOpsManager.MODE_ALLOWED
}
