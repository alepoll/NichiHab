package com.nichihab.app.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "tasks",
    indices = [androidx.room.Index(value = ["sessionId"])]
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val emoji: String = "✅",
    val goalType: String, // "morning_routine", "deep_work", "fitness", "custom"
    val isCustom: Boolean = false,
    val estimatedMinutes: Int = 5,
    val rewardMinutes: Int = 10, // minutes of unlock time earned on completion
    val sessionId: Long = 0,
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    // Day/time scheduling
    val scheduledDays: String = "1,2,3,4,5,6,7", // comma-separated day-of-week (1=Mon..7=Sun)
    val scheduledStartHour: Int? = null,
    val scheduledStartMinute: Int? = null,
    val scheduledEndHour: Int? = null,
    val scheduledEndMinute: Int? = null,
    // One-time tasks
    val isTodayOnly: Boolean = false,
    val createdDate: String? = null, // ISO date for today-only expiration
    // AI Photo suggestion
    val photoHint: String? = null
)
