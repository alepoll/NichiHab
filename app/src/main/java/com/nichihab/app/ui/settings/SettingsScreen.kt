package com.nichihab.app.ui.settings

import android.Manifest
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nichihab.app.util.AppStrings
import com.nichihab.app.util.Constants
import com.nichihab.app.util.LocalStrings
import com.nichihab.app.service.NichiHabDeviceAdmin
import androidx.compose.ui.platform.LocalContext

@OptIn(com.google.accompanist.permissions.ExperimentalPermissionsApi::class)
@Composable
fun SettingsScreen(
    onNavigateToBlockedApps: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val s = LocalStrings.current
    val context = LocalContext.current
    val isHardLocked = state.isSessionActive && state.strictnessMode == "hard" && !state.allTasksComplete
    val contentAlpha = if (isHardLocked) 0.45f else 1f

    // Device Admin state
    val dpm = remember { context.getSystemService(android.content.Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager }
    val adminComponent = remember { ComponentName(context, NichiHabDeviceAdmin::class.java) }
    var isDeviceAdminActive by remember { mutableStateOf(dpm.isAdminActive(adminComponent)) }

    val adminLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        isDeviceAdminActive = dpm.isAdminActive(adminComponent)
    }

    // Collapsible section states (collapsed by default)
    var permissionsExpanded by remember { mutableStateOf(false) }
    var appearanceExpanded by remember { mutableStateOf(false) }
    var languageExpanded by remember { mutableStateOf(false) }
    
    var showPaywall by remember { mutableStateOf(false) }
    if (showPaywall) {
        com.nichihab.app.ui.commercial.PaywallScreen(onDismiss = { showPaywall = false })
    }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    if (showPrivacyDialog) {
        val isEs = state.language == "es" || (state.language == "auto" && java.util.Locale.getDefault().language == "es")
        val privacyUrl = if (isEs) com.nichihab.app.util.Constants.PRIVACY_URL_ES else com.nichihab.app.util.Constants.PRIVACY_URL
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text(s.get("privacy_policy"), style = MaterialTheme.typography.titleLarge) },
            text = {
                Text(
                    text = s.get("privacy_policy_body"),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    showPrivacyDialog = false
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(privacyUrl))
                    context.startActivity(intent)
                }) {
                    Text(s.get("read_full_policy"))
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showPrivacyDialog = false }) {
                    Text(s.get("close"))
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text(
            text = s.get("settings_title"),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        // ── Hard Mode Lock Banner ──
        if (isHardLocked) {
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(s.get("hard_mode_locked_banner"), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Permissions Section (BLOCKED during Hard Mode) ──
        SectionHeader(title = s.get("permissions_title"), icon = Icons.Default.Security, expanded = permissionsExpanded, onClick = { permissionsExpanded = !permissionsExpanded })
        Spacer(modifier = Modifier.height(12.dp))
        AnimatedVisibility(visible = permissionsExpanded, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {

        val context = androidx.compose.ui.platform.LocalContext.current

        Column {
            // Hard Mode: block all permission changes (prevents navigating to system Settings)
            if (isHardLocked) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = s.get("permissions_locked_hard_mode"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    androidx.compose.material3.TextButton(
                        onClick = { showPrivacyDialog = true }
                    ) {
                        Text(s.get("privacy_policy"), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

        Card(
            modifier = Modifier.fillMaxWidth().alpha(if (isHardLocked) 0.45f else 1f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                PermissionItem(
                    title = s.get("permissions_accessibility"),
                    subtitle = s.get("permissions_accessibility_sub"),
                    isGranted = isAccessibilityServiceEnabled(context),
                    grantedLabel = s.get("permission_granted"),
                    deniedLabel = s.get("permission_tap_grant"),
                    onClick = {
                        if (!isHardLocked) {
                            val intent = android.content.Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)
                            context.startActivity(intent)
                        }
                    }
                )
                PermDivider()
                PermissionItem(
                    title = s.get("permissions_overlay"),
                    subtitle = s.get("permissions_overlay_sub"),
                    isGranted = android.provider.Settings.canDrawOverlays(context),
                    grantedLabel = s.get("permission_granted"),
                    deniedLabel = s.get("permission_tap_grant"),
                    onClick = {
                        if (!isHardLocked) {
                            val intent = android.content.Intent(
                                android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                android.net.Uri.parse("package:${context.packageName}")
                            )
                            context.startActivity(intent)
                        }
                    }
                )
                PermDivider()
                PermissionItem(
                    title = s.get("permissions_usage"),
                    subtitle = s.get("permissions_usage_sub"),
                    isGranted = isUsageStatsPermissionGranted(context),
                    grantedLabel = s.get("permission_granted"),
                    deniedLabel = s.get("permission_tap_grant"),
                    onClick = {
                        if (!isHardLocked) {
                            val intent = android.content.Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS)
                            context.startActivity(intent)
                        }
                    }
                )
                PermDivider()
                // Camera permission
                val hasCam = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED
                PermissionItem(
                    title = s.get("permissions_camera"),
                    subtitle = s.get("permissions_camera_sub"),
                    isGranted = hasCam,
                    grantedLabel = s.get("permission_granted"),
                    deniedLabel = s.get("permission_tap_grant"),
                    onClick = {
                        if (!isHardLocked) {
                            val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = android.net.Uri.parse("package:${context.packageName}")
                            }
                            context.startActivity(intent)
                        }
                    }
                )
                PermDivider()
                // Notification permission (Android 13+)
                val hasNotif = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
                } else true
                PermissionItem(
                    title = s.get("permissions_notifications"),
                    subtitle = s.get("permissions_notifications_sub"),
                    isGranted = hasNotif,
                    grantedLabel = s.get("permission_granted"),
                    deniedLabel = s.get("permission_tap_grant"),
                    onClick = {
                        if (!isHardLocked) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                val intent = android.content.Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                    putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
                                }
                                context.startActivity(intent)
                            }
                        }
                    }
                )
                PermDivider()
                // Battery optimization
                val powerManager = context.getSystemService(android.content.Context.POWER_SERVICE) as android.os.PowerManager
                val isBatteryOptimized = !powerManager.isIgnoringBatteryOptimizations(context.packageName)
                PermissionItem(
                    title = s.get("permissions_battery"),
                    subtitle = s.get("permissions_battery_sub"),
                    isGranted = !isBatteryOptimized,
                    grantedLabel = s.get("permission_granted"),
                    deniedLabel = s.get("permission_tap_grant"),
                    onClick = {
                        if (!isHardLocked) {
                            val intent = android.content.Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                data = android.net.Uri.parse("package:${context.packageName}")
                            }
                            context.startActivity(intent)
                        }
                    }
                )
            }
        }
        }

        }
        Spacer(modifier = Modifier.height(24.dp))

        // ── Appearance & Language are ALWAYS accessible (even during Hard Mode) ──

        // ── Appearance (always accessible, even during Hard Mode) ──
        SectionHeader(title = s.get("appearance_title"), expanded = appearanceExpanded, onClick = { appearanceExpanded = !appearanceExpanded })
        Spacer(modifier = Modifier.height(12.dp))
        AnimatedVisibility(visible = appearanceExpanded, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    listOf("system" to s.get("theme_system"), "dark" to s.get("theme_dark"), "light" to s.get("theme_light")).forEach { (value, label) ->
                        Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { viewModel.onThemeModeChanged(value) }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = state.themeMode == value, onClick = { viewModel.onThemeModeChanged(value) }, colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Language (always accessible, even during Hard Mode) ──
        SectionHeader(title = s.get("language_title"), icon = Icons.Default.Language, expanded = languageExpanded, onClick = { languageExpanded = !languageExpanded })
        Spacer(modifier = Modifier.height(12.dp))
        AnimatedVisibility(visible = languageExpanded, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    AppStrings.SUPPORTED_LANGUAGES.forEach { (code, label) ->
                        Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { viewModel.onLanguageChanged(code) }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = state.language == code, onClick = { viewModel.onLanguageChanged(code) }, colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── AI Provider, Notifications, Blocked Apps, Strictness are restricted ──
        Column(modifier = Modifier.alpha(contentAlpha)) {


            Spacer(modifier = Modifier.height(24.dp))

            // ── Notifications ──
            SectionHeader(title = s.get("notifications_title"))
            Spacer(modifier = Modifier.height(12.dp))
            val permissionState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                com.google.accompanist.permissions.rememberPermissionState(android.Manifest.permission.POST_NOTIFICATIONS)
            } else null

            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer), shape = RoundedCornerShape(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(s.get("session_reminders"), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                        Text(s.get("session_reminders_sub"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = state.notificationsEnabled,
                        onCheckedChange = { enabled ->
                            if (!isHardLocked) {
                                if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    val isGranted = androidx.core.content.ContextCompat.checkSelfPermission(
                                        context,
                                        android.Manifest.permission.POST_NOTIFICATIONS
                                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                    
                                    if (!isGranted) {
                                        permissionState?.launchPermissionRequest()
                                    }
                                }
                                viewModel.onNotificationsToggled(enabled)
                            }
                        },
                        enabled = !isHardLocked,
                        colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary, checkedThumbColor = MaterialTheme.colorScheme.onPrimary)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))



            // ── Strictness ──
            SectionHeader(title = s.get("strictness_title"), icon = Icons.Default.Shield)
            Spacer(modifier = Modifier.height(12.dp))
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer), shape = RoundedCornerShape(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        val isHardMode = state.strictnessMode == "hard"
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (isHardMode) s.get("hard_mode_active") else s.get("hard_mode"),
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (isHardMode) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                            )
                            if (!com.nichihab.app.ui.commercial.CommercialTierManager.canUseHardMode()) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "PRO",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                    color = androidx.compose.ui.graphics.Color(0xFFFFA500),
                                    modifier = Modifier
                                        .background(androidx.compose.ui.graphics.Color(0xFFFFA500).copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(s.get("hard_mode_sub"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = state.strictnessMode == "hard",
                        onCheckedChange = { enabled ->
                            if (enabled && !com.nichihab.app.ui.commercial.CommercialTierManager.canUseHardMode()) {
                                showPaywall = true
                                return@Switch
                            }
                            if (enabled) {
                                // Auto-activate Device Admin when enabling Strict Mode
                                if (!isDeviceAdminActive) {
                                    val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                                        putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
                                        putExtra(
                                            DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                                            s.get("device_admin_explanation")
                                        )
                                    }
                                    adminLauncher.launch(intent)
                                }
                                viewModel.onStrictnessModeChanged("hard")
                                isDeviceAdminActive = dpm.isAdminActive(adminComponent)
                            } else {
                                // Deactivate Device Admin when disabling Hard Mode
                                if (isDeviceAdminActive) {
                                    dpm.removeActiveAdmin(adminComponent)
                                    isDeviceAdminActive = false
                                }
                                viewModel.onStrictnessModeChanged("normal")
                            }
                        },
                        enabled = !(isHardLocked && state.strictnessMode == "hard"),
                        colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.error, checkedThumbColor = MaterialTheme.colorScheme.onError)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Spacer(modifier = Modifier.height(24.dp))

            // ── Privacy & Data ──
            SectionHeader(title = s.get("privacy_data"), icon = Icons.Default.Shield)
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(s.get("right_to_be_forgotten"), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
                    Text(s.get("delete_account_desc"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            showDeleteDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(s.get("delete_account_btn"))
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))

            // ── About ──
            Text(s.get("about_version"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline, modifier = Modifier.align(Alignment.CenterHorizontally))
            Spacer(modifier = Modifier.height(48.dp))
        }
    }

    if (showDeleteDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(s.get("delete_everything_title"), color = MaterialTheme.colorScheme.error) },
            text = { Text(s.get("delete_everything_body")) },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        val am = context.getSystemService(android.content.Context.ACTIVITY_SERVICE) as android.app.ActivityManager
                        am.clearApplicationUserData()
                    }
                ) {
                    Text(s.get("delete_forever"), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showDeleteDialog = false }) {
                    Text(s.get("cancel"))
                }
            }
        )
    }
}

