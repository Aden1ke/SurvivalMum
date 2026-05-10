package com.survivemum.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.survivemum.app.ui.screens.HomeDashboardScreen
import com.survivemum.app.ui.screens.LoginScreen
import com.survivemum.app.ui.screens.SignupScreen
import com.survivemum.app.ui.screens.UserTypeScreen
import com.survivemum.app.ui.theme.SurvivalMumTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SurvivalMumTheme {
                val navController = rememberNavController()
                NavHost(
                    navController = navController,
                    startDestination = "user_type"
                ) {
                    composable("user_type") {
                        UserTypeScreen(navController = navController)
                    }
                    composable("login/{userType}") { backStackEntry ->
                        val userType = backStackEntry.arguments?.getString("userType") ?: "TBA"
                        LoginScreen(navController = navController, userType = userType)
                    }
                    composable("signup/{userType}") { backStackEntry ->
                        val userType = backStackEntry.arguments?.getString("userType") ?: "TBA"
                        SignupScreen(navController = navController, userType = userType)
                    }
                    composable("dashboard/{userType}") { backStackEntry ->
                        val userType = backStackEntry.arguments?.getString("userType") ?: "TBA"
                        HomeDashboardScreen(navController = navController, userType = userType)
                    }
                }
            }
        }
    }
}