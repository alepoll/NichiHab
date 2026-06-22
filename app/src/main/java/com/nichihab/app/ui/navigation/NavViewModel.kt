package com.nichihab.app.ui.navigation

import androidx.lifecycle.ViewModel
import com.nichihab.app.data.local.datastore.SettingsDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class NavViewModel @Inject constructor(
    settingsDataStore: SettingsDataStore
) : ViewModel() {
    val onboardingCompleted: Flow<Boolean> = settingsDataStore.onboardingCompleted
}
