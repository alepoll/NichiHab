package com.nichihab.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.nichihab.app.data.local.db.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: TaskEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tasks: List<TaskEntity>)

    @Update
    suspend fun update(task: TaskEntity)

    @Delete
    suspend fun delete(task: TaskEntity)

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getById(id: Long): TaskEntity?

    @Query("SELECT * FROM tasks WHERE sessionId = :sessionId ORDER BY createdAt ASC")
    fun getTasksForSession(sessionId: Long): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE sessionId = :sessionId ORDER BY createdAt ASC")
    suspend fun getTasksForSessionOnce(sessionId: Long): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE goalType = :goalType ORDER BY createdAt DESC LIMIT 10")
    suspend fun getTasksByGoal(goalType: String): List<TaskEntity>

    @Query("UPDATE tasks SET isCompleted = 1, completedAt = :completedAt WHERE id = :taskId")
    suspend fun markCompleted(taskId: Long, completedAt: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM tasks WHERE sessionId = :sessionId AND isCompleted = 0")
    suspend fun getIncompleteCount(sessionId: Long): Int

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteById(id: Long)
}
