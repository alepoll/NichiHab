package com.nichihab.app.data.repository

import com.nichihab.app.BuildConfig
import com.nichihab.app.domain.repository.ApiKeyRepository
import com.nichihab.app.ui.commercial.CommercialSecurityConfig
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class ApiKeyRepositoryImpl @Inject constructor() : ApiKeyRepository{
    override suspend fun getGeminiApiKey(): String {
        // 1. BuildConfig del .env (desarrollo local)
        if (BuildConfig.GEMINI_API_KEY.isNotBlank()) return BuildConfig.GEMINI_API_KEY
        // 2. Fallback: C++
        return CommercialSecurityConfig.GLOBAL_API_KEY
    }
}
