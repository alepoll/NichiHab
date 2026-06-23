package com.nichihab.app.ui.verification

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nichihab.app.domain.model.RewardTier
import com.nichihab.app.ui.theme.StatusLocked
import com.nichihab.app.ui.theme.StatusVerified
import com.nichihab.app.ui.theme.StreakGold
import com.nichihab.app.util.LocalStrings
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun VerificationResultScreen(
    verified: Boolean,
    reason: String,
    earnedMinutes: Int = 0,
    rewardTier: RewardTier = RewardTier.STANDARD,
    onGoHome: () -> Unit,
    onRetry: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val s = LocalStrings.current
    
    // Animation states
    val scale = remember { Animatable(0f) }
    val glowAlpha = remember { Animatable(0f) }
    var showBadge by remember { mutableStateOf(false) }
    var showPopup by remember { mutableStateOf(false) }
    val screenFlash = remember { Animatable(0f) }

    val bgColor = if (verified) rewardTier.color else StatusLocked

    LaunchedEffect(verified) {
        if (verified) {
            // Layer 1: Screen flash (0-50ms)
            launch {
                screenFlash.animateTo(0.2f, tween(50))
                screenFlash.animateTo(0f, tween(200))
            }
            
            // Layer 2: Enhanced haptic pattern (50-100ms)
            launch {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                if (rewardTier == RewardTier.EXCELLENT || rewardTier == RewardTier.PERFECT_FOCUS || rewardTier == RewardTier.LEGENDARY) {
                    delay(80)
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
                if (rewardTier == RewardTier.PERFECT_FOCUS || rewardTier == RewardTier.LEGENDARY) {
                    delay(100)
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
            }

            // Layer 3: Glow checkmark (100-300ms)
            launch {
                scale.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
                )
            }
            launch {
                delay(100)
                glowAlpha.animateTo(1f, tween(300))
                glowAlpha.animateTo(0.4f, tween(500))
            }

            // Layer 4: Floating badge (200-600ms)
            launch {
                delay(300)
                showBadge = true
            }

            // Layer 5: Tier-dependent popup (300-800ms)
            launch {
                delay(600)
                if (rewardTier != RewardTier.STANDARD) {
                    showPopup = true
                }
            }
        } else {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            scale.animateTo(
                targetValue = 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            
            // Icon with bounce animation and conditional glow
            Box(
                modifier = Modifier
                    .size(160.dp),
                contentAlignment = Alignment.Center
            ) {
                // Glow effect for verified
                if (verified) {
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .scale(scale.value)
                            .alpha(glowAlpha.value)
                            .background(bgColor.copy(alpha = 0.2f), CircleShape)
                    )
                    Box(
                        modifier = Modifier
                            .size(130.dp)
                            .scale(scale.value)
                            .alpha(glowAlpha.value * 0.8f)
                            .background(bgColor.copy(alpha = 0.3f), CircleShape)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .scale(scale.value)
                        .background(if (verified) bgColor else bgColor.copy(alpha = 0.25f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (verified) Icons.Default.Check else Icons.Default.Close,
                        contentDescription = if (verified) s.get("verified_title") else s.get("not_verified_title"),
                        modifier = Modifier.size(56.dp),
                        tint = if (verified) Color.White else bgColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = if (verified) s.get("task_verified_format").format(rewardTier.emoji) else s.get("not_verified_title"),
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = bgColor
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = if (verified) s.get("great_work_unlocked")
                else s.get("ai_could_not_confirm"),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Reward badge
            AnimatedVisibility(
                visible = showBadge && verified,
                enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { it / 2 }
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = StreakGold.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.padding(bottom = 24.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🔓", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = s.get("minutes_unlocked_format").format(earnedMinutes),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = StreakGold
                        )
                    }
                }
            }

            // AI response card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🤖", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = s.get("ai_analysis"),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = reason.ifBlank { if (verified) s.get("task_completed_successfully") else s.get("unable_to_verify") },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Actions
            if (verified) {
                Button(
                    onClick = onGoHome,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = bgColor),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(s.get("back_to_home"), style = MaterialTheme.typography.labelLarge, fontSize = 16.sp, color = Color.White)
                }
            } else {
                Button(
                    onClick = onRetry,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Refresh, null, modifier = Modifier.size(20.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(s.get("try_again"), style = MaterialTheme.typography.labelLarge, fontSize = 16.sp, color = Color.White)
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onGoHome,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(s.get("back_to_home"))
                }
            }
        }

        // Tier popup / Toast-style
        AnimatedVisibility(
            visible = showPopup,
            enter = fadeIn(tween(300)) + slideInVertically(tween(400)) { -it },
            exit = fadeOut(tween(300)),
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 48.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = rewardTier.color),
                shape = RoundedCornerShape(32.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(rewardTier.emoji, fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "${rewardTier.displayName} Focus!",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }
            }
        }

        // Screen Flash Overlay
        if (screenFlash.value > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = screenFlash.value))
            )
        }
    }
}
