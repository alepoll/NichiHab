package com.nichihab.app.service.worker

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.nichihab.app.domain.model.Session
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

object NotificationScheduler {

    fun scheduleSessionNotifications(
        context: Context, 
        sessionId: Long, 
        dateStr: String, 
        startTimeHour: Int, 
        startTimeMinute: Int
    ) {
        val workManager = WorkManager.getInstance(context)

        try {
            val sessionDate = LocalDate.parse(dateStr)
            val sessionStartTime = LocalTime.of(startTimeHour, startTimeMinute)
            val sessionStartDateTime = LocalDateTime.of(sessionDate, sessionStartTime)
            val sessionStartMillis = sessionStartDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val nowMillis = System.currentTimeMillis()

            // 1. Reminder Worker (15 minutes before)
            val reminderTimeMillis = sessionStartMillis - TimeUnit.MINUTES.toMillis(15)
            val reminderDelay = reminderTimeMillis - nowMillis
            
            if (reminderDelay > 0) {
                val reminderRequest = OneTimeWorkRequestBuilder<SessionReminderWorker>()
                    .setInitialDelay(reminderDelay, TimeUnit.MILLISECONDS)
                    .build()
                
                workManager.enqueueUniqueWork(
                    SessionReminderWorker.WORK_NAME + "_${sessionId}",
                    ExistingWorkPolicy.REPLACE,
                    reminderRequest
                )
            }

            // 2. Start Worker (at start time)
            val startDelay = sessionStartMillis - nowMillis
            if (startDelay > 0) {
                val startRequest = OneTimeWorkRequestBuilder<SessionStartWorker>()
                    .setInitialDelay(startDelay, TimeUnit.MILLISECONDS)
                    .build()
                
                workManager.enqueueUniqueWork(
                    SessionStartWorker.WORK_NAME + "_${sessionId}",
                    ExistingWorkPolicy.REPLACE,
                    startRequest
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("NotificationScheduler", "Failed to schedule notifications", e)
        }
    }
}
