package com.survivemum.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.survivemum.app.data.PatientEntity
import com.survivemum.app.data.SurviveMumDatabase
import com.survivemum.app.navigation.Screen
import com.survivemum.app.ui.theme.*

@Composable
fun HomeDashboardScreen(navController: NavController, userType: String) {
    if (userType == "TBA") {
        TBADashboard(navController = navController)
    } else {
        MotherDashboard(navController = navController)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TBA Dashboard
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun TBADashboard(navController: NavController) {

    val context = LocalContext.current
    var patients        by remember { mutableStateOf<List<PatientEntity>>(emptyList()) }
    var currentUserName by remember { mutableStateOf("") }
    var isLoading       by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val db   = SurviveMumDatabase.getDatabase(context)
        val user = db.userDao().getCurrentUser()
        currentUserName = user?.fullName ?: "TBA"
        if (user != null) {
            db.patientDao().getAllPatients(user.userId).collect { list ->
                patients  = list
                isLoading = false
            }
        } else {
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        DashboardTopBar(
            name       = currentUserName,
            onSettings = { navController.navigate(Screen.Settings.route) }
        )

        if (isLoading) {
            LoadingBox()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(Modifier.weight(1f), "${patients.size}", "Active Patients", MotherPrimary)
                        StatCard(
                            Modifier.weight(1f),
                            "${patients.count { it.riskLevel == "HIGH" || it.riskLevel == "CRITICAL" }}",
                            "High Risk",
                            SurviveMumRed
                        )
                    }
                }

                item { SectionHeader("Monitoring Tools") }

                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        MonitoringToolCard(Modifier.weight(1f), "📷", "Camera Monitor", "rPPG vitals via AI", MotherPrimary) {
                            navController.navigate(Screen.CameraMonitor.route)
                        }
                        MonitoringToolCard(Modifier.weight(1f), "🤱", "Mother Monitor", "ANC card + vitals", SurviveMumRed) {
                            navController.navigate(Screen.MotherMonitor.route)
                        }
                    }
                }

                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        MonitoringToolCard(Modifier.weight(1f), "👶", "Newborn Monitor", "Cry + visual assess", NewbornPrimary) {
                            navController.navigate(Screen.NewbornMonitor.route)
                        }
                        MonitoringToolCard(Modifier.weight(1f), "🧒", "Toddler Monitor", "1–5 yr development", Color(0xFF7B5EA7)) {
                            navController.navigate(Screen.ToddlerMonitor.route)
                        }
                    }
                }

                item {
                    Button(
                        onClick = { navController.navigate(Screen.Signup.go("PATIENT")) },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SurviveMumRed)
                    ) {
                        Text("+ Add New Patient", style = MaterialTheme.typography.labelLarge, color = Color.White)
                    }
                }

                item { SectionHeader("Your Patients") }

                if (patients.isEmpty()) {
                    item { EmptyCard("Tap Add New Patient to register\nyour first mother") }
                } else {
                    items(patients) { patient ->
                        TBAPatientCard(
                            patient       = patient,
                            onViewProfile = { navController.navigate(Screen.PatientProfile.go(patient.patientId)) },
                            onViewHistory = { navController.navigate(Screen.PatientHistory.go(patient.patientId)) },
                            onViewAlerts  = { navController.navigate(Screen.AlertHistory.go(patient.patientId)) },
                            onViewQR      = {
                                // Pass patientId so QRCodeScreen loads this specific patient
                                navController.navigate("${Screen.QRCode.route}?patientId=${patient.patientId}")
                            }
                        )
                    }
                }

                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Mother Dashboard
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun MotherDashboard(navController: NavController) {

    val context = LocalContext.current
    var myProfile       by remember { mutableStateOf<PatientEntity?>(null) }
    var currentUserName by remember { mutableStateOf("") }
    var isLoading       by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val db   = SurviveMumDatabase.getDatabase(context)
        val user = db.userDao().getCurrentUser()
        currentUserName = user?.fullName ?: "Mother"
        if (user != null) {
            db.patientDao().getAllPatients(user.userId).collect { list ->
                myProfile = list.firstOrNull()
                isLoading = false
            }
        } else {
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        DashboardTopBar(
            name       = currentUserName,
            onSettings = { navController.navigate(Screen.Settings.route) }
        )

        if (isLoading) {
            LoadingBox()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatCard(
                            Modifier.weight(1f),
                            if ((myProfile?.weeksPregnant ?: 0) > 0) "${myProfile?.weeksPregnant}" else "—",
                            "Weeks Pregnant",
                            MotherPrimary
                        )
                        StatCard(
                            Modifier.weight(1f),
                            myProfile?.riskLevel ?: "—",
                            "Risk Level",
                            riskColor(myProfile?.riskLevel ?: "LOW")
                        )
                    }
                }

                item { SectionHeader("My Monitoring Tools") }

                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        MonitoringToolCard(Modifier.weight(1f), "📷", "Camera Monitor", "Check my vitals", MotherPrimary) {
                            navController.navigate(Screen.CameraMonitor.route)
                        }
                        MonitoringToolCard(Modifier.weight(1f), "👶", "Newborn Monitor", "Monitor my baby", NewbornPrimary) {
                            navController.navigate(Screen.NewbornMonitor.route)
                        }
                    }
                }

                item { SectionHeader("Your Pregnancy") }

                if (myProfile == null) {
                    item { EmptyCard("Your pregnancy profile\nwill appear here") }
                } else {
                    item {
                        MotherProfileCard(
                            patient             = myProfile!!,
                            onViewRecord        = { navController.navigate(Screen.PatientProfile.go(myProfile!!.patientId)) },
                            onViewNewbornRecord = { navController.navigate(Screen.NewbornRecord.go(myProfile!!.patientId)) },
                            onViewAlerts        = { navController.navigate(Screen.AlertHistory.go(myProfile!!.patientId)) },
                            // Mother's own QR — no patientId needed, ViewModel loads her own record
                            onViewQR            = { navController.navigate(Screen.QRCode.route) }
                        )
                    }
                }

                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Shared top bar
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun DashboardTopBar(name: String, onSettings: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurviveMumDark)
            .statusBarsPadding()
            .padding(20.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("SurviveMum", style = MaterialTheme.typography.titleLarge, color = SurviveMumRed)
                    Text("Welcome, $name", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.8f))
                }
                IconButton(onClick = onSettings) {
                    Text("⚙", fontSize = 22.sp, color = Color.White)
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(NewbornPrimary))
                Text("Offline — Gemma 4 running locally", style = MaterialTheme.typography.labelSmall, color = NewbornPrimary)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Patient cards
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun TBAPatientCard(
    patient       : PatientEntity,
    onViewProfile : () -> Unit,
    onViewHistory : () -> Unit,
    onViewAlerts  : () -> Unit,
    onViewQR      : () -> Unit          // ADDED — navigates to QRCodeScreen with patientId
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(patient.fullName, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        if (patient.weeksPregnant > 0) "${patient.weeksPregnant} weeks pregnant" else "Postpartum",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (patient.community.isNotBlank()) {
                        Text(patient.community, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                RiskBadge(patient.riskLevel)
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(10.dp))

            // Row 1 — core actions
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PatientActionButton(Modifier.weight(1f), "👤 Profile",  onViewProfile)
                PatientActionButton(Modifier.weight(1f), "📋 History",  onViewHistory)
                PatientActionButton(Modifier.weight(1f), "🔔 Alerts",   onViewAlerts)
            }

            Spacer(Modifier.height(8.dp))

            // Row 2 — QR portable record
            OutlinedButton(
                onClick = onViewQR,
                modifier = Modifier.fillMaxWidth().height(40.dp),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MotherPrimary),
                border = androidx.compose.foundation.BorderStroke(1.dp, MotherPrimary.copy(alpha = 0.4f))
            ) {
                Text("📋 QR Health Record", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun MotherProfileCard(
    patient             : PatientEntity,
    onViewRecord        : () -> Unit,
    onViewNewbornRecord : () -> Unit,
    onViewAlerts        : () -> Unit,
    onViewQR            : () -> Unit    // ADDED — navigates to QRCodeScreen (no patientId needed)
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(patient.fullName, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        if (patient.weeksPregnant > 0) "${patient.weeksPregnant} weeks pregnant" else "Postpartum",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (patient.community.isNotBlank()) {
                        Text(patient.community, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                RiskBadge(patient.riskLevel)
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(10.dp))

            // Row 1 — core actions
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PatientActionButton(Modifier.weight(1f), "📄 My Record",   onViewRecord)
                PatientActionButton(Modifier.weight(1f), "👶 Baby Record", onViewNewbornRecord)
                PatientActionButton(Modifier.weight(1f), "🔔 My Alerts",  onViewAlerts)
            }

            Spacer(Modifier.height(8.dp))

            // Row 2 — QR portable record
            OutlinedButton(
                onClick = onViewQR,
                modifier = Modifier.fillMaxWidth().height(40.dp),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MotherPrimary),
                border = androidx.compose.foundation.BorderStroke(1.dp, MotherPrimary.copy(alpha = 0.4f))
            ) {
                Text("📋 My QR Health Record", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Reusable small components
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun MonitoringToolCard(
    modifier : Modifier = Modifier,
    emoji    : String,
    title    : String,
    subtitle : String,
    color    : Color,
    onClick  : () -> Unit
) {
    Card(
        modifier = modifier.height(100.dp).clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(emoji, fontSize = 26.sp)
            Column {
                Text(title, style = MaterialTheme.typography.labelLarge, color = color, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
        }
    }
}

@Composable
fun StatCard(modifier: Modifier = Modifier, number: String, label: String, color: Color) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(number, fontSize = 32.sp, fontWeight = FontWeight.Bold, color = color)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground, modifier = modifier.padding(top = 4.dp))
}

@Composable
fun RiskBadge(level: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(riskBgColor(level))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(level, style = MaterialTheme.typography.labelMedium, color = riskColor(level))
    }
}

@Composable
fun PatientActionButton(modifier: Modifier = Modifier, label: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(40.dp),
        shape = RoundedCornerShape(10.dp),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
    }
}

@Composable
fun EmptyCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("No patients yet", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun LoadingBox() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = SurviveMumRed)
    }
}

fun riskColor(level: String): Color = when (level) {
    "CRITICAL" -> AlertCritical
    "HIGH"     -> AlertHigh
    "MEDIUM"   -> AlertMedium
    else       -> AlertLow
}

fun riskBgColor(level: String): Color = when (level) {
    "CRITICAL" -> AlertCriticalBg
    "HIGH"     -> AlertHighBg
    "MEDIUM"   -> AlertMediumBg
    else       -> AlertLowBg
}