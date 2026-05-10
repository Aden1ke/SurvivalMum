package com.survivemum.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.survivemum.app.navigation.Screen
import com.survivemum.app.ui.components.StatusPill
import com.survivemum.app.ui.components.VitalCard
import com.survivemum.app.ui.theme.SurvivalMumTheme
import com.survivemum.app.viewmodel.VitalsViewModel
import com.survivemum.app.model.VitalsState
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CameraMonitorScreen(
    navController: NavController,
    viewModel: VitalsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isReady by viewModel.isEngineReady.collectAsState()
    val alertTriggered by viewModel.alertTriggered.collectAsState()
    
    CameraMonitorContent(
        uiState = uiState,
        isReady = isReady,
        alertTriggered = alertTriggered,
        onNavigateToAlert = { navController.navigate(Screen.Alert.route) }
    )
}

@Composable
fun CameraMonitorContent(
    uiState: VitalsState,
    isReady: Boolean,
    alertTriggered: Boolean,
    onNavigateToAlert: () -> Unit
) {
    var isRecording by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { isRecording = !isRecording },
                containerColor = if (isRecording) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                },
                contentColor = if (isRecording) {
                    MaterialTheme.colorScheme.onError
                } else {
                    MaterialTheme.colorScheme.onPrimary
                },
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
            // Header Section
            MonitorHeader(isReady)

            // Camera Feed Section
            CameraFeedBox(isReady, alertTriggered)

            // Vitals Grid Section
            VitalsGrid(uiState)

            // Blood Pressure Section
            BloodPressureCard(uiState)

            // Action Section
            Button(
                onClick = onNavigateToAlert,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("🚨 Test Alert Screen", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun MonitorHeader(isReady: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Camera Monitor",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Live maternal vitals via AI",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        StatusPill(
            label = if (isReady) "LITERT READY" else "INIT ENGINE...",
            isActive = isReady
        )
    }
}

@Composable
private fun CameraFeedBox(isReady: Boolean, alertTriggered: Boolean) {
    val accentColor = MaterialTheme.colorScheme.primary
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        CornerBrackets(Modifier.matchParentSize(), color = accentColor)

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Visibility,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = accentColor
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isReady) "AI Watching" else "Engine Loading...",
                style = MaterialTheme.typography.labelLarge,
                color = accentColor,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Analysing maternal signals...",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (alertTriggered) {
            AlertOverlay()
        }
    }
}

@Composable
private fun AlertOverlay() {
    val infiniteTransition = rememberInfiniteTransition(label = "alert")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.error.copy(alpha = alpha)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "⚠️ RISK DETECTED",
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
private fun VitalsGrid(uiState: VitalsState) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        item { VitalCard("Heart Rate", uiState.hr.toString(), "bpm") }
        item { VitalCard("SpO₂", "${uiState.spo2}%", "") }
        item { VitalCard("Breathing", uiState.rr.toString(), "rpm") }
        item { VitalCard("Temperature", "${uiState.temp}°C", "") }
    }
}

@Composable
private fun BloodPressureCard(uiState: VitalsState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "💉 Blood Pressure",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = uiState.bp,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "mmHg",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Last update",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(uiState.timestamp)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun CornerBrackets(modifier: Modifier = Modifier, color: Color) {
    Box(modifier = modifier.padding(12.dp)) {
        val bracketSize = 20.dp
        val strokeWidth = 2.dp

        Box(modifier = Modifier.size(bracketSize).align(Alignment.TopStart).drawCorner(color, strokeWidth, true, true))
        Box(modifier = Modifier.size(bracketSize).align(Alignment.TopEnd).drawCorner(color, strokeWidth, true, false))
        Box(modifier = Modifier.size(bracketSize).align(Alignment.BottomStart).drawCorner(color, strokeWidth, false, true))
        Box(modifier = Modifier.size(bracketSize).align(Alignment.BottomEnd).drawCorner(color, strokeWidth, false, false))
    }
}

fun Modifier.drawCorner(color: Color, strokeWidth: androidx.compose.ui.unit.Dp, isTop: Boolean, isLeft: Boolean): Modifier = this.then(
    Modifier.drawWithCache {
        onDrawWithContent {
            val stroke = strokeWidth.toPx()
            val sizePx = size.width
            drawRect(color, Offset(0f, if (isTop) 0f else sizePx - stroke), Size(sizePx, stroke))
            drawRect(color, Offset(if (isLeft) 0f else sizePx - stroke, 0f), Size(stroke, sizePx))
        }
    }
)

@Preview(showBackground = true)
@Composable
fun CameraMonitorPreview() {
    SurvivalMumTheme {
        CameraMonitorContent(
            uiState = VitalsState(hr = 75, spo2 = 98, rr = 16, temp = 36.6, bp = "120/80"),
            isReady = true,
            alertTriggered = false,
            onNavigateToAlert = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun CameraMonitorAlertPreview() {
    SurvivalMumTheme(darkTheme = true) {
        CameraMonitorContent(
            uiState = VitalsState(hr = 110, spo2 = 94, rr = 22, temp = 38.2, bp = "145/95"),
            isReady = true,
            alertTriggered = true,
            onNavigateToAlert = {}
        )
    }
}
