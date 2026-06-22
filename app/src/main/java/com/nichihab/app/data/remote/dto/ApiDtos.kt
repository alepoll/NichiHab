package com.nichihab.app.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass


// ── Google Gemini API ──

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    val contents: List<GeminiContent>,
    @Json(name = "system_instruction") val systemInstruction: GeminiContent? = null  // System instruction
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    val parts: List<GeminiPart>
)

@JsonClass(generateAdapter = true)
data class GeminiPart(
    val text: String? = null,
    @Json(name = "inline_data") val inlineData: GeminiInlineData? = null
)

@JsonClass(generateAdapter = true)
data class GeminiInlineData(
    @Json(name = "mime_type") val mimeType: String = "image/jpeg",
    val data: String
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    val candidates: List<GeminiCandidate> = emptyList()
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    val content: GeminiCandidateContent? = null
)

@JsonClass(generateAdapter = true)
data class GeminiCandidateContent(
    val parts: List<GeminiResponsePart> = emptyList()
)

@JsonClass(generateAdapter = true)
data class GeminiResponsePart(
    val text: String = ""
)
