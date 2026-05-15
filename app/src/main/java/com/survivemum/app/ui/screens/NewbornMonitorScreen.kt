package com.survivemum.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.survivemum.app.data.PatientEntity
import com.survivemum.app.data.SurviveMumDatabase
import com.survivemum.app.ml.CryClassifier
import com.survivemum.app.navigation.Screen
import com.survivemum.app.ui.components.AIStatusIndicator
import com.survivemum.app.ui.components.ThinkingTracePanel
import com.survivemum.app.ui.theme.*
import com.survivemum.app.viewmodel.NewbornUIState
import com.survivemum.app.viewmodel.NewbornViewModel
import androidx.compose.foundation.layout.statusBarsPadding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun NewbornMonitorScreen(
    navController: NavController,
    viewModel: NewbornViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isReady by viewModel.isEngineReady.collectAsState()

    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    var patients by remember { mutableStateOf<List<PatientEntity>>(emptyList()) }

    // ── Cry test state ────────────────────────────────────────────────────────
    var cryTestResult  by remember { mutableStateOf<String?>(null) }
    var cryTestRunning by remember { mutableStateOf(false) }
    var cryTestColor   by remember { mutableStateOf(Color.Gray) }

    // Load patients so we can link to their newborn records
    LaunchedEffect(Unit) {
        val db   = SurviveMumDatabase.getDatabase(context)
        val user = db.userDao().getCurrentUser()
        if (user != null) {
            db.patientDao().getAllPatients(user.userId).collect { patients = it }
        }
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .statusBarsPadding()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                        Text(
                            text = "NEONATAL CONSOLE",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black, letterSpacing = 1.sp
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
            MonitoringStatusHeader(uiState.isDistressDetected)
            MonitoringVisualizationRow(uiState)
            CryAnalysisCard(uiState.cryStatus, uiState.confidence)
            JaundiceMeter(uiState.jaundiceRisk)
            ThinkingTracePanel(steps = uiState.reasoningSteps, confidence = uiState.confidence)

            // ── CRY CLASSIFIER TEST ───────────────────────────────────────────
            // Tests all 4 clinical cry patterns so you can demo each one
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "AI CRY CLASSIFIER TEST",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold, letterSpacing = 1.sp
                        ),
                        color = SurviveMumRed
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Tap a pattern to classify it using Gemma 4",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))

                    // 4 cry pattern buttons — one per clinical category
                    val cryPatterns = listOf(
                        Triple("🔴 DISTRESS",      550f, 0.3f),   // meningitis pattern
                        Triple("🔴 RESPIRATORY",   180f, 3.5f),   // respiratory distress
                        Triple("🟡 PAIN",          380f, 2.0f),   // pain cry
                        Triple("🟢 NORMAL",        320f, 1.5f)    // healthy cry
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        cryPatterns.take(2).forEach { (label, pitch, burst) ->
                            OutlinedButton(
                                onClick = {
                                    cryTestRunning = true
                                    cryTestResult  = "Analysing..."
                                    scope.launch(Dispatchers.IO) {
                                        try {
                                            val cry = CryClassifier(context)
                                            cry.initialize()
                                            val result = cry.classify(pitch, burst)
                                            withContext(Dispatchers.Main) {
                                                cryTestResult = "${result.label}\n${result.severity}\n${result.description}"
                                                cryTestColor  = when (result.severity) {
                                                    "CRITICAL" -> AlertCritical
                                                    "HIGH"     -> AlertHigh
                                                    else       -> AlertLow
                                                }
                                                cryTestRunning = false
                                            }
                                        } catch (e: Exception) {
                                            withContext(Dispatchers.Main) {
                                                cryTestResult  = "Error: ${e.message}"
                                                cryTestRunning = false
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                enabled = !cryTestRunning,
                                contentPadding = PaddingValues(4.dp)
                            ) {
                                Text(label, style = MaterialTheme.typography.labelSmall, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        cryPatterns.drop(2).forEach { (label, pitch, burst) ->
                            OutlinedButton(
                                onClick = {
                                    cryTestRunning = true
                                    cryTestResult  = "Analysing..."
                                    scope.launch(Dispatchers.IO) {
                                        try {
                                            val cry = CryClassifier(context)
                                            cry.initialize()
                                            val result = cry.classify(pitch, burst)
                                            withContext(Dispatchers.Main) {
                                                cryTestResult = "${result.label}\n${result.severity}\n${result.description}"
                                                cryTestColor  = when (result.severity) {
                                                    "CRITICAL" -> AlertCritical
                                                    "HIGH"     -> AlertHigh
                                                    else       -> AlertLow
                                                }
                                                cryTestRunning = false
                                            }
                                        } catch (e: Exception) {
                                            withContext(Dispatchers.Main) {
                                                cryTestResult  = "Error: ${e.message}"
                                                cryTestRunning = false
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                enabled = !cryTestRunning,
                                contentPadding = PaddingValues(4.dp)
                            ) {
                                Text(label, style = MaterialTheme.typography.labelSmall, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            }
                        }
                    }

                    // Result display
                    cryTestResult?.let { result ->
                        Spacer(Modifier.height(12.dp))
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = cryTestColor.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(androidx.compose.foundation.shape.CircleShape)
                                        .background(cryTestColor)
                                )
                                Text(
                                    result,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = cryTestColor,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }
            }

            // ── Newborn records quick links ───────────────────────────────────
            if (patients.isNotEmpty()) {
                Text(
                    "Newborn Records",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                patients.forEach { patient ->
                    OutlinedButton(
                        onClick = { navController.navigate(Screen.NewbornRecord.go(patient.patientId)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            "👶 ${patient.fullName} — View Record",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // ── Emergency action ──────────────────────────────────────────────
            if (uiState.isDistressDetected) {
                Button(
                    onClick = { navController.navigate(Screen.Alert.route) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.PriorityHigh, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("START RESUSCITATION PROTOCOL", fontWeight = FontWeight.Bold)
                }
            }

            // ── Alert test button ─────────────────────────────────────────────
            Button(
                onClick = { navController.navigate(Screen.Alert.route) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor   = MaterialTheme.colorScheme.onErrorContainer
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("🚨 Test Alert Screen", style = MaterialTheme.typography.labelLarge)
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Subcomponents
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun MonitoringStatusHeader(isDistress: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
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
            if (isDistress) "DISTRESS DETECTED" else "LIVE MONITORING ACTIVE",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = if (isDistress) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun MonitoringVisualizationRow(uiState: NewbornUIState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            modifier = Modifier.weight(1.5f),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
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
        Card(
            modifier = Modifier.weight(1f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.Favorite, contentDescription = null,
                    tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(24.dp))
                Text("PULSE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                Text("128", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
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
                Icon(Icons.Default.GraphicEq, contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Column {
                Text(
                    "AI CRY CLASSIFICATION",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(status, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                LinearProgressIndicator(
                    progress = { confidence / 100f },
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
                    color = if (risk > 0.3) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.secondary
                )
            }
            Spacer(Modifier.height(12.dp))
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
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(2.dp)
                        .background(Color.Black)
                        .align(Alignment.CenterStart)
                        .offset(x = (risk * 300).dp)
                )
            }
        }
    }
}

@Composable
fun AnimatedWaveform(data: List<Float>, color: Color) {
    Canvas(modifier = Modifier
        .fillMaxSize()
        .padding(vertical = 20.dp)
    ) {
        if (data.isNotEmpty()) {
            val stroke = 2.dp.toPx()
            val step   = size.width / (data.size - 1).coerceAtLeast(1)
            val path   = Path()
            path.moveTo(0f, size.height / 2 + (data[0] - 0.5f) * size.height)
            data.forEachIndexed { i, v ->
                path.lineTo(i * step, size.height / 2 + (v - 0.5f) * size.height)
            }
            drawPath(path, color, style = Stroke(width = stroke, cap = StrokeCap.Round))
        }
    }
}