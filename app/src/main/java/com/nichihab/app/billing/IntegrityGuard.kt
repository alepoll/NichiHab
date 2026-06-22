package com.nichihab.app.billing

import android.util.Log
import com.google.firebase.appcheck.FirebaseAppCheck
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * IntegrityGuard — Uses Firebase App Check (Play Integrity under the hood) to verify
 * that the device and app are in a genuine, untampered state.
 *
 * If a user has:
 *   - Rooted their device
 *   - Used LuckyPatcher or similar tools to modify the APK
 *   - Is running on an emulator
 *
 * ...the App Check token generation will FAIL, and premium features will be blocked
 * regardless of what the local BillingClient reports.
 *
 * This is the standard defense against client-side billing bypasses on the free (Spark) plan.
 */
@Singleton
class IntegrityGuard @Inject constructor() {

    companion object {
        private const val TAG = "IntegrityGuard"
    }

    /**
     * null  = integrity check hasn't completed yet (benefit of the doubt → allow)
     * true  = device passed Play Integrity → genuine, untampered
     * false = device FAILED Play Integrity → rooted, tampered, or emulated
     */
    private val _isDeviceTrusted = MutableStateFlow<Boolean?>(null)
    val isDeviceTrusted: StateFlow<Boolean?> = _isDeviceTrusted.asStateFlow()

    /**
     * Initiates a device integrity check via Firebase App Check + Play Integrity.
     * Should be called AFTER FirebaseApp.initializeApp() and App Check installation.
     */
    fun checkIntegrity() {
        try {
            val appCheck = FirebaseAppCheck.getInstance()
            // forceRefresh = false → uses cached token if still valid, else refreshes
            appCheck.getAppCheckToken(false)
                .addOnSuccessListener { token ->
                    _isDeviceTrusted.value = true
                    Log.d(TAG, "Device integrity verified successfully")
                }
                .addOnFailureListener { exception ->
                    _isDeviceTrusted.value = false
                    Log.w(TAG, "Device integrity check FAILED: ${exception.message}")
                }
        } catch (e: Exception) {
            // If App Check isn't properly configured, fail secure
            _isDeviceTrusted.value = false
            Log.e(TAG, "Error during integrity check", e)
        }
    }

    /**
     * Forces a fresh integrity check (bypasses cache).
     * Use this when the user completes a new purchase to re-validate.
     */
    fun forceRecheck() {
        try {
            val appCheck = FirebaseAppCheck.getInstance()
            appCheck.getAppCheckToken(true) // forceRefresh = true
                .addOnSuccessListener { _isDeviceTrusted.value = true }
                .addOnFailureListener { _isDeviceTrusted.value = false }
        } catch (e: Exception) {
            _isDeviceTrusted.value = false
        }
    }

    /**
     * Returns true if the device is effectively trusted:
     * - true if integrity check passed
     * - true if integrity check hasn't completed yet (benefit of the doubt)
     * - FALSE only if the check explicitly FAILED
     *
     * This means a LuckyPatcher user will be blocked as soon as the check completes,
     * but legitimate users won't be blocked during the brief check window at startup.
     */
    fun isEffectivelyTrusted(): Boolean {
        return _isDeviceTrusted.value != false
    }
}
