package com.survivemum.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.survivemum.app.ml.GemmaManager
import com.survivemum.app.ui.screens.HomeDashboardScreen
import com.survivemum.app.ui.screens.LoginScreen
import com.survivemum.app.ui.screens.SignupScreen
import com.survivemum.app.ui.screens.UserTypeScreen
import com.survivemum.app.ui.theme.SurvivalMumTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var gemmaManager: GemmaManager

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission granted
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraExecutor = Executors.newSingleThreadExecutor()
        gemmaManager = GemmaManager(this)

        // Initialize Gemma in background
        lifecycleScope.launch(Dispatchers.IO) {
            gemmaManager.initializeModel("gemma4.bin")
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

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

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}