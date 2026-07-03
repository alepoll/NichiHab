package com.nichihab.app.domain.repository


interface ApiKeyRepository {
    suspend fun getGeminiApiKey(): String
}