package com.survivemum.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.survivemum.app.data.SurviveMumDatabase
import com.survivemum.app.ml.CameraManager
import com.survivemum.app.ml.GemmaManager
import com.survivemum.app.ml.VitalAnalyzer
import com.survivemum.app.navigation.NavGraph
import com.survivemum.app.security.SecurityModule
import com.survivemum.app.ui.screens.DesignSystemDemoScreen
import com.survivemum.app.ui.screens.applyLanguage
import com.survivemum.app.ui.theme.SurvivalMumTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

@Composable
fun CameraScreen(
    cameraExecutor: ExecutorService,
    gemmaManager: GemmaManager,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    val scope = rememberCoroutineScope()

    var heartRate by remember { mutableDoubleStateOf(0.0) }
    var spo2 by remember { mutableDoubleStateOf(0.0) }
    var aiAssessment by remember { mutableStateOf("Initializing AI...") }
    var inferenceSpeed by remember { mutableLongStateOf(0L) }

    val analyzer = remember {
        VitalAnalyzer(context) { hr, s2 ->
            heartRate = hr
            spo2 = s2
        }
    }

    LaunchedEffect(previewView) {
        val cameraManager = CameraManager(
            context,
            previewView,
            lifecycleOwner,
            cameraExecutor,
            analyzer
        )
        cameraManager.startCamera()
    }

    // Periodically run AI assessment when heart rate is detected
    LaunchedEffect(heartRate) {
        if (heartRate > 0) {
            scope.launch {
                val startTime = System.currentTimeMillis()
                val result = withContext(Dispatchers.IO) {
                    gemmaManager.assess(
                        clinicalSituation = "Current heart rate is ${heartRate.toInt()} BPM and SpO2 is ${spo2.toInt()}%. Patient has history of mild hypertension. Last 3 visits BP: 130/85, 132/88, 135/90. Please assess risk.",
                        patientName = "the patient"
                    )
                }
                inferenceSpeed = System.currentTimeMillis() - startTime
                aiAssessment = result
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        // Vital Signs Overlay
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
                .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.5f))
                .padding(8.dp)
        ) {
            Text("Heart Rate: ${heartRate.toInt()} BPM", color = androidx.compose.ui.graphics.Color.Red)
            Text("SpO2: ${spo2.toInt()}%", color = androidx.compose.ui.graphics.Color.Cyan)
            Text("Inference: $inferenceSpeed ms", color = androidx.compose.ui.graphics.Color.Gray, style = MaterialTheme.typography.labelSmall)
            Spacer(modifier = Modifier.height(8.dp))
            Text("AI Assessment:", color = androidx.compose.ui.graphics.Color.Yellow, style = MaterialTheme.typography.labelLarge)
            Text(aiAssessment, color = androidx.compose.ui.graphics.Color.White, style = MaterialTheme.typography.bodySmall)
        }

        Text(
            text = "SurvivalMum: Eyes & Memory Active",
            modifier = Modifier.align(Alignment.TopCenter),
            color = androidx.compose.ui.graphics.Color.White
        )
    }
}

@Composable
fun ThemeDemoScreen(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "SurviveMum: Hospital Mode",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )

            // High-Contrast Emergency Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.error, RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = "🚨 CRITICAL ALERT DETECTED",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onError,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            // Card using Surface Color
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Vitals Summary",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Heart Rate: 82 bpm\nSpO2: 98%\nBP: 120/80",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Primary Button
            Button(
                onClick = { },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Acknowledge Patient", style = MaterialTheme.typography.bodyLarge)
            }

            // Tertiary Button
            OutlinedButton(
                onClick = { },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                border = ButtonDefaults.outlinedButtonBorder.copy(width = 2.dp)
            ) {
                Text("View Full History", color = MaterialTheme.colorScheme.primary)
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "ACCESSIBILITY: WCAG AAA CONTRAST ACTIVE",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ThemeDemoPreview() {
    SurvivalMumTheme(darkTheme = false) {
        ThemeDemoScreen()
    }
}

@Preview(showBackground = true, showSystemUi = true, backgroundColor = 0xFF000000)
@Composable
fun ThemeDemoDarkPreview() {
    SurvivalMumTheme(darkTheme = true) {
        ThemeDemoScreen()
    }
}