package com.nichihab.app.service.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nichihab.app.data.local.db.NichiHabDatabase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Periodic worker that ensures today's session exists.
 * This guarantees blocking activates at the designated time even if
 * the user never opens the app that day.
 */
@HiltWorker
class SessionAutoStartWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val database: NichiHabDatabase
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "session_auto_start"
    }

    override suspend fun doWork(): Result {
        try {
            val latestSession = database.sessionDao().getLatestSession()
            val logicalToday = com.nichihab.app.util.TimeUtil.getLogicalDate(latestSession)
            val session = database.sessionDao().getSessionForDate(logicalToday)

            if (session == null) {
                // Auto-create from latest repeating session
                if (latestSession != null && latestSession.isRepeating) {
                    val newId = database.sessionDao().insert(
                        latestSession.copy(
                            id = 0,
                            date = logicalToday,
                            status = "locked",
                            verifiedAt = null,
                            earnedSeconds = 0,
                            usedSeconds = 0,
                            unlockStartedAt = null
                        )
                    )
                    val tasks = database.taskDao().getTasksForSessionOnce(latestSession.id)
                    database.taskDao().insertAll(
                        tasks.map {
                            it.copy(
                                id = 0,
                                sessionId = newId,
                                isCompleted = false,
                                completedAt = null
                            )
                        }
                    )
                    android.util.Log.d("NichiHab", "AutoStart: Created session for $logicalToday from template ${latestSession.id}")

                    val createdSession = database.sessionDao().getSessionForDate(logicalToday)
                    if (createdSession != null) {
                        NotificationScheduler.scheduleSessionNotifications(
                            applicationContext,
                            createdSession.id,
                            createdSession.date,
                            createdSession.startTimeHour,
                            createdSession.startTimeMinute
                        )
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("NichiHab", "AutoStart worker error", e)
        }
        return Result.success()
    }
}
