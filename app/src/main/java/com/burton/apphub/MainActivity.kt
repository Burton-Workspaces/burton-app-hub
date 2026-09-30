package com.burton.apphub

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.burton.apphub.ui.apps.AppDetailScreen
import com.burton.apphub.ui.apps.AppsScreen
import com.burton.apphub.ui.navigation.Routes
import com.burton.apphub.ui.settings.SettingsScreen
import com.burton.apphub.ui.theme.BurtonAppHubTheme
import com.burton.apphub.ui.theme.BurtonBlack
import com.burton.apphub.ui.theme.BurtonIvory
import com.burton.apphub.ui.theme.BurtonMute
import com.burton.apphub.ui.updates.UpdatesScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val initialTab = intent.getStringExtra(EXTRA_TAB)
        setContent {
            BurtonAppHubTheme {
                BurtonApp(initialTab = initialTab)
            }
        }
    }

    companion object {
        const val EXTRA_TAB = "tab"
    }
}

@Composable
private fun BurtonApp(initialTab: String?) {
    val navController = rememberNavController()
    val backStack = navController.currentBackStackEntryAsState()
    val route = backStack.value?.destination?.route
    val tabs = listOf(Routes.APPS, Routes.UPDATES, Routes.SETTINGS)
    val selectedTab = when {
        route in tabs -> route
        route?.startsWith("app/") == true -> Routes.APPS
        else -> Routes.APPS
    }
    LaunchedEffect(initialTab) {
        if (initialTab == "updates") navController.goTab(Routes.UPDATES)
    }
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(BurtonBlack),
        containerColor = BurtonBlack,
        bottomBar = {
            NavigationBar(
                containerColor = BurtonBlack,
                contentColor = BurtonIvory,
                modifier = Modifier
                    .background(BurtonBlack)
                    .navigationBarsPadding(),
            ) {
                NavigationBarItem(
                    selected = selectedTab == Routes.APPS,
                    onClick = { navController.goTab(Routes.APPS) },
                    icon = { Icon(Icons.Rounded.Apps, contentDescription = "Apps") },
                    label = { Text("Apps") },
                    colors = navColors(selectedTab == Routes.APPS),
                )
                NavigationBarItem(
                    selected = selectedTab == Routes.UPDATES,
                    onClick = { navController.goTab(Routes.UPDATES) },
                    icon = { Icon(Icons.Rounded.SystemUpdate, contentDescription = "Updates") },
                    label = { Text("Updates") },
                    colors = navColors(selectedTab == Routes.UPDATES),
                )
                NavigationBarItem(
                    selected = selectedTab == Routes.SETTINGS,
                    onClick = { navController.goTab(Routes.SETTINGS) },
                    icon = { Icon(Icons.Rounded.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") },
                    colors = navColors(selectedTab == Routes.SETTINGS),
                )
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.APPS,
            modifier = Modifier.padding(padding),
        ) {
            composable(Routes.APPS) {
                AppsScreen(onOpenApp = { navController.navigate(Routes.app(it)) })
            }
            composable(Routes.UPDATES) {
                UpdatesScreen(onOpenApp = { navController.navigate(Routes.app(it)) })
            }
            composable(Routes.SETTINGS) {
                SettingsScreen()
            }
            composable(
                Routes.APP,
                arguments = listOf(navArgument("packageName") { type = NavType.StringType }),
            ) {
                AppDetailScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}

private fun NavHostController.goTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun navColors(selected: Boolean) = NavigationBarItemDefaults.colors(
    selectedIconColor = BurtonIvory,
    selectedTextColor = BurtonIvory,
    unselectedIconColor = BurtonMute,
    unselectedTextColor = BurtonMute,
    indicatorColor = Color(0xFF222222),
)
