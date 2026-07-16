package com.example.appguard.ui.navigation

import androidx.compose.animation.*
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.appguard.ui.screens.apps.AppsScreen
import com.example.appguard.ui.screens.home.HomeScreen
import com.example.appguard.ui.screens.settings.SettingsScreen

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    data object Home : Screen("home", "首页", Icons.Default.Home)
    data object Apps : Screen("apps", "管理", Icons.Default.Apps)
    data object Settings : Screen("settings", "设置", Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavGraph() {
    val navController = rememberNavController()
    val screens = listOf(Screen.Home, Screen.Apps, Screen.Settings)

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                screens.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
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
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition = {
                fadeIn(initialAlpha = 0.3f) + slideInHorizontally { it / 4 }
            },
            exitTransition = {
                fadeOut(targetAlpha = 0.3f) + slideOutHorizontally { -it / 4 }
            },
            popEnterTransition = {
                fadeIn(initialAlpha = 0.3f) + slideInHorizontally { -it / 4 }
            },
            popExitTransition = {
                fadeOut(targetAlpha = 0.3f) + slideOutHorizontally { it / 4 }
            }
        ) {
            composable(Screen.Home.route) { HomeScreen() }
            composable(Screen.Apps.route) { AppsScreen() }
            composable(Screen.Settings.route) { SettingsScreen() }
        }
    }
}