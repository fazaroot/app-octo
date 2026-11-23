package com.clonex.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.clonex.app.ui.navigation.AppRoute
import com.clonex.app.ui.screens.CompatibilityScreen
import com.clonex.app.ui.screens.DashboardScreen
import com.clonex.app.ui.screens.ProvisioningScreen
import com.clonex.app.ui.theme.CloneXTheme

/**
 * Activity utama CloneX.
 *
 * Navigasi bawah dengan tiga layar nyata: Beranda, Kompatibilitas,
 * dan Profil Kerja. Tidak ada layar atau tombol palsu.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CloneXTheme {
                CloneXApp()
            }
        }
    }
}

private data class BottomNavItem(
    val route: AppRoute,
    val icon: ImageVector
)

private val bottomNavItems = listOf(
    BottomNavItem(AppRoute.Dashboard, Icons.Filled.Apps),
    BottomNavItem(AppRoute.Compatibility, Icons.Filled.CheckCircle),
    BottomNavItem(AppRoute.Provisioning, Icons.Filled.Work)
)

@Composable
private fun CloneXApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                for (item in bottomNavItems) {
                    NavigationBarItem(
                        selected = currentRoute == item.route.route,
                        onClick = {
                            navController.navigate(item.route.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.route.label
                            )
                        },
                        label = { Text(item.route.label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = AppRoute.Dashboard.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(AppRoute.Dashboard.route) {
                DashboardScreen(
                    onOpenProvisioning = {
                        navController.navigate(AppRoute.Provisioning.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            composable(AppRoute.Compatibility.route) {
                CompatibilityScreen(
                    onOpenProvisioning = {
                        navController.navigate(AppRoute.Provisioning.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            composable(AppRoute.Provisioning.route) {
                ProvisioningScreen()
            }
        }
    }
}
