package com.nichihab.app.service.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nichihab.app.MainActivity
import com.nichihab.app.data.local.db.NichiHabDatabase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@HiltWorker
class SessionReminderWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val database: NichiHabDatabase
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val CHANNEL_ID = "NichiHab_reminders"
        const val NOTIFICATION_ID = 1001
        const val WORK_NAME = "session_reminder"
    }

    override suspend fun doWork(): Result {
        createNotificationChannel()

        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val session = database.sessionDao().getSessionForDate(today)

        val todayDow = LocalDate.now().dayOfWeek.value.toString()

        if (session != null && session.status != "verified") {
            val tasks = database.taskDao().getTasksForSessionOnce(session.id)
                .filter { !it.isCompleted && (it.scheduledDays.contains(todayDow) || it.isTodayOnly) }
                
            val taskListText = if (tasks.isNotEmpty()) {
                val names = tasks.take(3).joinToString(", ") { "${it.emoji} ${it.title}" }
                if (tasks.size > 3) "$names +${tasks.size - 3} more" else names
            } else ""

            val contentText = if (taskListText.isNotEmpty()) {
                "Ready to lock in? Tasks: $taskListText"
            } else {
                "Your focus window starts soon. Ready to lock in?"
            }

            val intent = Intent(applicationContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pendingIntent = PendingIntent.getActivity(
                applicationContext, 0, intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_lock)
                .setContentTitle("🔒 Focus window starts soon")
                .setContentText(contentText)
                .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

            val manager = applicationContext.getSystemService(NotificationManager::class.java)
            manager.notify(NOTIFICATION_ID, notification)
        }

        return Result.success()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Session Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders before focus windows start"
            }
            val manager = applicationContext.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}

@HiltWorker
class SessionStartWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val database: NichiHabDatabase
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val CHANNEL_ID = "NichiHab_session"
        const val NOTIFICATION_ID = 1002
        const val WORK_NAME = "session_start"
    }

    override suspend fun doWork(): Result {
        createNotificationChannel()

        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val todayDow = LocalDate.now().dayOfWeek.value.toString()
        val session = database.sessionDao().getSessionForDate(today)
        val incompleteTasks = session?.let { s -> 
            database.taskDao().getTasksForSessionOnce(s.id)
                .filter { !it.isCompleted && (it.scheduledDays.contains(todayDow) || it.isTodayOnly) }
        } ?: emptyList()
        val incompleteCount = incompleteTasks.size

        if (session != null && session.status != "verified" && incompleteCount > 0) {
            val taskListText = incompleteTasks.take(3).joinToString(", ") { "${it.emoji} ${it.title}" }
            val suffix = if (incompleteCount > 3) " +${incompleteCount - 3} more" else ""
            val contentText = "Complete to unlock: $taskListText$suffix"

            val intent = Intent(applicationContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pendingIntent = PendingIntent.getActivity(
                applicationContext, 0, intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_lock)
                .setContentTitle("🔒 NichiHab is active")
                .setContentText(contentText)
                .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setOngoing(true)
                .build()

            val manager = applicationContext.getSystemService(NotificationManager::class.java)
            manager.notify(NOTIFICATION_ID, notification)
        }

        return Result.success()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Active Sessions",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for active focus sessions"
            }
            val manager = applicationContext.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}

@HiltWorker
class StreakMilestoneWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val database: NichiHabDatabase
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val CHANNEL_ID = "NichiHab_achievements"
        const val NOTIFICATION_ID = 1003
        const val WORK_NAME = "streak_milestone"
    }

    override suspend fun doWork(): Result {
        createNotificationChannel()

        val verifiedCount = database.sessionDao().getVerifiedCount()
        val milestones = listOf(3, 7, 14, 21, 30, 50, 100)

        if (verifiedCount in milestones) {
            val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_lock)
                .setContentTitle("🔥 $verifiedCount-day streak!")
                .setContentText("You're building a real habit. Keep it going!")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .build()

            val manager = applicationContext.getSystemService(NotificationManager::class.java)
            manager.notify(NOTIFICATION_ID, notification)
        }

        return Result.success()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Achievements",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Streak milestones and achievements"
            }
            val manager = applicationContext.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}
