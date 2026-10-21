package com.batteryscope.pro

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.batteryscope.pro.ui.BatteryViewModel
import com.batteryscope.pro.ui.screens.DeviceInfoScreen
import com.batteryscope.pro.ui.screens.HistoryScreen
import com.batteryscope.pro.ui.screens.HomeScreen
import com.batteryscope.pro.ui.screens.RealtimeScreen
import com.batteryscope.pro.ui.theme.BatteryScopeTheme

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* 通知権限は任意。拒否されても主要機能は動作する */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            val viewModel: BatteryViewModel = viewModel()
            val settings by viewModel.settings.collectAsState()
            BatteryScopeTheme(themeMode = settings.themeMode) {
                MainNav(viewModel)
            }
        }
    }
}

private sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    data object Home : Screen("home", "ホーム", Icons.Outlined.Home)
    data object Realtime : Screen("realtime", "リアルタイム", Icons.Outlined.Speed)
    data object History : Screen("history", "履歴", Icons.Outlined.History)
    data object Device : Screen("device", "端末", Icons.Outlined.PhoneAndroid)
}

@Composable
private fun MainNav(viewModel: BatteryViewModel) {
    val navController = rememberNavController()
    val items = listOf(Screen.Home, Screen.Realtime, Screen.History, Screen.Device)

    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination
            NavigationBar {
                items.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.label) },
                        label = { Text(screen.label) },
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
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) { HomeScreen(viewModel) }
            composable(Screen.Realtime.route) { RealtimeScreen(viewModel) }
            composable(Screen.History.route) { HistoryScreen(viewModel) }
            composable(Screen.Device.route) { DeviceInfoScreen(viewModel) }
        }
    }
}
