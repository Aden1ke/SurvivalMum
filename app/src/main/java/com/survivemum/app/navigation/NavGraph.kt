package com.survivemum.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.survivemum.app.ui.screens.DashboardScreen
import com.survivemum.app.ui.screens.AlertScreen
import com.survivemum.app.ui.screens.CameraMonitorScreen

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object MotherMonitor : Screen("mother_monitor")
    object Alert : Screen("alert")
    object NewbornMonitor : Screen("newborn_monitor")
    object ToddlerMonitor : Screen("toddler_monitor")
    object Timeline : Screen("timeline")
    object QRCode : Screen("qr_code")
}

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route
    ) {
        composable(Screen.Dashboard.route) {
            DashboardScreen(navController)
        }
        
        composable(Screen.MotherMonitor.route) {
            MotherMonitorScreen(navController)
        }
        
        composable(Screen.Alert.route) {
            AlertScreen(navController)
        }
        
        // Lifecycle placeholders
        composable(Screen.NewbornMonitor.route) {
            NewbornMonitorScreen(navController)
        }
        composable(Screen.ToddlerMonitor.route) { /* ToddlerMonitorScreen() */ }
        composable(Screen.Timeline.route) { /* TimelineScreen() */ }
        composable(Screen.QRCode.route) {
            QRCodeScreen(navController)
        }
    }
}
