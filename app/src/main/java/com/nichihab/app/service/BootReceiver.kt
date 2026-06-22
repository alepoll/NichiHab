package com.nichihab.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.nichihab.app.data.local.db.NichiHabDatabase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var database: NichiHabDatabase

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val latest = database.sessionDao().getLatestSession()
                val logicalToday = com.nichihab.app.util.TimeUtil.getLogicalDate(latest)
                val session = database.sessionDao().getSessionForDate(logicalToday)

                if (session == null) {
                    // Auto-create session from latest repeating template on boot
                    if (latest != null && latest.isRepeating) {
                        val newId = database.sessionDao().insert(
                            latest.copy(
                                id = 0, date = logicalToday, status = "locked",
                                verifiedAt = null, earnedSeconds = 0,
                                usedSeconds = 0, unlockStartedAt = null
                            )
                        )
                        val tasks = database.taskDao().getTasksForSessionOnce(latest.id)
                        database.taskDao().insertAll(
                            tasks.map {
                                it.copy(id = 0, sessionId = newId, isCompleted = false, completedAt = null)
                            }
                        )
                        android.util.Log.d("NichiHab", "Boot: Created session for $logicalToday")
                    }
                } else if (session.status != "verified") {
                    android.util.Log.d("NichiHab", "Boot: Active session found, blocking will resume")
                }
            } catch (e: Exception) {
                android.util.Log.e("NichiHab", "Boot receiver error", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
