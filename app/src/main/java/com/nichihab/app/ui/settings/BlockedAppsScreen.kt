package com.nichihab.app.ui.settings

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nichihab.app.ui.theme.StatusLocked
import com.nichihab.app.util.LocalStrings

@Composable
fun BlockedAppsScreen(
    onNavigateBack: () -> Unit,
    viewModel: BlockedAppsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val s = LocalStrings.current



    val filteredApps = if (state.searchQuery.isBlank()) state.apps
    else state.apps.filter {
        it.appName.contains(state.searchQuery, ignoreCase = true) ||
        it.packageName.contains(state.searchQuery, ignoreCase = true)
    }

    val suggestedApps = filteredApps.filter { it.isSuggested }
    val otherApps = filteredApps.filter { !it.isSuggested }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = s.get("blocked_apps_title"),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = s.get("blocked_apps_count").replace("%d", "${state.blockedCount}"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Search
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = { viewModel.updateSearchQuery(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            placeholder = { Text(s.get("blocked_apps_search")) },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (state.isHardLocked) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = s.get("permissions_locked_hard_mode"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                // Suggested section
                if (suggestedApps.isNotEmpty() && state.searchQuery.isBlank()) {
                    item {
                        Text(
                            text = s.get("blocked_apps_suggested"),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    items(suggestedApps, key = { it.packageName }) { app ->
                        var icon by remember(app.packageName) { mutableStateOf<Drawable?>(null) }
                        LaunchedEffect(app.packageName) {
                            if (icon == null) {
                                icon = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                    viewModel.getAppIcon(app.packageName)
                                }
                            }
                        }
                        AppBlockItem(
                            app = app,
                            icon = icon,
                            isHardLocked = state.isHardLocked, onToggle = { viewModel.toggleApp(app.packageName, app.appName) }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = s.get("blocked_apps_all"),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    items(otherApps, key = { it.packageName }) { app ->
                        var icon by remember(app.packageName) { mutableStateOf<Drawable?>(null) }
                        LaunchedEffect(app.packageName) {
                            if (icon == null) {
                                icon = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                    viewModel.getAppIcon(app.packageName)
                                }
                            }
                        }
                        AppBlockItem(
                            app = app,
                            icon = icon,
                            isHardLocked = state.isHardLocked, onToggle = { viewModel.toggleApp(app.packageName, app.appName) }
                        )
                    }
                } else {
                    items(filteredApps, key = { it.packageName }) { app ->
                        var icon by remember(app.packageName) { mutableStateOf<Drawable?>(null) }
                        LaunchedEffect(app.packageName) {
                            if (icon == null) {
                                icon = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                    viewModel.getAppIcon(app.packageName)
                                }
                            }
                        }
                        AppBlockItem(
                            app = app,
                            icon = icon,
                            isHardLocked = state.isHardLocked, onToggle = { viewModel.toggleApp(app.packageName, app.appName) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppBlockItem(app: InstalledApp, icon: Drawable?, isHardLocked: Boolean, onToggle: () -> Unit) {
    val s = LocalStrings.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 3.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (app.isBlocked)
                StatusLocked.copy(alpha = 0.06f)
            else
                MaterialTheme.colorScheme.surfaceContainer
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App icon (lazy loaded)
            if (icon != null) {
                Image(
                    bitmap = icon.toBitmap(width = 96, height = 96).asImageBitmap(),
                    contentDescription = app.appName,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(
                            if (app.isBlocked) StatusLocked.copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = app.appName.first().uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.appName,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (app.isBlocked) {
                    Text(
                        text = s.get("blocked_apps_blocked_label"),
                        style = MaterialTheme.typography.labelSmall,
                        color = StatusLocked
                    )
                }
            }

            Switch(
                checked = app.isBlocked,
                onCheckedChange = { onToggle() },
                enabled = !isHardLocked,
                colors = SwitchDefaults.colors(
                    checkedTrackColor = StatusLocked,
                    checkedThumbColor = MaterialTheme.colorScheme.onError
                )
            )
        }
    }
}
