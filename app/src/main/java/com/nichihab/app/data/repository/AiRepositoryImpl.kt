package com.nichihab.app.data.repository

import android.util.Log
import com.nichihab.app.data.remote.api.GeminiApiService
import com.nichihab.app.data.remote.dto.*
import com.nichihab.app.domain.model.AiTaskSuggestion
import com.nichihab.app.domain.repository.AiRepository
import com.nichihab.app.domain.repository.AiVerificationResult
import com.nichihab.app.domain.repository.ApiKeyRepository
import com.nichihab.app.util.Constants
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.delay
import java.text.Normalizer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiRepositoryImpl @Inject constructor(
    private val geminiApi: GeminiApiService,
    private val moshi: Moshi,
    private val apiKeyRepository: ApiKeyRepository
) : AiRepository {

    // ── Local result cache for retry deduplication ──
    // Key: "${taskDescription}|${imageHashPrefix}", Value: (result, timestamp)
    private val resultCache = LinkedHashMap<String, Pair<AiVerificationResult, Long>>(16, 0.75f, true)
    private val cacheTtlMs = 60_000L // 60 seconds
    private val maxCacheSize = 10

    override suspend fun verifyTask(
        photoBase64: String,
        taskDescription: String
    ): AiVerificationResult {
        return try {
            val apiKey = apiKeyRepository.getGeminiApiKey()
            
            if (apiKey.isBlank()) {
                return AiVerificationResult(false, "API Key is missing. Please check your internet connection.", 0f, "")
            }
            
            verifyWithGemini(photoBase64, taskDescription, apiKey)
        } catch (e: retrofit2.HttpException) {
            val errorBody = e.response()?.errorBody()?.string()
            Log.e("AiRepository", "HTTP ${e.code()}: ${errorBody ?: e.message}")
            AiVerificationResult(
                isVerified = false,
                reason = "Error: ${e.message ?: "Unknown error"}",
                rawResponse = errorBody ?: e.toString()
            )
        } catch (e: Exception) {
            AiVerificationResult(
                isVerified = false,
                reason = "Error: ${e.message ?: "Unknown error"}",
                rawResponse = e.toString()
            )
        }
    }

    suspend fun verifyWithGemini(
        photoBase64: String,
        taskDescription: String,
        apiKey: String,
        language: String = "en"
    ): AiVerificationResult {
        // Check cache first
        val cacheKey = buildCacheKey(taskDescription, photoBase64)
        getCachedResult(cacheKey)?.let { return it }

        val systemInstruction = Constants.getVerificationSystemInstruction(language)
        val prompt = Constants.getVerificationPrompt(taskDescription, language)
        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(
                    parts = listOf(
                        GeminiPart(inlineData = GeminiInlineData(data = photoBase64)),
                        GeminiPart(text = prompt)
                    )
                )
            ),
            systemInstruction = GeminiContent(
                parts = listOf(GeminiPart(text = systemInstruction))
            )
        )

        val url = "v1beta/models/${Constants.GEMINI_MODEL}:generateContent"
        val response = retryOnRateLimit {
            geminiApi.generateContent(url = url, apiKey = apiKey, request = request)
        }
        val text = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
        val result = parseVerificationResponse(text)
        cacheResult(cacheKey, result)
        return result
    }

    override suspend fun suggestTasks(goal: String): List<AiTaskSuggestion> {
        // Returns hardcoded suggestions as fallback; AI suggestions require API key
        return getDefaultSuggestions(goal)
    }

    // ── Multi-language verification response parsing ──

    companion object {
        // All positive verification keywords across supported languages
        private val VERIFIED_KEYWORDS = setOf(
            "VERIFIED",        // English
            "VERIFICADO",      // Spanish / Portuguese
            "VERIFICADA",      // Spanish / Portuguese (feminine)
            "VERIFIE",         // French (normalized, no accent)
            "VERIFIZIERT",     // German
            "VERIFIEE"         // French feminine (normalized)
        )

        // All negative verification keywords across supported languages
        private val NOT_VERIFIED_KEYWORDS = setOf(
            "NOT_VERIFIED",
            "NOT VERIFIED",
            "NO_VERIFICADO",
            "NO VERIFICADO",
            "NO_VERIFICADA",
            "NO VERIFICADA",
            "NON_VERIFIE",
            "NON VERIFIE",
            "NON_VERIFIEE",
            "NON VERIFIEE",
            "NICHT_VERIFIZIERT",
            "NICHT VERIFIZIERT",
            "NAO_VERIFICADO",       // Portuguese
            "NAO VERIFICADO",
            "NAO_VERIFICADA",
            "NAO VERIFICADA",
            "NOT_VERIFICADO",       // Mixed
            "NOT_VERIFIE"
        )
    }

    /**
     * Normalizes unicode text by stripping accents/diacritics.
     * e.g., "VÉRIFIÉ" → "VERIFIE"
     */
    private fun normalizeUnicode(text: String): String {
        val normalized = Normalizer.normalize(text, Normalizer.Form.NFD)
        return normalized.replace(Regex("[\\p{InCombiningDiacriticalMarks}]"), "")
    }

    private fun parseVerificationResponse(text: String): AiVerificationResult {
        val normalizedText = normalizeUnicode(text.uppercase().trim())

        // Check for negative keywords FIRST (they contain positive keywords as substrings)
        val isNegative = NOT_VERIFIED_KEYWORDS.any { keyword ->
            normalizedText.startsWith(keyword)
        }

        val isVerified = if (isNegative) {
            false
        } else {
            VERIFIED_KEYWORDS.any { keyword ->
                normalizedText.startsWith(keyword)
            }
        }

        // Extract reason: remove all known keywords from the text
        var reason = text
        // Remove negative keywords first (longer matches)
        for (kw in NOT_VERIFIED_KEYWORDS) {
            reason = reason.replace(kw, "", ignoreCase = true)
        }
        // Then positive keywords
        for (kw in VERIFIED_KEYWORDS) {
            reason = reason.replace(kw, "", ignoreCase = true)
        }
        // Also handle accented forms in the original text
        reason = reason
            .replace("VÉRIFIÉ", "", ignoreCase = true)
            .replace("NON VÉRIFIÉ", "", ignoreCase = true)
            .replace("NÃO VERIFICADO", "", ignoreCase = true)
            .replace("NÃO VERIFICADA", "", ignoreCase = true)
            .trim()
            .trimStart('.', ',', ':', '-', ' ')
            .trim()
            .ifBlank { if (isVerified) "Task appears complete." else "Could not confirm task completion." }

        // Simple confidence heuristic based on response clarity
        val confidence = when {
            isVerified && VERIFIED_KEYWORDS.any { normalizedText.startsWith(it) } -> 0.92f
            !isVerified && NOT_VERIFIED_KEYWORDS.any { normalizedText.startsWith(it) } -> 0.88f
            else -> 0.65f
        }

        return AiVerificationResult(
            isVerified = isVerified,
            reason = reason,
            confidence = confidence,
            rawResponse = text
        )
    }

    // ── Cache helpers ──

    private fun buildCacheKey(taskDescription: String, photoBase64: String): String {
        // Use last 64 chars of base64 — JPEG headers are identical across photos,
        // but the tail of the encoded data contains actual pixel content.
        val imageSuffix = photoBase64.takeLast(64)
        return "$taskDescription|$imageSuffix"
    }

    private fun getCachedResult(key: String): AiVerificationResult? {
        val entry = resultCache[key] ?: return null
        val (result, timestamp) = entry
        return if (System.currentTimeMillis() - timestamp < cacheTtlMs) result else {
            resultCache.remove(key)
            null
        }
    }

    private fun cacheResult(key: String, result: AiVerificationResult) {
        // Evict oldest if at capacity
        if (resultCache.size >= maxCacheSize) {
            val oldestKey = resultCache.keys.firstOrNull()
            if (oldestKey != null) resultCache.remove(oldestKey)
        }
        resultCache[key] = result to System.currentTimeMillis()
    }

    private fun parseTaskSuggestions(text: String): List<AiTaskSuggestion> {
        return try {
            // Find JSON array in the response
            val jsonStart = text.indexOf('[')
            val jsonEnd = text.lastIndexOf(']')
            if (jsonStart == -1 || jsonEnd == -1) return getDefaultSuggestions("")

            val jsonStr = text.substring(jsonStart, jsonEnd + 1)
            val type = Types.newParameterizedType(List::class.java, Map::class.java)
            val adapter = moshi.adapter<List<Map<String, Any>>>(type)
            val parsed = adapter.fromJson(jsonStr) ?: return getDefaultSuggestions("")

            parsed.map { item ->
                AiTaskSuggestion(
                    task = item["task"]?.toString() ?: "",
                    emoji = item["emoji"]?.toString() ?: "✅",
                    estimatedMinutes = (item["estimatedMinutes"] as? Number)?.toInt() ?: 5,
                    photoHint = item["photoHint"]?.toString()
                )
            }
        } catch (e: Exception) {
            getDefaultSuggestions("")
        }
    }

    private fun getDefaultSuggestions(goal: String): List<AiTaskSuggestion> {
        return listOf(
            AiTaskSuggestion("Make your bed", "🛏️", 2),
            AiTaskSuggestion("Drink a glass of water", "💧", 1),
            AiTaskSuggestion("Do 10 push-ups", "💪", 3),
            AiTaskSuggestion("Write 3 things you're grateful for", "📝", 5),
            AiTaskSuggestion("Tidy your desk", "✨", 5)
        )
    }

    /**
     * Retries a suspend block up to [maxRetries] times on HTTP 429 (rate limit) errors,
     * using exponential backoff. This prevents the user from seeing "limit error" on
     * first-time rate limit hits.
     */
    private suspend fun <T> retryOnRateLimit(
        maxRetries: Int = 2,
        initialDelayMs: Long = 2000,
        block: suspend () -> T
    ): T {
        var lastException: Exception? = null
        var delayMs = initialDelayMs
        repeat(maxRetries + 1) { attempt ->
            try {
                return block()
            } catch (e: retrofit2.HttpException) {
                if (e.code() == 429 && attempt < maxRetries) {
                    // Check for Retry-After header
                    val retryAfter = e.response()?.headers()?.get("retry-after")?.toLongOrNull()
                    val waitMs = if (retryAfter != null) retryAfter * 1000 else delayMs
                    delay(waitMs)
                    delayMs *= 2 // Exponential backoff
                    lastException = e
                } else {
                    throw e
                }
            }
        }
        throw lastException!!
    }
}
