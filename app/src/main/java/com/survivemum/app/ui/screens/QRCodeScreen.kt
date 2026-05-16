package com.survivemum.app.ui.screens

import android.graphics.Bitmap
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.survivemum.app.ui.theme.*
import com.survivemum.app.viewmodel.PatientRecord
import com.survivemum.app.viewmodel.QRSystemViewModel
import com.survivemum.app.viewmodel.TimelineEvent
import androidx.compose.foundation.layout.statusBarsPadding

// ─────────────────────────────────────────────────────────────────────────────
// QRCodeScreen
// Two modes:
//   GENERATE — shows the patient's QR code for other facilities to scan
//   SCAN     — activates camera to scan an incoming patient's QR code
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun QRCodeScreen(
    navController : NavController,
    patientId     : String? = null,
    viewModel     : QRSystemViewModel = viewModel()
) {
    val patientRecord by viewModel.patientRecord.collectAsState()
    val isScanning    by viewModel.isScanning.collectAsState()
    val isLoading     by viewModel.isLoading.collectAsState()
    val scanResult    by viewModel.scanResult.collectAsState()
    val errorMessage  by viewModel.errorMessage.collectAsState()

    var activeTab by remember { mutableStateOf(if (patientId != null) QRTab.GENERATE else QRTab.GENERATE) }

    // Load patient data on entry
    LaunchedEffect(patientId) {
        if (patientId != null) {
            viewModel.loadPatient(patientId)
        } else {
            viewModel.loadCurrentUserRecord()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ── Top Bar ───────────────────────────────────────────────────────────
        QRTopBar(
            onBack    = { navController.popBackStack() },
            activeTab = activeTab,
            onTabChange = { activeTab = it }
        )

        // ── Error banner ──────────────────────────────────────────────────────
        AnimatedVisibility(visible = errorMessage != null) {
            errorMessage?.let { msg ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            msg,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { viewModel.clearError() }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // ── Content ───────────────────────────────────────────────────────────
        when {
            isLoading -> LoadingBox()

            activeTab == QRTab.GENERATE -> {
                GenerateTab(
                    patientRecord = patientRecord,
                    isLoading     = isLoading
                )
            }

            activeTab == QRTab.SCAN -> {
                ScanTab(
                    isScanning  = isScanning,
                    scanResult  = scanResult,
                    onStartScan = { viewModel.startScanning() },
                    onStopScan  = { viewModel.stopScanning() },
                    onSimulateScan = {
                        // Demo simulation — replace with real ZXing scanner integration
                        viewModel.handleScanSuccess(
                            """{"app":"SurviveMum","version":"1.0","id":"SM-29485",
                            "name":"Amina Yusuf","weeks":32,"risk":"HIGH",
                            "blood":"O+","community":"Kano North LGA",
                            "lastBP":"148/96","lastVisit":"2026-05-10",
                            "lastWeek":32,"alertCount":2,"highAlerts":1}"""
                        )
                    },
                    onClearResult = { viewModel.clearScanResult() }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Top bar with tab switcher
// ─────────────────────────────────────────────────────────────────────────────

enum class QRTab { GENERATE, SCAN }

@Composable
private fun QRTopBar(
    onBack      : () -> Unit,
    activeTab   : QRTab,
    onTabChange : (QRTab) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurviveMumDark)
            .statusBarsPadding()
            .padding(bottom = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "HEALTH RECORD",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    ),
                    color = Color.White
                )
                Text(
                    "QR Code System",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
        }

        // Tab row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QRTabButton(
                label     = "Generate QR",
                icon      = Icons.Default.QrCode,
                selected  = activeTab == QRTab.GENERATE,
                modifier  = Modifier.weight(1f),
                onClick   = { onTabChange(QRTab.GENERATE) }
            )
            QRTabButton(
                label     = "Scan QR",
                icon      = Icons.Default.QrCodeScanner,
                selected  = activeTab == QRTab.SCAN,
                modifier  = Modifier.weight(1f),
                onClick   = { onTabChange(QRTab.SCAN) }
            )
        }
    }
}

@Composable
private fun QRTabButton(
    label    : String,
    icon     : androidx.compose.ui.graphics.vector.ImageVector,
    selected : Boolean,
    modifier : Modifier = Modifier,
    onClick  : () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) SurviveMumRed else Color.White.copy(alpha = 0.1f),
            contentColor   = Color.White
        ),
        contentPadding = PaddingValues(horizontal = 12.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// GENERATE TAB — show QR code + patient summary
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun GenerateTab(
    patientRecord : com.survivemum.app.viewmodel.PatientRecord,
    isLoading     : Boolean
) {
    val qrBitmap = remember(patientRecord.qrPayload) {
        if (patientRecord.qrPayload.isNotBlank()) {
            generateQRBitmap(patientRecord.qrPayload, 600)
        } else null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        // QR Code card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // QR code or placeholder
                if (qrBitmap != null) {
                    Box(
                        modifier = Modifier
                            .size(220.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .padding(12.dp)
                    ) {
                        Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "Patient QR Code",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                } else {
                    // Placeholder when no data loaded yet
                    Box(
                        modifier = Modifier
                            .size(220.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.QrCode,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "No patient loaded",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    patientRecord.name.ifBlank { "—" },
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    if (patientRecord.id.isNotBlank()) "ID: ${patientRecord.id}" else "",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(12.dp))

                // Instructions
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MotherLight,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MotherPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            "Any facility can scan this code to instantly access the complete health record.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MotherAccent,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }

        // Patient summary card
        if (patientRecord.name.isNotBlank()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "RECORD SUMMARY",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = SurviveMumRed
                    )
                    Spacer(Modifier.height(14.dp))

                    RecordRow("Pregnancy Week",   "${patientRecord.pregnancyWeek} weeks")
                    RecordRow("Blood Type",        patientRecord.bloodType)
                    RecordRow("Recent Vitals",     patientRecord.recentVitals)
                    RecordRow("Last Visit",        patientRecord.lastScanDate)
                    RecordRow("Allergies",         patientRecord.allergies)
                }
            }
        }

        // Timeline card
        if (patientRecord.history.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "VISIT HISTORY",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = SurviveMumRed
                    )
                    Spacer(Modifier.height(14.dp))

                    patientRecord.history.forEachIndexed { index, event ->
                        TimelineRow(
                            event  = event,
                            isLast = index == patientRecord.history.lastIndex
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}


// ─────────────────────────────────────────────────────────────────────────────
// SCAN TAB — camera scanner + result display
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ScanTab(
    isScanning     : Boolean,
    scanResult     : com.survivemum.app.viewmodel.PatientRecord?,
    onStartScan    : () -> Unit,
    onStopScan     : () -> Unit,
    onSimulateScan : () -> Unit,
    onClearResult  : () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Scanner viewfinder area
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Scanner frame
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(SurviveMumDark),
                    contentAlignment = Alignment.Center
                ) {
                    // Corner brackets
                    ScannerBrackets(isActive = isScanning)

                    if (isScanning) {
                        // Scanning animation line
                        val infiniteTransition = rememberInfiniteTransition(label = "scan")
                        val yOffset by infiniteTransition.animateFloat(
                            initialValue = -100f,
                            targetValue  = 100f,
                            animationSpec = infiniteRepeatable(
                                animation  = tween(1800, easing = LinearEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "scanLine"
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.7f)
                                .height(2.dp)
                                .offset(y = yOffset.dp)
                                .background(SurviveMumRed.copy(alpha = 0.8f))
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Spacer(Modifier.height(160.dp))
                            Text(
                                "Scanning...",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.QrCodeScanner,
                                contentDescription = null,
                                modifier = Modifier.size(56.dp),
                                tint = Color.White.copy(alpha = 0.4f)
                            )
                            Text(
                                "Point camera at\na SurviveMum QR code",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.5f),
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Scan / Stop button
                if (!isScanning) {
                    Button(
                        onClick = onStartScan,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SurviveMumRed)
                    ) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Start Scanning", style = MaterialTheme.typography.labelLarge, color = Color.White)
                    }
                } else {
                    OutlinedButton(
                        onClick = onStopScan,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Text("Stop Scanning", style = MaterialTheme.typography.labelLarge)
                    }
                }

                // Demo simulation button — remove before production
                if (isScanning) {
                    Spacer(Modifier.height(8.dp))
                    TextButton(
                        onClick = onSimulateScan,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Simulate Successful Scan (Demo)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Scan result
        AnimatedVisibility(
            visible = scanResult != null,
            enter = fadeIn() + slideInVertically(),
            exit  = fadeOut() + slideOutVertically()
        ) {
            scanResult?.let { record ->
                ScannedPatientCard(
                    record    = record,
                    onDismiss = onClearResult
                )
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Scanned patient result card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ScannedPatientCard(
    record    : com.survivemum.app.viewmodel.PatientRecord,
    onDismiss : () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(4.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MotherPrimary.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MotherLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MotherPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        "PATIENT FOUND",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = MotherPrimary
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(16.dp))

            Text(
                record.name,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            if (record.id.isNotBlank()) {
                Text(
                    "ID: ${record.id}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(16.dp))

            RecordRow("Pregnancy Week", "${record.pregnancyWeek} weeks")
            RecordRow("Blood Type",     record.bloodType)
            RecordRow("Recent Vitals",  record.recentVitals)
            RecordRow("Last Visit",     record.lastScanDate)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Helper composables
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun RecordRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value.ifBlank { "—" },
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        thickness = 0.5.dp
    )
}

@Composable
private fun TimelineRow(event: TimelineEvent, isLast: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = if (isLast) 0.dp else 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Timeline dot and line
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(
                        if (event.type.contains("ALERT")) SurviveMumRed else MotherPrimary
                    )
            )
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(40.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    event.type,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = if (event.type.contains("ALERT")) SurviveMumRed else MotherPrimary
                )
                Text(
                    event.date,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                event.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun ScannerBrackets(isActive: Boolean) {
    val color = if (isActive) SurviveMumRed else Color.White.copy(alpha = 0.3f)
    val size  = 220.dp
    val arm   = 24.dp
    val stroke = 3.dp

    Box(
        modifier = Modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // Top-left
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .size(arm)
                .border(
                    width = stroke,
                    color = color,
                    shape = RoundedCornerShape(topStart = 8.dp)
                )
        )
        // Top-right
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(arm)
                .border(
                    width = stroke,
                    color = color,
                    shape = RoundedCornerShape(topEnd = 8.dp)
                )
        )
        // Bottom-left
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .size(arm)
                .border(
                    width = stroke,
                    color = color,
                    shape = RoundedCornerShape(bottomStart = 8.dp)
                )
        )
        // Bottom-right
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(arm)
                .border(
                    width = stroke,
                    color = color,
                    shape = RoundedCornerShape(bottomEnd = 8.dp)
                )
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// QR code bitmap generation using ZXing
// ─────────────────────────────────────────────────────────────────────────────

fun generateQRBitmap(content: String, size: Int): Bitmap? {
    return try {
        val hints = mapOf<EncodeHintType, Any>(EncodeHintType.MARGIN to 1)
        val bitMatrix = MultiFormatWriter().encode(
            content,
            BarcodeFormat.QR_CODE,
            size,
            size,
            hints
        )
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(
                    x, y,
                    if (bitMatrix[x, y]) Color.Black.toArgb() else Color.White.toArgb()
                )
            }
        }
        bitmap
    } catch (e: Exception) {
        null
    }
}