package com.nichihab.app.billing

import android.content.Context
import android.util.Base64
import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.android.play.core.integrity.IntegrityManager
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.IntegrityTokenRequest
import com.google.android.play.core.integrity.IntegrityTokenResponse

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.security.SecureRandom

data class IntegrityResult(
    val isIntegrityVerified: Boolean,
    val isDeviceIntegrityPassed: Boolean = false,
    val isAppIntegrityPassed: Boolean = false,
    val errorMessage: String = ""
)

object IntegrityGuard {

    private const val TAG = "IntegrityGuard"

    // TODO: Set this to your actual Google Cloud project number (not project ID)
    // Found in: Google Cloud Console > IAM & Admin > Settings > Project number
    // Or: Play Console > Play Integrity > Cloud project number
    private const val CLOUD_PROJECT_NUMBER = 306337512308L

    suspend fun verifyIntegrity(context: Context): IntegrityResult {
        return withContext(Dispatchers.IO) {
            try {
                val integrityManager = IntegrityManagerFactory.create(context)
                val nonce = generateNonce()

                val tokenResponse: IntegrityTokenResponse = Tasks.await(
                    integrityManager.requestIntegrityToken(
                        IntegrityTokenRequest.builder()
                            .setCloudProjectNumber(CLOUD_PROJECT_NUMBER)
                            .setNonce(nonce)
                            .build()
                    )
                )

                val token = tokenResponse.token()
                decodeAndVerifyToken(token, nonce)
            } catch (e: Exception) {
                Log.w(TAG, "Integrity check failed: ${e.message}")
                IntegrityResult(
                    isIntegrityVerified = false,
                    errorMessage = "Integrity check failed. Device may not be Google Play certified."
                )
            }
        }
    }

    suspend fun verifyPurchaseToken(context: Context, purchaseToken: String): IntegrityResult {
        return withContext(Dispatchers.IO) {
            try {
                val integrityManager = IntegrityManagerFactory.create(context)
                val nonce = generateNonce()

                val tokenResponse: IntegrityTokenResponse = Tasks.await(
                    integrityManager.requestIntegrityToken(
                        IntegrityTokenRequest.builder()
                            .setCloudProjectNumber(CLOUD_PROJECT_NUMBER)
                            .setNonce(nonce)
                            .build()
                    )
                )

                val token = tokenResponse.token()
                // Verify the nonce in the response matches what we sent
                val result = decodeAndVerifyToken(token, nonce)
                // Note: actual purchase token should be verified server-side
                // with the Google Play Developer API for production

                if (result.isIntegrityVerified) {
                    Log.d(TAG, "Purchase integrity verified successfully")
                } else {
                    Log.w(TAG, "Purchase integrity verification failed: $result")
                }
                result
            } catch (e: Exception) {
                Log.w(TAG, "Purchase token integrity check failed: ${e.message}")
                IntegrityResult(
                    isIntegrityVerified = false,
                    errorMessage = "Purchase integrity check failed: ${e.message ?: "Unknown error"}"
                )
            }
        }
    }

    private fun generateNonce(): String {
        val random = ByteArray(32)
        SecureRandom().nextBytes(random)
        return Base64.encodeToString(random, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    internal fun decodeAndVerifyToken(token: String, expectedNonce: String? = null): IntegrityResult {
        return try {
            val parts = token.split(".")
            if (parts.size < 2) {
                return IntegrityResult(false, errorMessage = "Invalid token format")
            }

            val payload = String(Base64.decode(parts[1], Base64.URL_SAFE))
            val json = JSONObject(payload)

            // Verify nonce if provided (REPLAY PROTECTION)
            if (expectedNonce != null) {
                val tokenNonce = json.optString("nonce", "")
                if (tokenNonce != expectedNonce) {
                    Log.w(TAG, "Nonce mismatch: token nonce differs from expected")
                    return IntegrityResult(false, errorMessage = "Nonce mismatch - possible replay attack")
                }
            }

            val deviceIntegrity = json.optJSONObject("deviceIntegrity")
            val passesDeviceIntegrity = deviceIntegrity
                ?.optJSONArray("deviceRecognitionVerdict")
                ?.let { arr ->
                    for (i in 0 until arr.length()) {
                        if (arr.getString(i) == "MEETS_DEVICE_INTEGRITY") return@let true
                    }
                    false
                } ?: false

            val appIntegrity = json.optJSONObject("appIntegrity")
            val appRecognitionVerdict = appIntegrity?.optString("appRecognitionVerdict")
            val passesAppIntegrity = appRecognitionVerdict == "PLAY_RECOGNIZED"

            IntegrityResult(
                isIntegrityVerified = passesDeviceIntegrity && passesAppIntegrity,
                isDeviceIntegrityPassed = passesDeviceIntegrity,
                isAppIntegrityPassed = passesAppIntegrity
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decode integrity token: ${e.message}")
            IntegrityResult(false, errorMessage = "Failed to decode token: ${e.message ?: "Unknown error"}")
        }
    }
}
