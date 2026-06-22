package com.nichihab.app.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "verifications")
data class VerificationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val taskId: Long,
    val sessionId: Long,
    val photoPath: String = "",
    val aiResponse: String = "",
    val isVerified: Boolean = false,
    val confidence: Float = 0f,
    val attemptNumber: Int = 1,
    val provider: String = "anthropic", // "anthropic" or "gemini"
    val timestamp: Long = System.currentTimeMillis()
)
