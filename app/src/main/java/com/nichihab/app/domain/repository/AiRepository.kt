package com.nichihab.app.domain.repository

import com.nichihab.app.domain.model.AiTaskSuggestion

interface AiRepository {
    suspend fun verifyTask(photoBase64: String, taskDescription: String): AiVerificationResult
    suspend fun suggestTasks(goal: String): List<AiTaskSuggestion>
}

data class AiVerificationResult(
    val isVerified: Boolean,
    val reason: String,
    val confidence: Float = 0f,
    val rawResponse: String = ""
)
