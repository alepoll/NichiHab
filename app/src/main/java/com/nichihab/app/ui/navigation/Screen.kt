package com.nichihab.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String = "",
    val selectedIcon: ImageVector = Icons.Default.Home,
    val unselectedIcon: ImageVector = Icons.Outlined.Home
) {
    // Bottom nav destinations
    data object Home : Screen(
        route = "home",
        title = "Home",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    )
    data object Stats : Screen(
        route = "stats",
        title = "Stats",
        selectedIcon = Icons.Filled.BarChart,
        unselectedIcon = Icons.Outlined.BarChart
    )
    data object Settings : Screen(
        route = "settings",
        title = "Settings",
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings
    )

    // Non-nav destinations
    data object Onboarding : Screen(route = "onboarding")
    data object Camera : Screen(route = "camera")
    data object VerificationResult : Screen(route = "verification_result/{verified}/{reason}") {
        fun createRoute(verified: Boolean, reason: String): String {
            val encodedReason = java.net.URLEncoder.encode(reason, "UTF-8")
            return "verification_result/$verified/$encodedReason"
        }
    }
    data object BlockedApps : Screen(route = "blocked_apps")

    companion object {
        val bottomNavItems = listOf(Home, Stats, Settings)
    }
}
