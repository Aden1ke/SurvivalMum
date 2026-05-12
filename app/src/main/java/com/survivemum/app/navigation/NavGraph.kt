package com.survivemum.app.navigation

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

// ── Confirmed working ────────────────────────────────────────────────────────
import com.survivemum.app.ui.screens.AlertScreen
import com.survivemum.app.ui.screens.CameraMonitorScreen
import com.survivemum.app.ui.screens.DashboardScreen
import com.survivemum.app.ui.screens.HomeDashboardScreen
import com.survivemum.app.ui.screens.LoginScreen
import com.survivemum.app.ui.screens.MotherMonitorScreen
import com.survivemum.app.ui.screens.NewbornMonitorScreen
import com.survivemum.app.ui.screens.QRCodeScreen
import com.survivemum.app.ui.screens.SignupScreen
import com.survivemum.app.ui.screens.UserTypeScreen

// ── TODO: uncomment once you confirm these compile ───────────────────────────
// import com.survivemum.app.ui.screens.PatientProfileScreen
// import com.survivemum.app.ui.screens.PatientHistoryScreen
// import com.survivemum.app.ui.screens.NewbornRecordScreen
// import com.survivemum.app.ui.screens.SettingsScreen
// import com.survivemum.app.ui.screens.AlertHistoryScreen

sealed class Screen(val route: String) {
    // Auth — userType passed as path argument
    object UserType      : Screen("user_type")
    object Login         : Screen("login/{userType}") {
        fun go(userType: String) = "login/$userType"
    }
    object Signup        : Screen("signup/{userType}") {
        fun go(userType: String) = "signup/$userType"
    }

    // Home — userType passed as path argument
    object HomeDashboard : Screen("dashboard/{userType}") {
        fun go(userType: String) = "dashboard/$userType"
    }

    // Feature screens (no args)
    object Dashboard      : Screen("old_dashboard")
    object MotherMonitor  : Screen("mother_monitor")
    object NewbornMonitor : Screen("newborn_monitor")
    object ToddlerMonitor : Screen("toddler_monitor")
    object CameraMonitor  : Screen("camera_monitor")
    object PatientProfile : Screen("patient_profile")
    object PatientHistory : Screen("patient_history")
    object NewbornRecord  : Screen("newborn_record")
    object Alert          : Screen("alert")
    object AlertHistory   : Screen("alert_history")
    object QRCode         : Screen("qr_code")
    object Settings       : Screen("settings")
    object Timeline       : Screen("timeline")
}

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.UserType.route
    ) {
        // ── Auth ─────────────────────────────────────────────────────────────
        composable(Screen.UserType.route) {
            UserTypeScreen(navController = navController)
        }
        composable(Screen.Login.route) { backStackEntry ->
            val userType = backStackEntry.arguments?.getString("userType") ?: "mother"
            LoginScreen(navController = navController, userType = userType)
        }
        composable(Screen.Signup.route) { backStackEntry ->
            val userType = backStackEntry.arguments?.getString("userType") ?: "mother"
            SignupScreen(navController = navController, userType = userType)
        }

        // ── Home ─────────────────────────────────────────────────────────────
        composable(Screen.HomeDashboard.route) { backStackEntry ->
            val userType = backStackEntry.arguments?.getString("userType") ?: "mother"
            HomeDashboardScreen(navController = navController, userType = userType)
        }
        composable(Screen.Dashboard.route) {
            DashboardScreen(navController = navController)
        }

        // ── Monitoring ───────────────────────────────────────────────────────
        composable(Screen.MotherMonitor.route) {
            MotherMonitorScreen(navController = navController)
        }
        composable(Screen.NewbornMonitor.route) {
            NewbornMonitorScreen(navController = navController)
        }
        composable(Screen.CameraMonitor.route) {
            CameraMonitorScreen(navController = navController)
        }
        composable(Screen.ToddlerMonitor.route) {
            Text("Toddler Monitor — coming soon")
        }
        composable(Screen.Timeline.route) {
            Text("Timeline — coming soon")
        }

        // ── Patient ──────────────────────────────────────────────────────────
        composable(Screen.PatientProfile.route) {
            // PatientProfileScreen(navController) — uncomment once confirmed
            Text("Patient Profile — coming soon")
        }
        composable(Screen.PatientHistory.route) {
            // PatientHistoryScreen(navController) — uncomment once confirmed
            Text("Patient History — coming soon")
        }
        composable(Screen.NewbornRecord.route) {
            // NewbornRecordScreen(navController) — uncomment once confirmed
            Text("Newborn Record — coming soon")
        }

        // ── Utilities ────────────────────────────────────────────────────────
        composable(Screen.Alert.route) {
            AlertScreen(navController = navController)
        }
        composable(Screen.AlertHistory.route) {
            // AlertHistoryScreen(navController) — uncomment once confirmed
            Text("Alert History — coming soon")
        }
        composable(Screen.QRCode.route) {
            QRCodeScreen(navController = navController)
        }
        composable(Screen.Settings.route) {
            // SettingsScreen(navController) — uncomment once confirmed
            Text("Settings — coming soon")
        }
    }
}