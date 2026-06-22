package com.nichihab.app.ui.onboarding

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.nichihab.app.util.LocalStrings

/**
 * Mandatory system permissions page for onboarding.
 * Displays 3 sequential permission cards with deep-linking and visual guides.
 * Users cannot proceed until all 3 permissions are granted.
 */
@Composable
fun SystemPermissionsPage(
    onAllGranted: () -> Unit
) {
    val context = LocalContext.current
    val s = LocalStrings.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var overlayGranted by remember { mutableStateOf(checkOverlayPermission(context)) }
    var accessibilityGranted by remember { mutableStateOf(checkAccessibilityPermission(context)) }
    var batteryGranted by remember { mutableStateOf(checkBatteryOptimization(context)) }

    // Re-check permissions on resume (after user returns from system settings)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                overlayGranted = checkOverlayPermission(context)
                accessibilityGranted = checkAccessibilityPermission(context)
                batteryGranted = checkBatteryOptimization(context)
                if (overlayGranted && accessibilityGranted && batteryGranted) {
                    onAllGranted()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val allGranted = overlayGranted && accessibilityGranted && batteryGranted
    val grantedCount = listOf(overlayGranted, accessibilityGranted, batteryGranted).count { it }

    // Navigation trigger
    LaunchedEffect(allGranted) {
        if (allGranted) {
            onAllGranted()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = s.get("onboarding_perm_title"),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = s.get("onboarding_perm_subtitle"),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Progress indicator
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(3) { index ->
                val filled = index < grantedCount
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .background(
                            if (filled) NeonBlue else MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(2.dp)
                        )
                )
            }
        }
        Text(
            text = s.get("perm_step_label").replace("%d", "$grantedCount"),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.End
        )

        Spacer(modifier = Modifier.height(4.dp))

        // 1. Overlay Permission
        PermissionCard(
            icon = Icons.Default.Layers,
            title = s.get("perm_overlay_title"),
            description = s.get("perm_overlay_desc"),
            isGranted = overlayGranted,
            onGrant = {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                )
                context.startActivity(intent)
            }
        )

        // 2. Accessibility Service
        PermissionCard(
            icon = Icons.Default.Accessibility,
            title = s.get("perm_accessibility_title"),
            description = s.get("perm_accessibility_desc"),
            isGranted = accessibilityGranted,
            onGrant = {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                context.startActivity(intent)
            }
        )

        // 3. Battery Optimization
        PermissionCard(
            icon = Icons.Default.BatteryChargingFull,
            title = s.get("perm_battery_title"),
            description = s.get("perm_battery_desc"),
            isGranted = batteryGranted,
            onGrant = {
                val intent = Intent(
                    Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Uri.parse("package:${context.packageName}")
                )
                context.startActivity(intent)
            }
        )

        // Must-grant warning
        if (!allGranted) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        NeonBlue.copy(alpha = 0.08f),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                WithPulsingGuide(showGuide = true, dotOffset = Pair(0.dp, 0.dp)) {
                    // Empty — the dot is the visual cue
                }
                Text(
                    text = s.get("perm_must_grant_all"),
                    style = MaterialTheme.typography.bodySmall,
                    color = NeonBlue,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun PermissionCard(
    icon: ImageVector,
    title: String,
    description: String,
    isGranted: Boolean,
    onGrant: () -> Unit
) {
    val s = LocalStrings.current

    val scale by animateFloatAsState(
        targetValue = if (isGranted) 1f else 1f,
        animationSpec = spring(dampingRatio = 0.8f),
        label = "cardScale"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isGranted) NeonBlue
        else MaterialTheme.colorScheme.surfaceVariant,
        label = "borderColor"
    )
    val containerColor by animateColorAsState(
        targetValue = if (isGranted) NeonBlue.copy(alpha = 0.06f)
        else MaterialTheme.colorScheme.surfaceContainer,
        label = "containerColor"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isGranted) 2.dp else 1.dp,
            color = borderColor
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Icon with status badge
                Box {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                if (isGranted) NeonBlue.copy(alpha = 0.15f)
                                else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            icon, contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = if (isGranted) NeonBlue else MaterialTheme.colorScheme.primary
                        )
                    }
                    if (isGranted) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .align(Alignment.BottomEnd)
                                .background(NeonBlue, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Check, contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.surface
                            )
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isGranted) NeonBlue
                        else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (isGranted) {
                    Text(
                        text = s.get("permission_granted"),
                        style = MaterialTheme.typography.labelMedium,
                        color = NeonBlue,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (!isGranted) {
                Button(
                    onClick = onGrant,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonBlue
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = s.get("permission_grant_btn"),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

// ── Permission Check Helpers ──

fun checkOverlayPermission(context: Context): Boolean {
    return Settings.canDrawOverlays(context)
}

fun checkAccessibilityPermission(context: Context): Boolean {
    val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
        ?: return false
    val enabled = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
    return enabled.any {
        it.resolveInfo?.serviceInfo?.packageName == context.packageName
    }
}

fun checkBatteryOptimization(context: Context): Boolean {
    val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        ?: return false
    return pm.isIgnoringBatteryOptimizations(context.packageName)
}
