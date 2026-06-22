package com.nichihab.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.nichihab.app.data.local.db.entity.SessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: SessionEntity): Long

    @Update
    suspend fun update(session: SessionEntity)

    @Query("""
        UPDATE sessions 
        SET startTimeHour = :startHour, startTimeMinute = :startMin,
            endTimeHour = :endHour, endTimeMinute = :endMin, status = :status
        WHERE id = :sessionId
    """)
    suspend fun updateTimeWindow(
        sessionId: Long,
        startHour: Int, startMin: Int,
        endHour: Int, endMin: Int,
        status: String = "locked"
    )

    @Query("SELECT * FROM sessions WHERE id = :id")
    suspend fun getById(id: Long): SessionEntity?

    @Query("SELECT * FROM sessions WHERE date = :date LIMIT 1")
    suspend fun getSessionForDate(date: String): SessionEntity?

    @Query("SELECT * FROM sessions WHERE date = :date LIMIT 1")
    fun observeSessionForDate(date: String): Flow<SessionEntity?>

    @Query("SELECT * FROM sessions ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatestSession(): SessionEntity?

    @Query("SELECT * FROM sessions ORDER BY date DESC")
    fun observeAllSessions(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    suspend fun getSessionsInRange(startDate: String, endDate: String): List<SessionEntity>

    @Query("UPDATE sessions SET status = :status, verifiedAt = :verifiedAt WHERE id = :sessionId")
    suspend fun updateStatus(sessionId: Long, status: String, verifiedAt: Long? = null)

    @Query("SELECT COUNT(*) FROM sessions WHERE status = 'verified'")
    suspend fun getVerifiedCount(): Int

    @Query("SELECT SUM(earnedSeconds) FROM sessions")
    suspend fun getTotalEarnedSeconds(): Long?

    // ── Earned unlock time queries ──

    @Query("UPDATE sessions SET earnedSeconds = earnedSeconds + :seconds WHERE id = :sessionId")
    suspend fun addEarnedSeconds(sessionId: Long, seconds: Long)

    @Query("UPDATE sessions SET status = 'unlocked', unlockStartedAt = :startedAt WHERE id = :sessionId")
    suspend fun startUnlock(sessionId: Long, startedAt: Long)

    @Query("UPDATE sessions SET status = 'locked', usedSeconds = usedSeconds + :additionalUsed, unlockStartedAt = NULL WHERE id = :sessionId")
    suspend fun stopUnlock(sessionId: Long, additionalUsed: Long)

    @Query("UPDATE sessions SET usedSeconds = usedSeconds + ((:currentTime - unlockStartedAt) / 1000), unlockStartedAt = NULL WHERE id = :sessionId AND unlockStartedAt IS NOT NULL")
    suspend fun pauseUnlockTimer(sessionId: Long, currentTime: Long)

    @Query("UPDATE sessions SET unlockStartedAt = :startedAt WHERE id = :sessionId")
    suspend fun resumeUnlockTimer(sessionId: Long, startedAt: Long)

    @Query("UPDATE sessions SET editGracePeriodEndsAt = :endTime WHERE id = :sessionId")
    suspend fun setEditGracePeriod(sessionId: Long, endTime: Long?)
}
