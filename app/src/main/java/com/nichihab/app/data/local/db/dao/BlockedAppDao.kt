package com.nichihab.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.nichihab.app.data.local.db.entity.BlockedAppEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockedAppDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(app: BlockedAppEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(apps: List<BlockedAppEntity>)

    @Update
    suspend fun update(app: BlockedAppEntity)

    @Query("DELETE FROM blocked_apps WHERE packageName = :packageName")
    suspend fun delete(packageName: String)

    @Query("SELECT * FROM blocked_apps WHERE isBlocked = 1 ORDER BY appName ASC")
    fun getAllBlocked(): Flow<List<BlockedAppEntity>>

    @Query("SELECT * FROM blocked_apps ORDER BY appName ASC")
    fun getAll(): Flow<List<BlockedAppEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM blocked_apps WHERE packageName = :packageName AND isBlocked = 1)")
    suspend fun isBlocked(packageName: String): Boolean
}