@Composable
private fun PermDivider() {
    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
}

@Composable
private fun SectionHeader(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    expanded: Boolean? = null,
    onClick: (() -> Unit)? = null
) {
    val rotation by animateFloatAsState(
        targetValue = if (expanded == true) 90f else 0f,
        label = "chevronRotation"
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clip(RoundedCornerShape(8.dp)).clickable { onClick() } else Modifier)
            .padding(vertical = 4.dp)
    ) {
        if (icon != null) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(text = title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
        if (expanded != null) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = if (expanded) "Collapse" else "Expand",
                modifier = Modifier.size(18.dp).graphicsLayer { rotationZ = rotation },
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun PermissionItem(
    title: String, subtitle: String, isGranted: Boolean,
    grantedLabel: String = "Granted", deniedLabel: String = "Tap to grant",
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            // Status dot
            Box(
                modifier = Modifier.size(32.dp)
                    .background(if (isGranted) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.error.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isGranted) Icons.Default.Check else Icons.Default.PlayArrow,
                    contentDescription = null, modifier = Modifier.size(14.dp),
                    tint = if (isGranted) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        // Status chip
        Box(
            modifier = Modifier
                .background(
                    if (isGranted) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                    RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = if (isGranted) grantedLabel else deniedLabel,
                style = MaterialTheme.typography.labelSmall,
                color = if (isGranted) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
            )
        }
    }
}

private fun isAccessibilityServiceEnabled(context: android.content.Context): Boolean {
    val enabledServices = android.provider.Settings.Secure.getString(
        context.contentResolver, android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: return false
    return enabledServices.contains(context.packageName)
}

private fun isUsageStatsPermissionGranted(context: android.content.Context): Boolean {
    val appOps = context.getSystemService(android.content.Context.APP_OPS_SERVICE) as android.app.AppOpsManager
    val mode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
        appOps.unsafeCheckOpNoThrow(android.app.AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.packageName)
    } else {
        @Suppress("DEPRECATION")
        appOps.checkOpNoThrow(android.app.AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.packageName)
    }
    return mode == android.app.AppOpsManager.MODE_ALLOWED
}

@Composable
private fun ProviderOption(label: String, subtitle: String, selected: Boolean, enabled: Boolean = true, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .then(if (selected) Modifier.border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(12.dp)) else Modifier)
            .clickable(enabled = enabled, onClick = onClick).padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = { if (enabled) onClick() }, colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary))
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
