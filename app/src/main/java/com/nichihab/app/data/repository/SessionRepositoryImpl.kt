package com.nichihab.app.data.repository

import com.nichihab.app.data.local.db.dao.SessionDao
import com.nichihab.app.data.local.db.entity.SessionEntity
import com.nichihab.app.domain.model.GoalType
import com.nichihab.app.domain.model.Session
import com.nichihab.app.domain.model.SessionStatus
import com.nichihab.app.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionRepositoryImpl @Inject constructor(
    private val sessionDao: SessionDao
) : SessionRepository {

    override suspend fun insert(session: Session): Long {
        return sessionDao.insert(session.toEntity())
    }

    override suspend fun update(session: Session) {
        sessionDao.update(session.toEntity())
    }

    override suspend fun updateTimeWindow(
        sessionId: Long,
        startHour: Int, startMin: Int,
        endHour: Int, endMin: Int,
        status: String
    ) {
        sessionDao.updateTimeWindow(sessionId, startHour, startMin, endHour, endMin, status)
    }

    override suspend fun getById(id: Long): Session? {
        return sessionDao.getById(id)?.toDomain()
    }

    override suspend fun getSessionForDate(date: String): Session? {
        return sessionDao.getSessionForDate(date)?.toDomain()
    }

    override fun observeSessionForDate(date: String): Flow<Session?> {
        return sessionDao.observeSessionForDate(date).map { it?.toDomain() }
    }

    override suspend fun getLatestSession(): Session? {
        return sessionDao.getLatestSession()?.toDomain()
    }

    override fun observeAllSessions(): Flow<List<Session>> {
        return sessionDao.observeAllSessions().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getSessionsInRange(startDate: String, endDate: String): List<Session> {
        return sessionDao.getSessionsInRange(startDate, endDate).map { it.toDomain() }
    }

    override suspend fun updateStatus(sessionId: Long, status: String, verifiedAt: Long?) {
        sessionDao.updateStatus(sessionId, status, verifiedAt)
    }

    override suspend fun getVerifiedCount(): Int {
        return sessionDao.getVerifiedCount()
    }

    override suspend fun getTotalEarnedSeconds(): Long {
        return sessionDao.getTotalEarnedSeconds() ?: 0L
    }

    override suspend fun addEarnedSeconds(sessionId: Long, seconds: Long) {
        sessionDao.addEarnedSeconds(sessionId, seconds)
    }

    override suspend fun startUnlock(sessionId: Long, startedAt: Long) {
        sessionDao.startUnlock(sessionId, startedAt)
    }

    override suspend fun stopUnlock(sessionId: Long, additionalUsed: Long) {
        sessionDao.stopUnlock(sessionId, additionalUsed)
    }

    override suspend fun pauseUnlockTimer(sessionId: Long, currentTime: Long) {
        sessionDao.pauseUnlockTimer(sessionId, currentTime)
    }

    override suspend fun resumeUnlockTimer(sessionId: Long, startedAt: Long) {
        sessionDao.resumeUnlockTimer(sessionId, startedAt)
    }

    override suspend fun setEditGracePeriod(sessionId: Long, endTime: Long?) {
        sessionDao.setEditGracePeriod(sessionId, endTime)
    }

    private fun Session.toEntity() = SessionEntity(
        id = id,
        date = date,
        startTimeHour = startTimeHour,
        startTimeMinute = startTimeMinute,
        endTimeHour = endTimeHour,
        endTimeMinute = endTimeMinute,
        status = status.value,
        goalType = goalType.name,
        isRepeating = isRepeating,
        earnedSeconds = earnedSeconds,
        usedSeconds = usedSeconds,
        unlockStartedAt = unlockStartedAt,
        editGracePeriodEndsAt = editGracePeriodEndsAt
    )

    private fun SessionEntity.toDomain() = Session(
        id = id,
        date = date,
        startTimeHour = startTimeHour,
        startTimeMinute = startTimeMinute,
        endTimeHour = endTimeHour,
        endTimeMinute = endTimeMinute,
        status = SessionStatus.fromString(status),
        goalType = GoalType.fromString(goalType),
        isRepeating = isRepeating,
        earnedSeconds = earnedSeconds,
        usedSeconds = usedSeconds,
        unlockStartedAt = unlockStartedAt,
        editGracePeriodEndsAt = editGracePeriodEndsAt
    )
}
