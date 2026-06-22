package com.nichihab.app.domain.model

enum class GoalType(val displayNameKey: String, val emoji: String, val subtitleKey: String) {
    MORNING_ROUTINE("goal_morning_routine", "🌅", "goal_morning_routine_sub"),
    DEEP_WORK("goal_deep_work", "📚", "goal_deep_work_sub"),
    FITNESS("goal_fitness", "💪", "goal_fitness_sub"),
    CUSTOM("goal_custom", "⚡", "goal_custom_sub");

    companion object {
        fun fromString(value: String): GoalType {
            return entries.find { it.name.equals(value, ignoreCase = true) }
                ?: CUSTOM
        }
    }
}

enum class SessionStatus(val value: String) {
    LOCKED("locked"),
    UNLOCKED("unlocked"),   // actively spending earned time
    VERIFIED("verified"),   // all tasks done → full day unlock
    FREE("free");

    companion object {
        fun fromString(value: String): SessionStatus {
            return entries.find { it.value.equals(value, ignoreCase = true) }
                ?: FREE
        }
    }
}

data class Task(
    val id: Long = 0,
    val title: String,
    val emoji: String = "✅",
    val goalType: GoalType = GoalType.CUSTOM,
    val isCustom: Boolean = false,
    val estimatedMinutes: Int = 5,
    val rewardMinutes: Int = 10, // minutes of unlock time earned on completion
    val sessionId: Long = 0,
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    // Day/time scheduling: days = 1(Mon)..7(Sun), null times = use session time
    val scheduledDays: List<Int> = listOf(1, 2, 3, 4, 5, 6, 7),
    val scheduledStartHour: Int? = null,
    val scheduledStartMinute: Int? = null,
    val scheduledEndHour: Int? = null,
    val scheduledEndMinute: Int? = null,
    // One-time tasks: only valid for the day they were created
    val isTodayOnly: Boolean = false,
    val createdDate: String? = null, // ISO date string for today-only expiration
    // AI Photo suggestion
    val photoHint: String? = null
)

data class Session(
    val id: Long = 0,
    val date: String,
    val startTimeHour: Int,
    val startTimeMinute: Int,
    val endTimeHour: Int,
    val endTimeMinute: Int,
    val status: SessionStatus = SessionStatus.LOCKED,
    val goalType: GoalType = GoalType.CUSTOM,
    val isRepeating: Boolean = true,
    val tasks: List<Task> = emptyList(),
    // Earned unlock time tracking
    val earnedSeconds: Long = 0,        // total seconds earned from tasks
    val usedSeconds: Long = 0,          // total seconds already consumed
    val unlockStartedAt: Long? = null,  // millis when user started spending time (null = not spending)
    val editGracePeriodEndsAt: Long? = null
)

data class Verification(
    val id: Long = 0,
    val taskId: Long,
    val sessionId: Long,
    val photoPath: String = "",
    val aiResponse: String = "",
    val isVerified: Boolean = false,
    val confidence: Float = 0f,
    val attemptNumber: Int = 1,
    val provider: String = "gemini",
    val timestamp: Long = System.currentTimeMillis()
)

data class BlockedApp(
    val packageName: String,
    val appName: String,
    val isBlocked: Boolean = true
)

data class TimeWindow(
    val label: String,
    val emoji: String,
    val startHour: Int,
    val startMinute: Int = 0,
    val endHour: Int,
    val endMinute: Int = 0,
    val isCustom: Boolean = false
) {
    companion object {
        val PRESETS = listOf(
            TimeWindow("Morning routine", "🌅", 6, 0, 9, 0),
            TimeWindow("Study block", "📚", 9, 0, 11, 0),
            TimeWindow("Workout window", "💪", 7, 0, 8, 0),
            TimeWindow("Evening wind-down", "🌙", 20, 0, 22, 0),
            TimeWindow("Custom schedule", "⚡", 0, 0, 0, 0, isCustom = true)
        )
    }
}


data class AiTaskSuggestion(
    val task: String,
    val emoji: String,
    val estimatedMinutes: Int,
    val photoHint: String? = null
)
