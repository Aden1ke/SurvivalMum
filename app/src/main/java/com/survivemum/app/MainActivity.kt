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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.survivemum.app.ml.CameraManager
import com.survivemum.app.ml.GemmaManager
import com.survivemum.app.ml.VitalAnalyzer
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.survivemum.app.ui.theme.SurvivalMumTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->

                    CameraScreen(
                        cameraExecutor = cameraExecutor,
                        gemmaManager = gemmaManager,
                        modifier = Modifier.padding(innerPadding)
                    ThemeDemoScreen(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    )
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
                        pregnancyHistory = "Patient has history of mild hypertension. Last 3 visits BP: 130/85, 132/88, 135/90.",
                        currentQuery = "Current heart rate is ${heartRate.toInt()} BPM and SpO2 is ${spo2.toInt()}%. Please assess risk."
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
            Text("Inference: $inferenceSpeed ms", color = androidx.compose.ui.graphics.Color.Gray, style = androidx.compose.material3.MaterialTheme.typography.labelSmall)
            Spacer(modifier = Modifier.height(8.dp))
            Text("AI Assessment:", color = androidx.compose.ui.graphics.Color.Yellow, style = androidx.compose.material3.MaterialTheme.typography.labelLarge)
            Text(aiAssessment, color = androidx.compose.ui.graphics.Color.White, style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
        }

        Text(
            text = "SurvivalMum: Eyes & Memory Active",
            modifier = Modifier.align(Alignment.TopCenter),
            color = androidx.compose.ui.graphics.Color.White
        )
    }
}
