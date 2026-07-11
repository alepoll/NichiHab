package com.nichihab.app.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.nichihab.app.data.local.db.dao.BlockedAppDao
import com.nichihab.app.data.local.db.dao.SessionDao
import com.nichihab.app.data.local.db.dao.TaskDao
import com.nichihab.app.data.local.db.dao.VerificationDao
import com.nichihab.app.data.local.db.entity.BlockedAppEntity
import com.nichihab.app.data.local.db.entity.SessionEntity
import com.nichihab.app.data.local.db.entity.TaskEntity
import com.nichihab.app.data.local.db.entity.VerificationEntity

@Database(
    entities = [
        TaskEntity::class,
        SessionEntity::class,
        VerificationEntity::class,
        BlockedAppEntity::class
    ],
    version = 6,
    exportSchema = true
)
abstract class NichiHabDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun sessionDao(): SessionDao
    abstract fun verificationDao(): VerificationDao
    abstract fun blockedAppDao(): BlockedAppDao
}
