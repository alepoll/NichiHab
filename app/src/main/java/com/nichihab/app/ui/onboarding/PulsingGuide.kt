package com.nichihab.app.ui.onboarding

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Brand activation color used throughout the guided tutorial */
val NeonBlue = Color(0xFFF04B5F) // Updated to new Primary color
val NeonBlueGlow = Color(0xFFFF7080)

/**
 * A glowing, pulsing neon-blue dot used as a visual cue to guide the user.
 * Animates between scale 0.8–1.2 with a radiating glow halo.
 */
@Composable
fun PulsingDot(
    modifier: Modifier = Modifier,
    size: Dp = 16.dp,
    color: Color = NeonBlue
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulsingDot")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotScale"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotGlow"
    )

    Box(
        modifier = modifier
            .size(size * 2.5f)
            .scale(scale)
            .drawBehind {
                // Outer halo glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            color.copy(alpha = glowAlpha),
                            color.copy(alpha = glowAlpha * 0.5f),
                            Color.Transparent
                        )
                    ),
                    radius = this.size.minDimension / 2f
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // Core dot
        Box(
            modifier = Modifier
                .size(size)
                .background(color, CircleShape)
                .drawBehind {
                    drawCircle(
                        color = NeonBlueGlow.copy(alpha = 0.4f),
                        radius = this.size.minDimension / 2f + 4.dp.toPx()
                    )
                }
        )
    }
}

/**
 * Wraps content with a pulsing blue dot indicator at the specified position.
 * Used to draw attention to specific UI elements during the tutorial.
 */
@Composable
fun WithPulsingGuide(
    showGuide: Boolean,
    modifier: Modifier = Modifier,
    dotOffset: Pair<Dp, Dp> = Pair((-8).dp, (-8).dp),
    content: @Composable () -> Unit
) {
    Box(modifier = modifier) {
        content()
        if (showGuide) {
            PulsingDot(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = dotOffset.first, y = dotOffset.second),
                size = 12.dp
            )
        }
    }
}
