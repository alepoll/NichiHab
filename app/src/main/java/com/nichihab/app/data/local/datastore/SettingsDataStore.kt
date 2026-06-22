package com.nichihab.app.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.nichihab.app.util.Constants
import com.nichihab.app.util.CryptoManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = Constants.DATASTORE_NAME
)

@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cryptoManager: CryptoManager
) {
    companion object {
        private val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        private val KEY_API_PROVIDER = stringPreferencesKey("api_provider")
        private val KEY_API_KEY_ENCRYPTED = stringPreferencesKey("api_key_encrypted")
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        private val KEY_STRICTNESS_MODE = stringPreferencesKey("strictness_mode")
        private val KEY_USER_NAME = stringPreferencesKey("user_name")
        private val KEY_NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        private val KEY_REPEAT_DAILY = booleanPreferencesKey("repeat_daily")
        private val KEY_LANGUAGE = stringPreferencesKey("language")
        private val KEY_HAS_ACCEPTED_PRIVACY = booleanPreferencesKey("has_accepted_privacy")
    }

    // ── Onboarding ──
    val onboardingCompleted: Flow<Boolean> = context.dataStore.data
        .map { it[KEY_ONBOARDING_COMPLETED] ?: false }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { it[KEY_ONBOARDING_COMPLETED] = completed }
    }

    // ── Privacy Policy ──
    val hasAcceptedPrivacyPolicy: Flow<Boolean> = context.dataStore.data
        .map { it[KEY_HAS_ACCEPTED_PRIVACY] ?: false }

    suspend fun acceptPrivacyPolicy() {
        context.dataStore.edit { it[KEY_HAS_ACCEPTED_PRIVACY] = true }
    }

    // ── API Provider ──
    val apiProvider: Flow<String> = context.dataStore.data
        .map { it[KEY_API_PROVIDER] ?: Constants.PROVIDER_GEMINI }

    suspend fun setApiProvider(provider: String) {
        context.dataStore.edit { it[KEY_API_PROVIDER] = provider }
    }

    // ── API Key (encrypted) ──
    val apiKey: Flow<String> = context.dataStore.data
        .map { prefs ->
            val encrypted = prefs[KEY_API_KEY_ENCRYPTED] ?: ""
            if (encrypted.isNotBlank()) cryptoManager.decrypt(encrypted) else ""
        }

    suspend fun setApiKey(key: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_API_KEY_ENCRYPTED] = if (key.isNotBlank()) {
                cryptoManager.encrypt(key)
            } else ""
        }
    }

    // ── Theme ──
    val themeMode: Flow<String> = context.dataStore.data
        .map { it[KEY_THEME_MODE] ?: "system" }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { it[KEY_THEME_MODE] = mode }
    }

    // ── Strictness ──
    val strictnessMode: Flow<String> = context.dataStore.data
        .map { it[KEY_STRICTNESS_MODE] ?: "normal" }

    suspend fun setStrictnessMode(mode: String) {
        context.dataStore.edit { it[KEY_STRICTNESS_MODE] = mode }
    }

    // ── User Name ──
    val userName: Flow<String> = context.dataStore.data
        .map { it[KEY_USER_NAME] ?: "" }

    suspend fun setUserName(name: String) {
        context.dataStore.edit { it[KEY_USER_NAME] = name }
    }

    // ── Notifications ──
    val notificationsEnabled: Flow<Boolean> = context.dataStore.data
        .map { it[KEY_NOTIFICATIONS_ENABLED] ?: false }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_NOTIFICATIONS_ENABLED] = enabled }
    }

    // ── Repeat Daily ──
    val repeatDaily: Flow<Boolean> = context.dataStore.data
        .map { it[KEY_REPEAT_DAILY] ?: true }

    suspend fun setRepeatDaily(repeat: Boolean) {
        context.dataStore.edit { it[KEY_REPEAT_DAILY] = repeat }
    }

    // ── Language ──
    val language: Flow<String> = context.dataStore.data
        .map { it[KEY_LANGUAGE] ?: "auto" }

    suspend fun setLanguage(lang: String) {
        context.dataStore.edit { it[KEY_LANGUAGE] = lang }
    }

    /**
     * Resolves the effective language code. If "auto", uses device locale.
     */
    fun resolveLanguage(stored: String): String {
        if (stored != "auto") return stored
        val locale = java.util.Locale.getDefault().language
        return if (locale in listOf("en", "es", "fr", "pt", "de")) locale else "en"
    }
}
