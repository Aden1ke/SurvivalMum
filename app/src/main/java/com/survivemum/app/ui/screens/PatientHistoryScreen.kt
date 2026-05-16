package com.survivemum.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.survivemum.app.data.SurviveMumDatabase
import com.survivemum.app.data.VisitEntity
import com.survivemum.app.navigation.Screen
import com.survivemum.app.ui.theme.*
import kotlinx.coroutines.flow.first

@Composable
fun PatientHistoryScreen(navController: NavController, patientId: String) {

    val context = LocalContext.current
    var patientName by remember { mutableStateOf("") }
    var visits      by remember { mutableStateOf<List<VisitEntity>>(emptyList()) }
    var alertCount  by remember { mutableStateOf(0) }
    var isLoading   by remember { mutableStateOf(true) }

    LaunchedEffect(patientId) {
        val db = SurviveMumDatabase.getDatabase(context)
        patientName = db.patientDao().getPatient(patientId)?.fullName ?: ""
        visits      = db.visitDao().getVisitsForPatient(patientId).first()
        alertCount  = db.alertDao().getAlertsForPatient(patientId).first().size
        isLoading   = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {

        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurviveMumDark)
                .padding(20.dp)
        ) {
            Column {
                TextButton(
                    onClick = { navController.popBackStack() },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("← Back", color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodyMedium)
                }
                Text("Patient History",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White)
                Text(patientName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MotherPrimary)
            }
        }

        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = SurviveMumRed)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                // Summary row
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(
                            modifier = Modifier.weight(1f),
                            number   = "${visits.size}",
                            label    = "ANC Visits",
                            color    = MotherPrimary
                        )
                        // Fix 6: tapping Alerts navigates to AlertHistoryScreen
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(2.dp),
                            onClick = {
                                navController.navigate(Screen.AlertHistory.go(patientId))
                            }
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("$alertCount",
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.Bold),
                                    color = SurviveMumRed)
                                Text("Alerts  →",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                // Section header
                item {
                    Text("ANC Visit Timeline",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(top = 8.dp))
                }

                if (visits.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No visits recorded yet",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                } else {
                    items(visits.sortedByDescending { it.visitDate }) { visit ->
                        VisitCard(visit = visit)
                    }
                }

                // View All Alerts button at bottom
                item {
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            navController.navigate(Screen.AlertHistory.go(patientId))
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SurviveMumRed)
                    ) {
                        Text("🔔 View All Alerts", color = Color.White,
                            style = MaterialTheme.typography.labelLarge)
                    }
                }

                item { Spacer(Modifier.height(40.dp)) }
            }
        }
    }
}

@Composable
private fun VisitCard(visit: VisitEntity) {
    val isBPHigh = (visit.bpSystolic ?: 0) >= 140

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // Date + week
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(visit.visitDate,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MotherPrimary)
                Text("Week ${visit.weeksAtVisit}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(8.dp))

            // Vitals row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                VisitStat(
                    label = "Blood Pressure",
                    value = "${visit.bpSystolic}/${visit.bpDiastolic} mmHg",
                    color = if (isBPHigh) SurviveMumRed else NewbornPrimary
                )
                visit.weightKg?.let {
                    VisitStat(label = "Weight", value = "$it kg", color = MotherPrimary)
                }
            }

            if (visit.notes.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(visit.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            // High BP warning
            if (isBPHigh) {
                Spacer(Modifier.height(8.dp))
                Text("⚠️ Blood pressure above safe threshold",
                    style = MaterialTheme.typography.labelSmall,
                    color = SurviveMumRed,
                    fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun VisitStat(label: String, value: String, color: Color) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold, color = color)
    }
}