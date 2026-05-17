package com.survivemum.app.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.survivemum.app.ui.screens.*

sealed class Screen(val route: String) {

    // ── Auth ──────────────────────────────────────────────────────────────────
    object UserType : Screen("user_type")

    object Login : Screen("login/{userType}") {
        fun go(userType: String) = "login/$userType"
    }

    // Signup handles "TBA", "mother", AND "PATIENT" (TBA registering a patient)
    object Signup : Screen("signup/{userType}") {
        fun go(userType: String) = "signup/$userType"
    }

    // ── Home ──────────────────────────────────────────────────────────────────
    object HomeDashboard : Screen("home/{userType}") {
        fun go(userType: String) = "home/$userType"
    }

    object Dashboard : Screen("old_dashboard")

    // ── Monitoring ────────────────────────────────────────────────────────────
    object MotherMonitor  : Screen("mother_monitor")
    object NewbornMonitor : Screen("newborn_monitor")
    object ToddlerMonitor : Screen("toddler_monitor")
    object CameraMonitor  : Screen("camera_monitor")

    // ── Patient — patientId as path arg ───────────────────────────────────────
    object PatientProfile : Screen("patient/{patientId}") {
        fun go(patientId: String) = "patient/$patientId"
    }
    object PatientHistory : Screen("history/{patientId}") {
        fun go(patientId: String) = "history/$patientId"
    }
    object AlertHistory : Screen("alerts/{patientId}") {
        fun go(patientId: String) = "alerts/$patientId"
    }
    object NewbornRecord : Screen("newborn/{patientId}") {
        fun go(patientId: String) = "newborn/$patientId"
    }

    // ── Utilities ─────────────────────────────────────────────────────────────
    object Alert    : Screen("alert")
    object QRCode   : Screen("qr_code")
    object Settings : Screen("settings")
    object Timeline : Screen("timeline")
}

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.UserType.route
    ) {

        // ── Auth ──────────────────────────────────────────────────────────────
        composable(Screen.UserType.route) {
            UserTypeScreen(navController = navController)
        }
        composable(Screen.Login.route) { back ->
            val userType = back.arguments?.getString("userType") ?: "mother"
            LoginScreen(navController = navController, userType = userType)
        }
        // Fix 5: "PATIENT" is a valid userType here — SignupScreen handles it
        // differently (creates PatientEntity not UserEntity, different back stack)
        composable(Screen.Signup.route) { back ->
            val userType = back.arguments?.getString("userType") ?: "mother"
            SignupScreen(navController = navController, userType = userType)
        }

        // ── Home ──────────────────────────────────────────────────────────────
        composable(Screen.HomeDashboard.route) { back ->
            val userType = back.arguments?.getString("userType") ?: "mother"
            HomeDashboardScreen(navController = navController, userType = userType)
        }
        composable(Screen.Dashboard.route) {
            DashboardScreen(navController = navController)
        }

        // ── Monitoring ────────────────────────────────────────────────────────
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
            ComingSoonScreen(name = "Toddler Monitor")
        }
        composable(Screen.Timeline.route) {
            ComingSoonScreen(name = "Timeline")
        }

        // ── Patient ───────────────────────────────────────────────────────────
        composable(Screen.PatientProfile.route) { back ->
            val patientId = back.arguments?.getString("patientId") ?: return@composable
            PatientProfileScreen(navController = navController, patientId = patientId)
        }
        // Fix 6: PatientHistoryScreen is now wired up
        composable(Screen.PatientHistory.route) { back ->
            val patientId = back.arguments?.getString("patientId") ?: return@composable
            PatientHistoryScreen(navController = navController, patientId = patientId)
        }
        composable(Screen.NewbornRecord.route) { back ->
            val patientId = back.arguments?.getString("patientId") ?: return@composable
            NewbornRecordScreen(navController = navController, patientId = patientId)
        }
        composable(Screen.AlertHistory.route) { back ->
            val patientId = back.arguments?.getString("patientId") ?: return@composable
            AlertHistoryScreen(navController = navController, patientId = patientId)
        }

        // ── Utilities ─────────────────────────────────────────────────────────
        composable(Screen.Alert.route) {
            AlertScreen(navController = navController)
        }
        composable(Screen.QRCode.route) {
            QRCodeScreen(navController = navController)
        }
        composable(Screen.Settings.route) {
            SettingsScreen(navController = navController)
        }
    }
}

// Placeholder shown for screens not yet built
@Composable
private fun ComingSoonScreen(name: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(name, style = MaterialTheme.typography.titleLarge)
            Text("Coming soon", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}