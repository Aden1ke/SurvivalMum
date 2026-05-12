package com.survivemum.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.survivemum.app.ui.components.AIStatusIndicator
import com.survivemum.app.ui.components.ThinkingTracePanel
import com.survivemum.app.viewmodel.NewbornViewModel

@Composable
fun NewbornMonitorScreen(
    navController: NavController,
    viewModel: NewbornViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isReady by viewModel.isEngineReady.collectAsState()

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                        Text(
                            text = "NEONATAL CONSOLE",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                        )
                    }
                    AIStatusIndicator()
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Live Monitoring Header
            MonitoringStatusHeader(uiState.isDistressDetected)

            // Audio & Breathing Visualization Section
            MonitoringVisualizationRow(uiState)

            // Cry Classification Card
            CryAnalysisCard(uiState.cryStatus, uiState.confidence)

            // Jaundice Detection Section
            JaundiceMeter(uiState.jaundiceRisk)

            // AI Reasoning
            ThinkingTracePanel(
                steps = uiState.reasoningSteps,
                confidence = uiState.confidence
            )

            // Emergency Action
            if (uiState.isDistressDetected) {
                Button(
                    onClick = { /* Emergency Protocol */ },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.PriorityHigh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("START RESUSCITATION PROTOCOL", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun MonitoringStatusHeader(isDistress: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isDistress) MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(
                    if (isDistress) MaterialTheme.colorScheme.error.copy(alpha = alpha)
                    else MaterialTheme.colorScheme.primary.copy(alpha = alpha)
                )
        )
        Text(
            text = if (isDistress) "DISTRESS DETECTED" else "LIVE MONITORING ACTIVE",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = if (isDistress) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun MonitoringVisualizationRow(uiState: com.survivemum.app.viewmodel.NewbornUIState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Breathing Waveform
        Card(
            modifier = Modifier.weight(1.5f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    "BREATHING RHYTHM",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.secondary
                )
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    AnimatedWaveform(uiState.waveformData, color = MaterialTheme.colorScheme.primary)
                    Text(
                        "${uiState.breathingRate} rpm",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                        modifier = Modifier.align(Alignment.BottomEnd)
                    )
                }
            }
        }

        // Pulse/Rate indicator
        Card(
            modifier = Modifier.weight(1f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.Favorite,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    "PULSE",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    "128",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

@Composable
fun CryAnalysisCard(status: String, confidence: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.GraphicEq, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Column {
                Text(
                    "AI CRY CLASSIFICATION",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    status,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                LinearProgressIndicator(
                    progress = confidence / 100f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
}

@Composable
fun JaundiceMeter(risk: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "JAUNDICE SKIN ANALYSIS",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    "${(risk * 100).toInt()}% RISK",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (risk > 0.3) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        androidx.compose.ui.graphics.Brush.horizontalGradient(
                            listOf(Color.Yellow.copy(alpha = 0.1f), Color.Yellow, Color.Red)
                        )
                    )
            ) {
                // Risk Pointer
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(2.dp)
                        .background(Color.Black)
                        .align(Alignment.CenterStart)
                        .offset(x = (risk * 300).dp) // Simplified offset for demo
                )
            }
        }
    }
}

@Composable
fun AnimatedWaveform(data: List<Float>, color: Color) {
    val strokeWidth = 2.dp
    Spacer(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 20.dp)
            .drawWithCache {
                val path = Path()
                onDrawBehind {
                    if (data.isNotEmpty()) {
                        val width = size.width
                        val height = size.height
                        val step = width / (data.size - 1)
                        
                        path.reset()
                        path.moveTo(0f, height / 2 + (data[0] - 0.5f) * height)
                        
                        data.forEachIndexed { index, value ->
                            path.lineTo(index * step, height / 2 + (value - 0.5f) * height)
                        }
                        
                        drawPath(
                            path = path,
                            color = color,
                            style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
                        )
                    }
                }
            }
    )
}
