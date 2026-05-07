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
import com.survivemum.app.ml.CameraManager
import com.survivemum.app.ml.VitalAnalyzer
import com.survivemum.app.ui.theme.SurvivalMumTheme
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {
    private lateinit var cameraExecutor: ExecutorService

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
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        enableEdgeToEdge()
        setContent {
            SurvivalMumTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    CameraScreen(
                        cameraExecutor = cameraExecutor,
                        modifier = Modifier.padding(innerPadding)
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
fun CameraScreen(cameraExecutor: ExecutorService, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    
    var heartRate by remember { mutableDoubleStateOf(0.0) }
    var spo2 by remember { mutableDoubleStateOf(0.0) }

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
        }

        Text(
            text = "SurvivalMum: Eyes Active",
            modifier = Modifier.align(Alignment.TopCenter),
            color = androidx.compose.ui.graphics.Color.White
        )
    }
}
