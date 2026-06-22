package com.nichihab.app.di

import com.nichihab.app.data.repository.SessionRepositoryImpl
import com.nichihab.app.data.repository.TaskRepositoryImpl
import com.nichihab.app.data.repository.VerificationRepositoryImpl
import com.nichihab.app.domain.repository.SessionRepository
import com.nichihab.app.domain.repository.TaskRepository
import com.nichihab.app.domain.repository.VerificationRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindTaskRepository(impl: TaskRepositoryImpl): TaskRepository

    @Binds
    @Singleton
    abstract fun bindSessionRepository(impl: SessionRepositoryImpl): SessionRepository

    @Binds
    @Singleton
    abstract fun bindVerificationRepository(impl: VerificationRepositoryImpl): VerificationRepository
}
