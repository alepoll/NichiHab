package com.nichihab.app.data.repository

import com.nichihab.app.data.local.db.dao.VerificationDao
import com.nichihab.app.data.local.db.entity.VerificationEntity
import com.nichihab.app.domain.model.Verification
import com.nichihab.app.domain.repository.VerificationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VerificationRepositoryImpl @Inject constructor(
    private val verificationDao: VerificationDao
) : VerificationRepository {

    override suspend fun insert(verification: Verification): Long {
        return verificationDao.insert(verification.toEntity())
    }

    override suspend fun getForSession(sessionId: Long): List<Verification> {
        return verificationDao.getForSession(sessionId).map { it.toDomain() }
    }

    override suspend fun getForTask(taskId: Long): List<Verification> {
        return verificationDao.getForTask(taskId).map { it.toDomain() }
    }

    override fun getRecentHistory(limit: Int): Flow<List<Verification>> {
        return verificationDao.getRecentHistory(limit).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getAttemptCount(taskId: Long, sessionId: Long): Int {
        return verificationDao.getAttemptCount(taskId, sessionId)
    }

    override suspend fun getLastFailedAttemptTime(taskId: Long, sessionId: Long): Long? {
        return verificationDao.getLastFailedAttemptTime(taskId, sessionId)
    }

    private fun Verification.toEntity() = VerificationEntity(
        id = id,
        taskId = taskId,
        sessionId = sessionId,
        photoPath = photoPath,
        aiResponse = aiResponse,
        isVerified = isVerified,
        confidence = confidence,
        attemptNumber = attemptNumber,
        provider = provider,
        timestamp = timestamp
    )

    private fun VerificationEntity.toDomain() = Verification(
        id = id,
        taskId = taskId,
        sessionId = sessionId,
        photoPath = photoPath,
        aiResponse = aiResponse,
        isVerified = isVerified,
        confidence = confidence,
        attemptNumber = attemptNumber,
        provider = provider,
        timestamp = timestamp
    )
}
