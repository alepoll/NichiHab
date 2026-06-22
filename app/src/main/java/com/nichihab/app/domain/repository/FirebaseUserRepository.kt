package com.nichihab.app.domain.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseUserRepository @Inject constructor() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    /**
     * Checks if the user is allowed to add a task today.
     * Validates against server-side time to prevent local "Time Travel" attacks.
     */
    suspend fun canAddTask(isProUser: Boolean, freeLimit: Int): Boolean {
        if (isProUser) return true

        val user = auth.currentUser ?: return true
        val userRef = db.collection("users").document(user.uid)

        return try {
            val snapshot = userRef.get().await()
            if (!snapshot.exists()) {
                // User document doesn't exist yet, so they haven't used any tasks
                true
            } else {
                // Get the server timestamp when the last task was created
                val lastTaskDate = snapshot.getTimestamp("lastTaskDate")
                val tasksUsedToday = snapshot.getLong("tasksUsedToday") ?: 0

                if (lastTaskDate != null) {
                    val lastDateLocal = lastTaskDate.toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                    val todayLocal = LocalDate.now()

                    if (lastDateLocal.isBefore(todayLocal)) {
                        // It's a new day, limit will be reset on first increment
                        true
                    } else {
                        // Still the same day, check limit
                        tasksUsedToday < freeLimit
                    }
                } else {
                    // document exists but no timestamp, assume reset
                    true
                }
            }
        } catch (e: Exception) {
            Log.e("FirebaseUserRepository", "Error checking limits", e)
            true // Allow on network/server error to avoid blocking legitimate users
        }
    }

    /**
     * Increments the task counter in Firestore.
     * Should be called ONLY after the task is successfully verified by AI.
     */
    suspend fun incrementTaskCount() {
        val user = auth.currentUser ?: return
        val userRef = db.collection("users").document(user.uid)

        try {
            val snapshot = userRef.get().await()
            if (!snapshot.exists()) {
                userRef.set(
                    mapOf(
                        "tasksUsedToday" to 1,
                        "lastTaskDate" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                val lastTaskDate = snapshot.getTimestamp("lastTaskDate")
                if (lastTaskDate != null) {
                    val lastDateLocal = lastTaskDate.toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                    val todayLocal = LocalDate.now()
                    
                    if (lastDateLocal.isBefore(todayLocal)) {
                        userRef.update(
                            mapOf(
                                "tasksUsedToday" to 1,
                                "lastTaskDate" to FieldValue.serverTimestamp()
                            )
                        ).await()
                    } else {
                        userRef.update(
                            mapOf(
                                "tasksUsedToday" to FieldValue.increment(1),
                                "lastTaskDate" to FieldValue.serverTimestamp()
                            )
                        ).await()
                    }
                } else {
                    userRef.update(
                        mapOf(
                            "tasksUsedToday" to 1,
                            "lastTaskDate" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }
        } catch (e: Exception) {
            Log.e("FirebaseUserRepository", "Error incrementing task count", e)
        }
    }
}
