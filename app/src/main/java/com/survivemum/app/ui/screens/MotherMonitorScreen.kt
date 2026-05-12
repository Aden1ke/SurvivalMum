package com.survivemum.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.survivemum.app.navigation.Screen
import com.survivemum.app.ui.components.AIStatusIndicator
import com.survivemum.app.ui.components.ThinkingTracePanel
import com.survivemum.app.viewmodel.MotherMonitorViewModel
import com.survivemum.app.viewmodel.RiskLevel
import androidx.compose.foundation.BorderStroke

@Composable
fun MotherMonitorScreen(
    navController: NavController,
    viewModel: MotherMonitorViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // Background Color based on Risk Level for immersion
    val backgroundColor by animateColorAsState(
        targetValue = when (uiState.riskLevel) {
            RiskLevel.NORMAL -> MaterialTheme.colorScheme.surface
            RiskLevel.WARNING -> Color(0xFF332B00) // Deep yellow/amber tint
            RiskLevel.CRITICAL -> Color(0xFF2B0000) // Deep red tint
        },
        animationSpec = tween(1000),
        label = "bg"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        // 1. Fullscreen Immersive Camera Placeholder (Cinematic overlay)
        CameraImmersiveLayer(uiState.riskLevel)

        // 2. UI Overlay Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            // Header Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    IconButton(
                        onClick = { navController.popBackStack() },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                        )
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "MATERNAL OS",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp
                        ),
                        color = Color.White
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    AIStatusIndicator(
                        modifier = Modifier.background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    AIActivityBadge(uiState.isScanning)
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // 3. Floating Vital Cards Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FloatingVitalCard(
                    label = "HEART RATE",
                    value = uiState.hr.toString(),
                    unit = "BPM",
                    isAlert = uiState.hr > 100,
                    modifier = Modifier.weight(1f)
                )
                FloatingVitalCard(
                    label = "RESP RATE",
                    value = uiState.rr.toString(),
                    unit = "RPM",
                    isAlert = uiState.rr > 22,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FloatingVitalCard(
                    label = "SPO2",
                    value = "${uiState.spo2}%",
                    unit = "",
                    isAlert = uiState.spo2 < 95,
                    modifier = Modifier.weight(1f)
                )
                FloatingVitalCard(
                    label = "BLOOD PRESS",
                    value = uiState.bp,
                    unit = "mmHg",
                    isAlert = uiState.riskLevel == RiskLevel.CRITICAL,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 4. Trend Intelligence & Reasoning Panel
            AnimatedVisibility(
                visible = uiState.riskLevel != RiskLevel.NORMAL,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Quick Reasoning Preview
                    ThinkingTracePanel(
                        steps = uiState.reasoningSteps,
                        confidence = 94
                    )
                    
                    // Critical Action Button
                    if (uiState.riskLevel == RiskLevel.CRITICAL) {
                        Button(
                            onClick = { navController.navigate(Screen.Alert.route) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("VIEW FULL AI DIAGNOSIS", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CameraImmersiveLayer(risk: RiskLevel) {
    val infiniteTransition = rememberInfiniteTransition(label = "scan")
    val scanY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scanline"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // Scanning Line Simulation
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .align(Alignment.TopCenter)
                .graphicsLayer {
                    translationY = scanY * size.height * 10f // Dynamic but efficient
                }
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Cyan.copy(alpha = 0.5f), Color.Transparent)
                    )
                )
        )

        // Corner Framing
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
        )

        // Center Intelligence Hub Icon
        Box(
            modifier = Modifier
                .size(120.dp)
                .align(Alignment.Center)
                .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Visibility,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = Color.White.copy(alpha = 0.2f)
            )
        }
    }
}

@Composable
fun FloatingVitalCard(
    label: String,
    value: String,
    unit: String,
    isAlert: Boolean,
    modifier: Modifier = Modifier
) {
    val alertColor by animateColorAsState(
        targetValue = if (isAlert) MaterialTheme.colorScheme.error else Color.White,
        animationSpec = tween(500),
        label = "alert"
    )

    Surface(
        modifier = modifier,
        color = Color.Black.copy(alpha = 0.7f),
        shape = RoundedCornerShape(12.dp),
        border = if (isAlert) BorderStroke(1.dp, MaterialTheme.colorScheme.error) else null
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = Color.White.copy(alpha = 0.6f)
            )
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                    color = alertColor
                )
                Text(
                    text = unit,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            
            // Small Pulsing Indicator for "Live" feel
            if (!isAlert) {
                Box(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(Color.Cyan)
                )
            }
        }
    }
}

@Composable
fun AIActivityBadge(isScanning: Boolean) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Cyan.copy(alpha = 0.1f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (isScanning) {
            val infiniteTransition = rememberInfiniteTransition(label = "spin")
            val rotation by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing)),
                label = "rotate"
            )
            Icon(
                Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = Color.Cyan
            )
        }
        Text(
            text = "AI ANALYZING",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
            color = Color.Cyan
        )
    }
}
