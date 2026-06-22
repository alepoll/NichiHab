package com.nichihab.app.domain.model

import androidx.compose.ui.graphics.Color

/**
 * Reward tier system for task verification celebrations.
 * Tier is calculated at verification time based on AI confidence,
 * current streak length, and task difficulty.
 */
enum class RewardTier(
    val displayName: String,
    val emoji: String,
    val bonusMinutes: Int,
    val color: Color
) {
    STANDARD(
        displayName = "Standard",
        emoji = "✅",
        bonusMinutes = 0,
        color = Color(0xFF008E3E) // StatusVerified green
    ),
    EXCELLENT(
        displayName = "Excellent",
        emoji = "✨",
        bonusMinutes = 5,
        color = Color(0xFF00BCD4) // Cyan
    ),
    PERFECT_FOCUS(
        displayName = "Perfect Focus",
        emoji = "🎯",
        bonusMinutes = 10,
        color = Color(0xFF7C4DFF) // Deep purple
    ),
    LEGENDARY(
        displayName = "Legendary",
        emoji = "👑",
        bonusMinutes = 20,
        color = Color(0xFFFFB300) // StreakGold
    );

    companion object {
        /**
         * Calculate reward tier from verification context.
         * @param confidence AI verification confidence (0f..1f)
         * @param currentStreak consecutive days streak
         * @param taskEstimatedMinutes how long the task was expected to take
         */
        fun calculate(
            confidence: Float,
            currentStreak: Int,
            taskEstimatedMinutes: Int = 5
        ): RewardTier {
            // Score: confidence weight (0-40) + streak weight (0-40) + task difficulty (0-20)
            val confidenceScore = (confidence * 40f).toInt()
            val streakScore = (currentStreak.coerceAtMost(30) / 30f * 40f).toInt()
            val difficultyScore = (taskEstimatedMinutes.coerceAtMost(60) / 60f * 20f).toInt()
            val totalScore = confidenceScore + streakScore + difficultyScore

            return when {
                totalScore >= 85 -> LEGENDARY
                totalScore >= 65 -> PERFECT_FOCUS
                totalScore >= 40 -> EXCELLENT
                else -> STANDARD
            }
        }
    }
}
