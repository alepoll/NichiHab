package com.nichihab.app.data.remote.api

import com.nichihab.app.data.remote.dto.GeminiRequest
import com.nichihab.app.data.remote.dto.GeminiResponse
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Url



interface GeminiApiService {
    @POST
    suspend fun generateContent(
        @Url url: String,
        @Header("x-goog-api-key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}
