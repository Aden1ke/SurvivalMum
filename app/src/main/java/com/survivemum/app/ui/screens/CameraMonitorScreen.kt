package com.survivemum.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.survivemum.app.navigation.Screen
import com.survivemum.app.ui.components.StatusPill
import com.survivemum.app.ui.components.VitalCard
import com.survivemum.app.viewmodel.VitalsViewModel
import com.survivemum.app.model.VitalsState
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors


@Composable
fun CameraMonitorScreen(
    navController: NavController,
    viewModel: VitalsViewModel = viewModel()
) {
    val uiState      by viewModel.uiState.collectAsState()
    val isReady      by viewModel.isEngineReady.collectAsState()
    val alertTriggered by viewModel.alertTriggered.collectAsState()

    val context        = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Check camera permission
    val hasCameraPermission = remember {
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    // Single-thread executor for CameraX image analysis
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    // Bind CameraX when permission is granted
    if (hasCameraPermission) {
        val previewView = remember { PreviewView(context) }

        LaunchedEffect(Unit) {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        // Wire the real VitalAnalyzer from the ViewModel
                        it.setAnalyzer(cameraExecutor, viewModel.analyzer)
                    }

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_FRONT_CAMERA,
                        preview,
                        imageAnalysis
                    )
                } catch (e: Exception) {
                    android.util.Log.e("CameraMonitorScreen", "Camera bind failed", e)
                }
            }, ContextCompat.getMainExecutor(context))
        }

        DisposableEffect(Unit) {
            onDispose { cameraExecutor.shutdown() }
        }

        CameraMonitorContent(
            uiState          = uiState,
            isReady          = isReady,
            alertTriggered   = alertTriggered,
            previewView      = previewView,
            onNavigateToAlert = { navController.navigate(Screen.Alert.route) }
        )
    } else {
        // Permission not granted — show a clear message
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(32.dp)
            ) {
                Text("Camera Permission Required",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold)
                Text(
                    "SurviveMum needs camera access to measure vitals using rPPG. " +
                            "Grant permission in Settings → Apps → SurviveMum → Permissions.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun CameraMonitorContent(
    uiState           : VitalsState,
    isReady           : Boolean,
    alertTriggered    : Boolean,
    previewView       : PreviewView,
    onNavigateToAlert : () -> Unit
) {
    var isRecording by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { isRecording = !isRecording },
                containerColor = if (isRecording) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.primary,
                contentColor = if (isRecording) MaterialTheme.colorScheme.onError
                else MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Mic, contentDescription = "Voice Input")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Camera Monitor",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold)
                    Text("Live maternal vitals via rPPG",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                StatusPill(
                    label    = if (isReady) "LITERT READY" else "INIT ENGINE...",
                    isActive = isReady
                )
            }

            // ── Real camera preview ───────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                // AndroidView embeds the CameraX PreviewView in Compose
                AndroidView(
                    factory  = { previewView },
                    modifier = Modifier.fillMaxSize()
                )

                // Corner brackets overlay
                CornerBrackets(
                    modifier = Modifier.matchParentSize(),
                    color    = MaterialTheme.colorScheme.primary
                )

                // Status label at bottom of preview
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        if (isReady) "Analysing skin signal..." else "Loading engine...",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }

                // Alert overlay pulsing when vitals are critical
                if (alertTriggered) {
                    AlertOverlay()
                }
            }

            // Vitals grid — now shows real rPPG values from VitalAnalyzer
            // HR and SpO2 update every ~5 seconds when the rPPG buffer fills
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement   = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 200.dp)
            ) {
                item {
                    VitalCard(
                        "Heart Rate",
                        if (uiState.hr > 0) uiState.hr.toString() else "...",
                        "bpm"
                    )
                }
                item {
                    VitalCard(
                        "SpO₂",
                        if (uiState.spo2 > 0) "${uiState.spo2}%" else "...",
                        ""
                    )
                }
                item {
                    VitalCard(
                        "Breathing",
                        if (uiState.rr > 0) uiState.rr.toString() else "...",
                        "rpm"
                    )
                }
                item {
                    VitalCard(
                        "Temperature",
                        if (uiState.temp > 0) "${uiState.temp}°C" else "N/A",
                        ""
                    )
                }
            }

            // BP card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("💉 Blood Pressure",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            if (uiState.bp == "0/0") "Requires cuff" else uiState.bp,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text("mmHg",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Last update",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                                .format(Date(uiState.timestamp)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Alert navigation
            Button(
                onClick = onNavigateToAlert,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor   = MaterialTheme.colorScheme.onErrorContainer),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("🚨 View Alert Screen",
                    style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

// Keep CornerBrackets and AlertOverlay from original CameraMonitorScreen
@Composable
fun AlertOverlay() {
    val infiniteTransition = rememberInfiniteTransition(label = "alert")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.1f, targetValue = 0.4f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "alpha"
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.error.copy(alpha = alpha)),
        contentAlignment = Alignment.Center
    ) {
        Text("⚠️ RISK DETECTED",
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black)
    }
}

@Composable
fun CornerBrackets(modifier: Modifier = Modifier, color: Color) {
    Box(modifier = modifier.padding(12.dp)) {
        val bracketSize = 20.dp
        val strokeWidth = 2.dp
        Box(Modifier.size(bracketSize).align(Alignment.TopStart)
            .drawCorner(color, strokeWidth, true, true))
        Box(Modifier.size(bracketSize).align(Alignment.TopEnd)
            .drawCorner(color, strokeWidth, true, false))
        Box(Modifier.size(bracketSize).align(Alignment.BottomStart)
            .drawCorner(color, strokeWidth, false, true))
        Box(Modifier.size(bracketSize).align(Alignment.BottomEnd)
            .drawCorner(color, strokeWidth, false, false))
    }
}

fun Modifier.drawCorner(
    color: Color,
    strokeWidth: androidx.compose.ui.unit.Dp,
    isTop: Boolean,
    isLeft: Boolean
): Modifier = this.then(
    Modifier.drawWithCache {
        onDrawWithContent {
            val stroke = strokeWidth.toPx()
            val sizePx = size.width
            drawRect(color, Offset(0f, if (isTop) 0f else sizePx - stroke), Size(sizePx, stroke))
            drawRect(color, Offset(if (isLeft) 0f else sizePx - stroke, 0f), Size(stroke, sizePx))
        }
    }
)