package com.nichihab.app.ui.verification

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nichihab.app.data.local.datastore.SettingsDataStore
import com.nichihab.app.data.repository.AiRepositoryImpl
import com.nichihab.app.domain.model.Verification
import com.nichihab.app.domain.repository.ApiKeyRepository
import com.nichihab.app.domain.repository.AiVerificationResult
import com.nichihab.app.domain.repository.SessionRepository
import com.nichihab.app.domain.repository.TaskRepository
import com.nichihab.app.domain.repository.VerificationRepository
import com.nichihab.app.util.AiPromptGenerator
import com.nichihab.app.util.Constants
import com.nichihab.app.util.ImageUtil
import com.nichihab.app.util.PhotoHashCache
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class VerificationUiState(
    val taskTitle: String = "",
    val taskEmoji: String = "✅",
    val taskId: Long = 0,
    val sessionId: Long = 0,
    val isProcessing: Boolean = false,
    val isCapturing: Boolean = false,
    val capturedBitmap: Bitmap? = null,
    val result: AiVerificationResult? = null,
    val attempts: Int = 0,
    val maxAttempts: Int = Constants.MAX_VERIFICATION_ATTEMPTS,
    val cooldownSeconds: Int = 0,
    val isInCooldown: Boolean = false,
    val isTooDark: Boolean = false,
    val isBlurry: Boolean = false,
    val isOffline: Boolean = false,
    val isDuplicate: Boolean = false,
    val hasApiKey: Boolean = false,
    val apiProvider: String = Constants.PROVIDER_GEMINI,
    val strictnessMode: String = "normal",
    val language: String = "en",
    val photoHint: String? = null,
    val error: String? = null,
    val photoPrompt: AiPromptGenerator.PhotoPrompt? = null
)

