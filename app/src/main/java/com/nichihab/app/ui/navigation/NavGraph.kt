package com.nichihab.app.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.nichihab.app.ui.home.HomeScreen
import com.nichihab.app.ui.onboarding.OnboardingScreen
import com.nichihab.app.ui.settings.SettingsScreen
import com.nichihab.app.ui.stats.StatsScreen
import com.nichihab.app.ui.settings.BlockedAppsScreen
import com.nichihab.app.ui.verification.CameraScreen
import com.nichihab.app.ui.verification.VerificationResultScreen

@Composable
fun NichiHabNavGraph(
    initialNavigateTo: String? = null,
    navViewModel: NavViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val onboardingCompletedState by navViewModel.onboardingCompleted.collectAsState(initial = null)

    // Wait for DataStore to load to prevent flashing the Onboarding screen
    if (onboardingCompletedState == null) {
        return
    }

    val startDestination = if (onboardingCompletedState == true) Screen.Home.route else Screen.Onboarding.route

    LaunchedEffect(initialNavigateTo, onboardingCompletedState) {
        if (initialNavigateTo == "camera" && onboardingCompletedState == true) {
            navController.navigate(Screen.Camera.route) {
                // Ensure we don't build a massive stack
                popUpTo(Screen.Home.route) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    // Hide bottom bar on full-screen destinations
    val fullScreenRoutes = setOf(Screen.Onboarding.route, Screen.Camera.route, Screen.BlockedApps.route, "verification_result/{verified}/{reason}")
    val showBottomBar = currentDestination?.route !in fullScreenRoutes

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    val s = com.nichihab.app.util.LocalStrings.current
                    Screen.bottomNavItems.filterNotNull().forEach { screen ->
                        val selected = currentDestination?.hierarchy?.any {
                            it.route == screen.route
                        } == true
                        val label = s.get("nav_${screen.route}")
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = if (selected) screen.selectedIcon else screen.unselectedIcon,
                                    contentDescription = label
                                )
                            },
                            label = { Text(label) },
                            selected = selected,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding),
            enterTransition = {
                fadeIn(animationSpec = tween(150))
            },
            exitTransition = {
                fadeOut(animationSpec = tween(150))
            },
            popEnterTransition = {
                fadeIn(animationSpec = tween(150))
            },
            popExitTransition = {
                fadeOut(animationSpec = tween(150))
            }
        ) {
            // Onboarding
            composable(Screen.Onboarding.route) {
                OnboardingScreen(
                    onComplete = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    }
                )
            }

            // Home
            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigateToCamera = {
                        navController.navigate(Screen.Camera.route)
                    },
                    onNavigateToSettings = {
                        navController.navigate(Screen.Settings.route)
                    },
                    onNavigateToBlockedApps = {
                        navController.navigate(Screen.BlockedApps.route)
                    }
                )
            }

            // Stats
            composable(Screen.Stats.route) {
                StatsScreen()
            }

            // Settings
            composable(Screen.Settings.route) {
                SettingsScreen(
                    onNavigateToBlockedApps = {
                        navController.navigate(Screen.BlockedApps.route)
                    }
                )
            }

            // Blocked Apps
            composable(Screen.BlockedApps.route) {
                BlockedAppsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Camera
            composable(Screen.Camera.route) {
                CameraScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onVerificationComplete = { verified, reason ->
                        navController.navigate(
                            Screen.VerificationResult.createRoute(verified, reason)
                        ) {
                            popUpTo(Screen.Camera.route) { inclusive = true }
                        }
                    }
                )
            }

            // Verification Result
            composable(
                route = Screen.VerificationResult.route,
                arguments = listOf(
                    navArgument("verified") { type = NavType.BoolType },
                    navArgument("reason") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val verified = backStackEntry.arguments?.getBoolean("verified") ?: false
                val reason = java.net.URLDecoder.decode(
                    backStackEntry.arguments?.getString("reason") ?: "", "UTF-8"
                )
                VerificationResultScreen(
                    verified = verified,
                    reason = reason,
                    onGoHome = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    },
                    onRetry = {
                        navController.navigate(Screen.Camera.route) {
                            popUpTo(Screen.Home.route)
                        }
                    }
                )
            }
        }
    }
}
