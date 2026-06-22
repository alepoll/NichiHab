package com.nichihab.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.nichihab.app.data.local.datastore.SettingsDataStore
import com.nichihab.app.ui.navigation.NichiHabNavGraph
import com.nichihab.app.ui.theme.NichiHabTheme
import com.nichihab.app.util.AppStrings
import com.nichihab.app.util.LocalStrings
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var settingsDataStore: SettingsDataStore

    private var navigationTarget = androidx.compose.runtime.mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        
        navigationTarget.value = intent.getStringExtra("navigate_to")
        
        enableEdgeToEdge()
        setContent {
            val themeMode by settingsDataStore.themeMode.collectAsState(initial = "system")
            val langStored by settingsDataStore.language.collectAsState(initial = "auto")
            val resolvedLang = androidx.compose.runtime.remember(langStored) { settingsDataStore.resolveLanguage(langStored) }
            val systemDark = isSystemInDarkTheme()
            val darkTheme = when (themeMode) {
                "dark" -> true
                "light" -> false
                else -> systemDark
            }

            NichiHabTheme(darkTheme = darkTheme, dynamicColor = false) {
                CompositionLocalProvider(LocalStrings provides AppStrings(resolvedLang)) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        NichiHabNavGraph(initialNavigateTo = navigationTarget.value)
                        
                        // Privacy consent is now handled in OnboardingScreen

                        // Reset after consumption
                        androidx.compose.runtime.LaunchedEffect(navigationTarget.value) {
                            if (navigationTarget.value != null) {
                                navigationTarget.value = null
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val target = intent.getStringExtra("navigate_to")
        if (target != null) {
            navigationTarget.value = target
        }
    }
}
