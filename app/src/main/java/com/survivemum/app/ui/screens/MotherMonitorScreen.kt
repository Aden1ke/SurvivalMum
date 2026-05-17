package com.survivemum.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.survivemum.app.data.PatientEntity
import com.survivemum.app.data.SurviveMumDatabase
import com.survivemum.app.navigation.Screen
import com.survivemum.app.ui.theme.*
import androidx.compose.foundation.layout.statusBarsPadding

@Composable
fun MotherMonitorScreen(navController: NavController) {

    val context = LocalContext.current
    var patients   by remember { mutableStateOf<List<PatientEntity>>(emptyList()) }
    var isLoading  by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val db   = SurviveMumDatabase.getDatabase(context)
        val user = db.userDao().getCurrentUser()
        if (user != null) {
            db.patientDao().getAllPatients(user.userId).collect { list ->
                patients  = list
                isLoading = false
            }
        } else {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurviveMumDark)
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
                            Icon(Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back", tint = Color.White)
                        }
                        Text("MOTHER MONITOR",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black, letterSpacing = 1.sp),
                            color = Color.White)
                    }
                }
            }
        }
    ) { innerPadding ->

        if (isLoading) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = SurviveMumRed)
            }
        } else if (patients.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("No patients registered yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(
                        onClick = { navController.navigate(Screen.Signup.go("mother")) },
                        colors = ButtonDefaults.buttonColors(containerColor = SurviveMumRed)
                    ) { Text("Add First Patient", color = Color.White) }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Select a patient to monitor",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground)

                patients.forEach { patient ->
                    MotherPatientRow(
                        patient    = patient,
                        onProfile  = { navController.navigate(Screen.PatientProfile.go(patient.patientId)) },
                        onQR       = { navController.navigate(Screen.QRCode.route) },
                        onAlert    = { navController.navigate(Screen.Alert.route) }
                    )
                }
            }
        }
    }
}

@Composable
private fun MotherPatientRow(
    patient   : PatientEntity,
    onProfile : () -> Unit,
    onQR      : () -> Unit,
    onAlert   : () -> Unit
) {
    val riskColor = riskColor(patient.riskLevel)
    val riskBg    = riskBgColor(patient.riskLevel)

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
                    Text(
                        if (patient.weeksPregnant > 0) "${patient.weeksPregnant} weeks pregnant"
                        else "Postpartum",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                RiskBadge(patient.riskLevel)
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(10.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PatientActionButton(Modifier.weight(1f), "👤 Profile", onProfile)
                PatientActionButton(Modifier.weight(1f), "📋 QR Record", onQR)
                // Alert button is red when the patient is high/critical risk
                OutlinedButton(
                    onClick = onAlert,
                    modifier = Modifier.weight(1f).height(40.dp),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (patient.riskLevel in listOf("HIGH", "CRITICAL"))
                            MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Text("🚨 Alert", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}