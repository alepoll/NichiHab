package com.nichihab.app.domain.repository

import com.nichihab.app.domain.model.Session
import kotlinx.coroutines.flow.Flow

interface SessionRepository {
    suspend fun insert(session: Session): Long
    suspend fun update(session: Session)
    suspend fun updateTimeWindow(
        sessionId: Long,
        startHour: Int, startMin: Int,
        endHour: Int, endMin: Int,
        status: String = "locked"
    )
    suspend fun getById(id: Long): Session?
    suspend fun getSessionForDate(date: String): Session?
    fun observeSessionForDate(date: String): Flow<Session?>
    suspend fun getLatestSession(): Session?
    fun observeAllSessions(): Flow<List<Session>>
    suspend fun getSessionsInRange(startDate: String, endDate: String): List<Session>
    suspend fun updateStatus(sessionId: Long, status: String, verifiedAt: Long? = null)
    suspend fun getVerifiedCount(): Int
    suspend fun getTotalEarnedSeconds(): Long
    // Earned unlock time
    suspend fun addEarnedSeconds(sessionId: Long, seconds: Long)
    suspend fun startUnlock(sessionId: Long, startedAt: Long)
    suspend fun stopUnlock(sessionId: Long, additionalUsed: Long)
    suspend fun pauseUnlockTimer(sessionId: Long, currentTime: Long)
    suspend fun resumeUnlockTimer(sessionId: Long, startedAt: Long)
    suspend fun setEditGracePeriod(sessionId: Long, endTime: Long?)
}
