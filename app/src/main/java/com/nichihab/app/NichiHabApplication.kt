package com.nichihab.app

import kotlinx.coroutines.launch
import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.nichihab.app.service.worker.SessionAutoStartWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings

@HiltAndroidApp
class NichiHabApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var integrityGuard: com.nichihab.app.billing.IntegrityGuard

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    private val applicationScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        
        // Initialize Firebase Core (keep on main thread as it's required immediately by many components)
        FirebaseApp.initializeApp(this)
        
        applicationScope.launch {
            // Initialize App Check (Play Integrity) - Keep this early as auth might need it
            val firebaseAppCheck = FirebaseAppCheck.getInstance()
            firebaseAppCheck.installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance()
            )
            
            // Ensure user is authenticated anonymously
            authenticateAnonymously()
            
            // Defer non-critical initializations to free up CPU during cold start
            kotlinx.coroutines.delay(3000)
            
            // Trigger the first integrity check
            integrityGuard.checkIntegrity()
            
            // Initialize Remote Config
            initializeRemoteConfig()
            
            scheduleAutoStartWorker()
        }
    }
    
    private fun authenticateAnonymously() {
        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            auth.signInAnonymously()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d("NichiHabApplication", "Firebase Anonymous Auth successful: ${auth.currentUser?.uid}")
                    } else {
                        Log.e("NichiHabApplication", "Firebase Anonymous Auth failed", task.exception)
                    }
                }
        } else {
            Log.d("NichiHabApplication", "User already authenticated: ${auth.currentUser?.uid}")
        }
    }

    private fun initializeRemoteConfig() {
        val remoteConfig = FirebaseRemoteConfig.getInstance()
        val configSettings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(3600) // 1 hour for production, change to 0 for debugging
            .build()
        remoteConfig.setConfigSettingsAsync(configSettings)

        // Set default values just in case
        val defaults = mapOf("gemini_api_key" to "")
        remoteConfig.setDefaultsAsync(defaults)

        remoteConfig.fetchAndActivate()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val updated = task.result
                    Log.d("NichiHabApplication", "Config params updated: $updated")
                } else {
                    Log.d("NichiHabApplication", "Fetch failed")
                }
            }
    }

    private fun scheduleAutoStartWorker() {
        val request = PeriodicWorkRequestBuilder<SessionAutoStartWorker>(
            15, TimeUnit.MINUTES
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            SessionAutoStartWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}
