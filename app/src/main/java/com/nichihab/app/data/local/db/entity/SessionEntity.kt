package com.nichihab.app.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "sessions",
    indices = [androidx.room.Index(value = ["date"])]
)
data class SessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String, // ISO date "2026-05-07"
    val startTimeHour: Int,
    val startTimeMinute: Int,
    val endTimeHour: Int,
    val endTimeMinute: Int,
    val status: String = "locked", // "locked", "unlocked", "verified", "free"
    val goalType: String,
    val isRepeating: Boolean = true,
    val verifiedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    // Earned unlock time tracking
    val earnedSeconds: Long = 0,
    val usedSeconds: Long = 0,
    val unlockStartedAt: Long? = null,
    val editGracePeriodEndsAt: Long? = null
)
