package com.nichihab.app.data.repository

import com.nichihab.app.data.local.db.dao.TaskDao
import com.nichihab.app.data.local.db.entity.TaskEntity
import com.nichihab.app.domain.model.GoalType
import com.nichihab.app.domain.model.Task
import com.nichihab.app.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepositoryImpl @Inject constructor(
    private val taskDao: TaskDao
) : TaskRepository {

    override suspend fun insert(task: Task): Long {
        return taskDao.insert(task.toEntity())
    }

    override suspend fun insertAll(tasks: List<Task>) {
        taskDao.insertAll(tasks.map { it.toEntity() })
    }

    override suspend fun update(task: Task) {
        taskDao.update(task.toEntity())
    }

    override suspend fun delete(task: Task) {
        taskDao.delete(task.toEntity())
    }

    override suspend fun deleteById(id: Long) {
        taskDao.deleteById(id)
    }

    override suspend fun getById(id: Long): Task? {
        return taskDao.getById(id)?.toDomain()
    }

    override fun getTasksForSession(sessionId: Long): Flow<List<Task>> {
        return taskDao.getTasksForSession(sessionId).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getTasksForSessionOnce(sessionId: Long): List<Task> {
        return taskDao.getTasksForSessionOnce(sessionId).map { it.toDomain() }
    }

    override suspend fun getTasksByGoal(goalType: String): List<Task> {
        return taskDao.getTasksByGoal(goalType).map { it.toDomain() }
    }

    override suspend fun markCompleted(taskId: Long) {
        taskDao.markCompleted(taskId)
    }

    override suspend fun getIncompleteCount(sessionId: Long): Int {
        return taskDao.getIncompleteCount(sessionId)
    }

    private fun Task.toEntity() = TaskEntity(
        id = id,
        title = title,
        emoji = emoji,
        goalType = goalType.name,
        isCustom = isCustom,
        estimatedMinutes = estimatedMinutes,
        rewardMinutes = rewardMinutes,
        sessionId = sessionId,
        isCompleted = isCompleted,
        completedAt = completedAt,
        createdAt = createdAt,
        scheduledDays = scheduledDays.joinToString(","),
        scheduledStartHour = scheduledStartHour,
        scheduledStartMinute = scheduledStartMinute,
        scheduledEndHour = scheduledEndHour,
        scheduledEndMinute = scheduledEndMinute,
        isTodayOnly = isTodayOnly,
        createdDate = createdDate,
        photoHint = photoHint
    )

    private fun TaskEntity.toDomain() = Task(
        id = id,
        title = title,
        emoji = emoji,
        goalType = GoalType.fromString(goalType),
        isCustom = isCustom,
        estimatedMinutes = estimatedMinutes,
        rewardMinutes = rewardMinutes,
        sessionId = sessionId,
        isCompleted = isCompleted,
        completedAt = completedAt,
        createdAt = createdAt,
        scheduledDays = scheduledDays.split(",").mapNotNull { it.trim().toIntOrNull() },
        scheduledStartHour = scheduledStartHour,
        scheduledStartMinute = scheduledStartMinute,
        scheduledEndHour = scheduledEndHour,
        scheduledEndMinute = scheduledEndMinute,
        isTodayOnly = isTodayOnly,
        createdDate = createdDate,
        photoHint = photoHint
    )
}
