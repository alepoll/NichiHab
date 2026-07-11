package com.nichihab.app.data.repository

import com.nichihab.app.domain.repository.ApiKeyRepository
import com.nichihab.app.ui.commercial.CommercialSecurityConfig
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class ApiKeyRepositoryImpl @Inject constructor() : ApiKeyRepository{
    override suspend fun getGeminiApiKey(): String {
        return CommercialSecurityConfig.GLOBAL_API_KEY
    }
}
