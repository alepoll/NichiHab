package com.nichihab.app.service

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nichihab.app.data.local.db.NichiHabDatabase
import com.nichihab.app.ui.theme.NichiHabTheme
import com.nichihab.app.ui.theme.StatusLocked
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.temporal.ChronoUnit

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import javax.inject.Inject
import com.nichihab.app.data.local.datastore.SettingsDataStore
import com.nichihab.app.util.LocalStrings
import kotlinx.coroutines.flow.first

@AndroidEntryPoint
class BlockingOverlayActivity : ComponentActivity() {

    @Inject lateinit var settingsDataStore: SettingsDataStore
    @Inject lateinit var database: NichiHabDatabase

    companion object {
        const val EXTRA_SESSION_ID = "session_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Make it hard to dismiss
        setFinishOnTouchOutside(false)
        
        // Android 13+ back gesture handling
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            onBackInvokedDispatcher.registerOnBackInvokedCallback(
                android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT
            ) {
                // Do nothing to block back gesture
            }
        }

        val injectedDb = database

        setContent {
            NichiHabTheme(darkTheme = true) {
                BlockingOverlayContent(
                    database = injectedDb,
                    settingsDataStore = settingsDataStore,
                    onVerifyClick = {
                        // Action is handled inside the composable now
                    }
                )
            }

























        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Block back button
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}

@Composable
private fun BlockingOverlayContent(
    database: NichiHabDatabase,
    settingsDataStore: SettingsDataStore,
    onVerifyClick: () -> Unit
) {
    val s = LocalStrings.current
    var countdownText by remember { mutableStateOf("--:--") }
    var showCountdown by remember { mutableStateOf(true) }
    var secondsRemaining by remember { mutableLongStateOf(0L) }
    var currentTaskTitle by remember { mutableStateOf(s.get("complete_your_task")) }

    val context = androidx.compose.ui.platform.LocalContext.current
    var earnedTimeAvailable by remember { mutableLongStateOf(0L) }
    var sessionIdForUnlock by remember { mutableLongStateOf(0L) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                val intent = android.content.Intent(context, com.nichihab.app.MainActivity::class.java).apply {
                    putExtra("navigate_to", "camera")
                    flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                context.startActivity(intent)
                (context as? android.app.Activity)?.finish()
            } else {
                android.widget.Toast.makeText(context, s.get("camera_required_toast"), android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    )

    LaunchedEffect(Unit) {
        try {
            val today = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
            val session = database.sessionDao().getSessionForDate(today)
            if (session != null) {
                sessionIdForUnlock = session.id
                val tasks = database.taskDao().getTasksForSessionOnce(session.id)
                val pending = tasks.firstOrNull { !it.isCompleted }
                if (pending != null) {
                    currentTaskTitle = "${pending.emoji} ${pending.title}"
                    if (pending.scheduledEndHour == null) {
                        showCountdown = false
                    }
                }

                // Countdown + earned time logic
                val windowEnd = java.time.LocalTime.of(session.endTimeHour, session.endTimeMinute)
                while (true) {
                    // Re-fetch session to get latest earned/used
                    val freshSession = database.sessionDao().getById(session.id)
                    if (freshSession != null) {
                        val available = freshSession.earnedSeconds - freshSession.usedSeconds
                        earnedTimeAvailable = if (available > 0) available else 0

                        // Auto-dismiss if unlocked or verified
                        if (freshSession.status == "unlocked" || freshSession.status == "verified") {
                            (context as? android.app.Activity)?.finish()
                            return@LaunchedEffect
                        }
                    }

                    val now = java.time.LocalTime.now()
                    if (now.isBefore(windowEnd)) {
                        val remaining = now.until(windowEnd, java.time.temporal.ChronoUnit.SECONDS)
                        secondsRemaining = remaining
                        val hours = remaining / 3600
                        val minutes = (remaining % 3600) / 60
                        val secs = remaining % 60
                        countdownText = "%d:%02d:%02d".format(hours, minutes, secs)
                    } else {
                        countdownText = "0:00:00"
                    }
                    kotlinx.coroutines.delay(500) // Faster check
                }
            }
        } catch (e: Exception) {
            // Fallback
        }
    }

    // Check API key availability
    var hasApiKey by remember { mutableStateOf(true) } // default true to avoid flash
    LaunchedEffect(Unit) {
        try {
            val apiKey = settingsDataStore.apiKey.first()
            hasApiKey = apiKey.isNotBlank()
        } catch (_: Exception) {}
    }

    val pulse = rememberInfiniteTransition(label = "blockPulse")
    val pulseAlpha by pulse.animateFloat(
        0.6f, 1f,
        infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Reverse),
        label = "alpha"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Lock icon with pulse
        Icon(
            Icons.Default.Lock,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = StatusLocked.copy(alpha = pulseAlpha)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = s.get("blocked_by_app"),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = StatusLocked
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = s.get("restricted_during_session"),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = currentTaskTitle,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.SemiBold
        )
        
        Text(
            text = s.get("required_to_earn"),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        Text(
            text = s.get("unlock_minutes"),
            style = MaterialTheme.typography.titleMedium,
            color = androidx.compose.ui.graphics.Color(0xFF4CAF50),
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Countdown
        if (showCountdown) {
            Text(
                text = countdownText,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = s.get("until_window_ends"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Verify button
        Button(
            onClick = {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    val intent = android.content.Intent(context, com.nichihab.app.MainActivity::class.java).apply {
                        putExtra("navigate_to", "camera")
                        flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or 
                                android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP
                    }
                    context.startActivity(intent)
                    (context as? android.app.Activity)?.finish()
                } else {
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.CameraAlt, null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.size(8.dp))
            Text(s.get("verify_now"), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }

        // Use Earned Time button (only if time is available)
        if (earnedTimeAvailable > 0) {
            Spacer(modifier = Modifier.height(16.dp))
            val earnedMins = earnedTimeAvailable / 60
            val earnedSecs = earnedTimeAvailable % 60
            val scope = rememberCoroutineScope()
            Button(
                onClick = {
                    scope.launch {
                        try {
                            database.sessionDao().startUnlock(sessionIdForUnlock, System.currentTimeMillis())
                            (context as? android.app.Activity)?.finish()
                        } catch (_: Exception) {}
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = androidx.compose.ui.graphics.Color(0xFF4CAF50)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.LockOpen, null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.size(8.dp))
                Text(s.get("use_earned_time_format").format(earnedMins, earnedSecs), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
