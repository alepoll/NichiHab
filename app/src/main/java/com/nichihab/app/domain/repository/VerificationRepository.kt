package com.nichihab.app.domain.repository

import com.nichihab.app.domain.model.Verification
import kotlinx.coroutines.flow.Flow

interface VerificationRepository {
    suspend fun insert(verification: Verification): Long
    suspend fun getForSession(sessionId: Long): List<Verification>
    suspend fun getForTask(taskId: Long): List<Verification>
    fun getRecentHistory(limit: Int = 50): Flow<List<Verification>>
    suspend fun getAttemptCount(taskId: Long, sessionId: Long): Int
    suspend fun getLastFailedAttemptTime(taskId: Long, sessionId: Long): Long?
}