@HiltViewModel
class VerificationViewModel @Inject constructor(
    private val aiRepository: AiRepositoryImpl,
    private val verificationRepository: VerificationRepository,
    private val taskRepository: TaskRepository,
    private val sessionRepository: SessionRepository,
    private val settingsDataStore: SettingsDataStore,
    private val apiKeyRepository: ApiKeyRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(VerificationUiState())
    val uiState: StateFlow<VerificationUiState> = _uiState.asStateFlow()
    private val verificationMutex = Mutex()

    init {
        loadCurrentTask()
    }

    private fun loadCurrentTask() {
        viewModelScope.launch {
            val latestSession = sessionRepository.getLatestSession()
            val logicalToday = com.nichihab.app.util.TimeUtil.getLogicalDateDomain(latestSession)
            val session = sessionRepository.getSessionForDate(logicalToday) ?: return@launch
            val tasks = taskRepository.getTasksForSessionOnce(session.id)
            val pendingTask = tasks.firstOrNull { !it.isCompleted } ?: tasks.firstOrNull() ?: return@launch
            val apiKey = settingsDataStore.apiKey.first()
            val provider = settingsDataStore.apiProvider.first()
            val strictness = settingsDataStore.strictnessMode.first()
            val langStored = settingsDataStore.language.first()
            val language = settingsDataStore.resolveLanguage(langStored)
            val attempts = verificationRepository.getAttemptCount(pendingTask.id, session.id)
            val photoPrompt = AiPromptGenerator.getPhotoPrompt(pendingTask.title, language)

            _uiState.update {
                it.copy(
                    taskTitle = pendingTask.title,
                    taskEmoji = pendingTask.emoji,
                    taskId = pendingTask.id,
                    sessionId = session.id,
                    hasApiKey = apiKey.isNotBlank(),
                    apiProvider = provider,
                    strictnessMode = strictness,
                    language = language,
                    photoHint = pendingTask.photoHint,
                    attempts = attempts,
                    isCapturing = false,
                    photoPrompt = photoPrompt
                )
            }

            // Check cooldown
            checkCooldown(pendingTask.id, session.id)
        }
    }

    private suspend fun checkCooldown(taskId: Long, sessionId: Long) {
        val attempts = verificationRepository.getAttemptCount(taskId, sessionId)
        if (attempts >= Constants.MAX_VERIFICATION_ATTEMPTS) {
            val lastFailed = verificationRepository.getLastFailedAttemptTime(taskId, sessionId)
            if (lastFailed != null) {
                val elapsed = System.currentTimeMillis() - lastFailed
                val cooldownMs = Constants.COOLDOWN_MINUTES * 60 * 1000L
                if (elapsed < cooldownMs) {
                    val remaining = ((cooldownMs - elapsed) / 1000).toInt()
                    _uiState.update { it.copy(isInCooldown = true, cooldownSeconds = remaining, attempts = attempts) }
                    startCooldownTimer(remaining)
                } else {
                    // Cooldown has expired — reset attempts so user can try again
                    _uiState.update { it.copy(isInCooldown = false, cooldownSeconds = 0, attempts = 0) }
                }
            } else {
                // All attempts used but no failed ones (all verified?) — reset
                _uiState.update { it.copy(attempts = 0) }
            }
        }
    }

    private fun startCooldownTimer(seconds: Int) {
        viewModelScope.launch {
            var remaining = seconds
            while (remaining > 0) {
                _uiState.update { it.copy(cooldownSeconds = remaining, isInCooldown = true) }
                kotlinx.coroutines.delay(1000)
                remaining--
            }
            _uiState.update { it.copy(isInCooldown = false, cooldownSeconds = 0, attempts = 0) }
        }
    }

    fun onCaptureStarted() {
        _uiState.update { it.copy(isCapturing = true) }
    }

    fun onPhotoCaptured(bitmap: Bitmap) {
        viewModelScope.launch {
            // Check sharpness first (blur detection)
            val sharpness = ImageUtil.checkSharpness(bitmap)
            if (sharpness < Constants.BLUR_THRESHOLD) {
                _uiState.update { it.copy(isBlurry = true, capturedBitmap = bitmap, isCapturing = false) }
                return@launch
            }

            // Check luminance (dark photo detection)
            val luminance = ImageUtil.checkLuminance(bitmap)
            if (luminance < 40) {
                _uiState.update { it.copy(isTooDark = true, capturedBitmap = bitmap, isCapturing = false) }
                return@launch
            }
            _uiState.update { it.copy(capturedBitmap = bitmap, isTooDark = false, isBlurry = false) }
            processVerification(bitmap)
        }
    }

    fun retryWithDarkPhoto() {
        val bitmap = _uiState.value.capturedBitmap ?: return
        _uiState.update { it.copy(isTooDark = false) }
        processVerification(bitmap)
    }

    fun dismissDarkWarning() {
        _uiState.update { it.copy(isTooDark = false, capturedBitmap = null, isCapturing = false) }
    }

    fun retryWithBlurryPhoto() {
        val bitmap = _uiState.value.capturedBitmap ?: return
        _uiState.update { it.copy(isBlurry = false) }
        processVerification(bitmap)
    }

    fun dismissBlurWarning() {
        _uiState.update { it.copy(isBlurry = false, capturedBitmap = null, isCapturing = false) }
    }

    private fun processVerification(bitmap: Bitmap) {
        viewModelScope.launch {
            if (!verificationMutex.tryLock()) return@launch
            try {
                _uiState.update { it.copy(isProcessing = true, error = null, result = null, isCapturing = false) }

                val apiKey = apiKeyRepository.getGeminiApiKey()
                val provider = com.nichihab.app.ui.commercial.CommercialSecurityConfig.GLOBAL_PROVIDER

                if (apiKey.isBlank()) {
                    _uiState.update {
                        it.copy(isProcessing = false, error = "System error: Global API key missing.")
                    }
                    return@launch
                }

                try {
                    // Duplicate photo detection (dHash)
                    val photoHash = ImageUtil.computeDHash(bitmap)
                    if (PhotoHashCache.isDuplicate(photoHash)) {
                        _uiState.update {
                            it.copy(isProcessing = false, isDuplicate = true)
                        }
                        return@launch
                    }

                    // bitmapToBase64 internally resizes to MAX_IMAGE_EDGE_PX before encoding
                    val base64 = ImageUtil.bitmapToBase64(bitmap)
                    val taskDesc = _uiState.value.taskTitle

                    val result: AiVerificationResult = aiRepository.verifyWithGemini(base64, taskDesc, apiKey, _uiState.value.language)

                    // Cache the photo hash after a successful submission
                    PhotoHashCache.addHash(photoHash)

                    val newAttempts = _uiState.value.attempts + 1

                    // Save verification record
                    verificationRepository.insert(
                        Verification(
                            taskId = _uiState.value.taskId,
                            sessionId = _uiState.value.sessionId,
                            aiResponse = result.rawResponse,
                            isVerified = result.isVerified,
                            confidence = result.confidence,
                            attemptNumber = newAttempts,
                            provider = provider
                        )
                    )

                    if (result.isVerified) {
                        // Mark task complete
                        taskRepository.markCompleted(_uiState.value.taskId)

                        // Add earned unlock time from this task
                        val task = taskRepository.getById(_uiState.value.taskId)
                        val rewardSeconds = (task?.rewardMinutes ?: 10) * 60L
                        sessionRepository.addEarnedSeconds(_uiState.value.sessionId, rewardSeconds)

                        // Check if all tasks done → full day unlock
                        val remaining = taskRepository.getIncompleteCount(_uiState.value.sessionId)
                        if (remaining == 0) {
                            sessionRepository.updateStatus(
                                _uiState.value.sessionId,
                                "verified",
                                System.currentTimeMillis()
                            )
                        }
                    }

                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            result = result,
                            attempts = newAttempts
                        )
                    }

                    // Start cooldown if max attempts reached and not verified
                    if (!result.isVerified && newAttempts >= Constants.MAX_VERIFICATION_ATTEMPTS) {
                        val cooldownSec = Constants.COOLDOWN_MINUTES * 60
                        _uiState.update { it.copy(isInCooldown = true) }
                        startCooldownTimer(cooldownSec)
                    }
                } catch (e: java.net.UnknownHostException) {
                    _uiState.update {
                        it.copy(isProcessing = false, isOffline = true, error = "No internet connection. Try again when online.")
                    }
                } catch (e: retrofit2.HttpException) {
                    val errorMsg = when (e.code()) {
                        429 -> "API rate limit hit. Image may be too large — waiting 30s before retry."
                        401 -> "Invalid API key. Please check Settings → AI Provider."
                        403 -> "Permission denied. Ensure your API key has Vision access enabled."
                        400 -> "Bad request. Your API key may not support vision/image inputs."
                        else -> "Server error (${e.code()}). Please try again later."
                    }
                    _uiState.update { it.copy(isProcessing = false, error = errorMsg) }
                    
                    // Add a temporary cooldown for 429 to prevent spamming
                    if (e.code() == 429) {
                        startCooldownTimer(30)
                    }
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(isProcessing = false, error = e.message ?: "Verification failed")
                    }
                }
            } finally {
                verificationMutex.unlock()
            }
        }
    }

    fun honorSystemVerify() {
        viewModelScope.launch {
            if (!verificationMutex.tryLock()) return@launch
            try {
                taskRepository.markCompleted(_uiState.value.taskId)

                // Add earned unlock time
                val task = taskRepository.getById(_uiState.value.taskId)
                val rewardSeconds = (task?.rewardMinutes ?: 10) * 60L
                sessionRepository.addEarnedSeconds(_uiState.value.sessionId, rewardSeconds)

                val remaining = taskRepository.getIncompleteCount(_uiState.value.sessionId)
                if (remaining == 0) {
                    sessionRepository.updateStatus(_uiState.value.sessionId, "verified", System.currentTimeMillis())
                }
                verificationRepository.insert(
                    Verification(
                        taskId = _uiState.value.taskId,
                        sessionId = _uiState.value.sessionId,
                        aiResponse = "HONOR_SYSTEM",
                        isVerified = true,
                        confidence = 0f,
                        attemptNumber = _uiState.value.attempts + 1,
                        provider = "honor_system"
                    )
                )
                _uiState.update {
                    it.copy(
                        result = AiVerificationResult(true, "Verified via honor system", 0f, "HONOR_SYSTEM"),
                        isOffline = false
                    )
                }
            } finally {
                verificationMutex.unlock()
            }
        }
    }

    fun resetForRetry() {
        _uiState.update { it.copy(capturedBitmap = null, result = null, error = null, isCapturing = false, isBlurry = false, isDuplicate = false) }
    }
}
