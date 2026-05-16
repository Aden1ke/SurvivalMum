package com.survivemum.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.survivemum.app.data.SurviveMumDatabase
import com.survivemum.app.ml.GemmaManager
import com.survivemum.app.navigation.NavGraph
import com.survivemum.app.security.SecurityModule
import com.survivemum.app.ui.screens.applyLanguage
import com.survivemum.app.ui.theme.SurvivalMumTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {

    /**
     * Container for the security layer — audit log, safety screener,
     * alert dispatcher, battery monitor, model router.
     *
     * Initialized once in onCreate and held for the activity's lifetime.
     */
    private lateinit var securityModule: SecurityModule

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var gemmaManager: GemmaManager

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* permission result handled inside composables */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ── Apply saved language BEFORE UI loads ──────────────────────────────
        // Ensures the correct language is shown from the first frame.
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val db = SurviveMumDatabase.getDatabase(this@MainActivity)
                val user = db.userDao().getCurrentUser()
                val lang = user?.language ?: "en"
                runOnUiThread {
                    applyLanguage(this@MainActivity, lang)
                }
            } catch (e: Exception) {
                // If DB not ready yet, default to English — no crash
            }
        }

        // ── Camera executor ───────────────────────────────────────────────────
        cameraExecutor = Executors.newSingleThreadExecutor()

        // ── Gemma model — create instance and start loading in background ────
        // We create the GemmaManager reference now (cheap, just allocates the
        // object), but the actual model load runs on IO. SecurityModule below
        // gets the reference and the safety screener will call into Gemma once
        // it's loaded. While loading, Layer 2 returns SAFE and Layer 1 still works.
        gemmaManager = GemmaManager(this)
        lifecycleScope.launch(Dispatchers.IO) {
            gemmaManager.initializeModel("gemma4.bin")
        }

        // ── Security layer (with Gemma as Layer 2 safety classifier) ──────────
        // Creates the safety audit database, starts the connectivity listener,
        // wires Gemma into the safety screener, and prepares the alert dispatcher
        // to receive queued alerts gated by two-layer safety screening.
        securityModule = SecurityModule(applicationContext, gemmaManager)

        // ── Camera permission ─────────────────────────────────────────────────
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        enableEdgeToEdge()

        setContent {
            SurvivalMumTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = MaterialTheme.colorScheme.background,
                    contentWindowInsets = WindowInsets(0, 0, 0, 0)
                ) { _ ->
                    val navController = rememberNavController()
                    NavGraph(navController = navController)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}