package com.nichihab.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nichihab.app.data.local.db.entity.VerificationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VerificationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(verification: VerificationEntity): Long

    @Query("SELECT * FROM verifications WHERE sessionId = :sessionId ORDER BY timestamp DESC")
    suspend fun getForSession(sessionId: Long): List<VerificationEntity>

    @Query("SELECT * FROM verifications WHERE taskId = :taskId ORDER BY timestamp DESC")
    suspend fun getForTask(taskId: Long): List<VerificationEntity>

    @Query("SELECT * FROM verifications ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentHistory(limit: Int = 50): Flow<List<VerificationEntity>>

    @Query("SELECT COUNT(*) FROM verifications WHERE taskId = :taskId AND sessionId = :sessionId")
    suspend fun getAttemptCount(taskId: Long, sessionId: Long): Int

    @Query("SELECT MAX(timestamp) FROM verifications WHERE taskId = :taskId AND sessionId = :sessionId AND isVerified = 0")
    suspend fun getLastFailedAttemptTime(taskId: Long, sessionId: Long): Long?
}
