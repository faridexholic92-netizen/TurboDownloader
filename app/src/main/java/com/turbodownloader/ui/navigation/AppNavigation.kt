package com.turbodownloader.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.turbodownloader.ui.screens.browser.BrowserScreen
import com.turbodownloader.ui.screens.downloads.DownloadsScreen
import com.turbodownloader.ui.screens.home.HomeScreen
import com.turbodownloader.ui.screens.home.HomeViewModel
import com.turbodownloader.ui.screens.settings.SettingsScreen
import androidx.hilt.navigation.compose.hiltViewModel

sealed class Screen(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    data object Home : Screen("home", "Home", Icons.Filled.Home, Icons.Outlined.Home)
    data object Downloads : Screen("downloads", "Downloads", Icons.Filled.Download, Icons.Outlined.Download)
    data object Browser : Screen("browser", "Browser", Icons.Filled.Public, Icons.Outlined.Public)
    data object Settings : Screen("settings", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val screens = listOf(Screen.Home, Screen.Downloads, Screen.Browser, Screen.Settings)
    val homeViewModel: HomeViewModel = hiltViewModel()

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                screens.forEach { screen ->
                    val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (selected) screen.selectedIcon else screen.unselectedIcon,
                                contentDescription = screen.title
                            )
                        },
                        label = { Text(screen.title) }
                    )
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(paddingValues),
            enterTransition = {
                fadeIn(animationSpec = tween(300))
            },
            exitTransition = {
                fadeOut(animationSpec = tween(300))
            }
        ) {
            composable(Screen.Home.route) {
                HomeScreen(viewModel = homeViewModel)
            }
            composable(Screen.Downloads.route) {
                DownloadsScreen(viewModel = homeViewModel)
            }
            composable(Screen.Browser.route) {
                BrowserScreen(
                    onDownloadUrl = { url, fileName ->
                        homeViewModel.addDownload(
                            com.turbodownloader.data.model.DownloadRequest(
                                url = url,
                                fileName = fileName
                            )
                        )
                    }
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen()
            }
        }
    }
}
