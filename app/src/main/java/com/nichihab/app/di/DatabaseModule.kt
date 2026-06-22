package com.nichihab.app.di

import android.content.Context
import androidx.room.Room
import com.nichihab.app.data.local.db.NichiHabDatabase
import com.nichihab.app.data.local.db.dao.BlockedAppDao
import com.nichihab.app.data.local.db.dao.SessionDao
import com.nichihab.app.data.local.db.dao.TaskDao
import com.nichihab.app.data.local.db.dao.VerificationDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): NichiHabDatabase {
        return Room.databaseBuilder(
            context,
            NichiHabDatabase::class.java,
            "NichiHab_db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideTaskDao(db: NichiHabDatabase): TaskDao = db.taskDao()

    @Provides
    fun provideSessionDao(db: NichiHabDatabase): SessionDao = db.sessionDao()

    @Provides
    fun provideVerificationDao(db: NichiHabDatabase): VerificationDao = db.verificationDao()

    @Provides
    fun provideBlockedAppDao(db: NichiHabDatabase): BlockedAppDao = db.blockedAppDao()
}
