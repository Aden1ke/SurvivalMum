package com.survivemum.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.survivemum.app.ui.screens.CameraMonitorScreen

sealed class Screen(val route: String) {
    object CameraMonitor : Screen("camera_monitor")
    object Alert : Screen("alert")
    object ANCScanner : Screen("anc_scanner")
    object Referral : Screen("referral")
    object Emergency : Screen("emergency")
    object Newborn : Screen("newborn")
    object QRCode : Screen("qr_code")
}

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.CameraMonitor.route
    ) {
        composable(Screen.CameraMonitor.route) {
            CameraMonitorScreen(navController)
        }
        
        // Placeholder routes for other screens
        composable(Screen.Alert.route) { /* AlertScreen() */ }
        composable(Screen.ANCScanner.route) { /* ANCScannerScreen() */ }
        composable(Screen.Referral.route) { /* ReferralScreen() */ }
        composable(Screen.Emergency.route) { /* EmergencyScreen() */ }
        composable(Screen.Newborn.route) { /* NewbornScreen() */ }
        composable(Screen.QRCode.route) { /* QRCodeScreen() */ }
    }
}
