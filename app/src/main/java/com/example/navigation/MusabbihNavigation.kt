package com.example.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.MusabbihApplication
import com.example.core.engine.CounterUiState
import com.example.feature.adhkar.AdhkarScreen
import com.example.feature.history.HistoryScreen
import com.example.feature.home.HomeScreen
import com.example.feature.settings.SettingsScreen
import com.example.feature.statistics.StatisticsScreen

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home", "الرئيسية", Icons.Default.RadioButtonChecked)
    object Adhkar : Screen("adhkar", "الأذكار", Icons.Default.MenuBook)
    object Statistics : Screen("statistics", "الإحصائيات", Icons.Default.AutoGraph)
    object History : Screen("history", "السجل", Icons.Default.History)
    object Settings : Screen("settings", "الإعدادات", Icons.Default.Settings)
}

val bottomNavScreens = listOf(
    Screen.Home,
    Screen.Adhkar,
    Screen.Statistics,
    Screen.History,
    Screen.Settings
)

@Composable
fun MusabbihApp(
    app: MusabbihApplication
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val userPreferences by app.userPreferencesRepository.userPreferencesFlow
        .collectAsState(initial = com.example.core.datastore.UserPreferences())

    val counterUiState by app.counterEngine.uiState.collectAsState()

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                bottomNavScreens.forEach { screen ->
                    val isSelected = currentRoute == screen.route
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            if (currentRoute != screen.route) {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = screen.title
                            )
                        },
                        label = {
                            Text(
                                text = screen.title,
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.testTag("nav_item_${screen.route}")
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    counterEngine = app.counterEngine,
                    hapticHelper = app.hapticFeedbackHelper,
                    hapticEnabled = userPreferences.hapticEnabled,
                    useArabicIndic = userPreferences.useArabicIndicNumerals,
                    uiState = counterUiState,
                    onNavigateToAdhkar = {
                        navController.navigate(Screen.Adhkar.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }

            composable(Screen.Adhkar.route) {
                AdhkarScreen(
                    database = app.database,
                    counterEngine = app.counterEngine,
                    useArabicIndic = userPreferences.useArabicIndicNumerals,
                    onDhikrSelected = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }

            composable(Screen.Statistics.route) {
                StatisticsScreen(
                    database = app.database,
                    useArabicIndic = userPreferences.useArabicIndicNumerals
                )
            }

            composable(Screen.History.route) {
                HistoryScreen(
                    database = app.database,
                    useArabicIndic = userPreferences.useArabicIndicNumerals
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    userPreferences = userPreferences,
                    preferencesRepository = app.userPreferencesRepository,
                    backupManager = app.backupManager
                )
            }
        }
    }
}
