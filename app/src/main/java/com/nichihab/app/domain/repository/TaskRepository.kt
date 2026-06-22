package com.nichihab.app.domain.repository

import com.nichihab.app.domain.model.Task
import kotlinx.coroutines.flow.Flow

interface TaskRepository {
    suspend fun insert(task: Task): Long
    suspend fun insertAll(tasks: List<Task>)
    suspend fun update(task: Task)
    suspend fun delete(task: Task)
    suspend fun deleteById(id: Long)
    suspend fun getById(id: Long): Task?
    fun getTasksForSession(sessionId: Long): Flow<List<Task>>
    suspend fun getTasksForSessionOnce(sessionId: Long): List<Task>
    suspend fun getTasksByGoal(goalType: String): List<Task>
    suspend fun markCompleted(taskId: Long)
    suspend fun getIncompleteCount(sessionId: Long): Int
}
