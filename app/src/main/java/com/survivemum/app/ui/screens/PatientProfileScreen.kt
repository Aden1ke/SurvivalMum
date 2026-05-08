package com.survivemum.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.survivemum.app.data.PatientEntity
import com.survivemum.app.data.SurviveMumDatabase
import com.survivemum.app.data.VisitEntity
import com.survivemum.app.ui.theme.*
import kotlinx.coroutines.flow.first

@Composable
fun PatientProfileScreen(navController: NavController, patientId: String) {

    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var patient by remember { mutableStateOf<PatientEntity?>(null) }
    var latestVisit by remember { mutableStateOf<VisitEntity?>(null) }
    var visitCount by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(patientId) {
        val db = SurviveMumDatabase.getDatabase(context)
        patient = db.patientDao().getPatient(patientId)
        latestVisit = db.visitDao().getLatestVisit(patientId)
        visitCount = db.visitDao().getVisitCount(patientId)
        isLoading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {

        // Back button + header
        val riskColor = when (patient?.riskLevel) {
            "CRITICAL" -> AlertCritical
            "HIGH" -> AlertHigh
            "MEDIUM" -> AlertMedium
            else -> AlertLow
        }

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
                    Text(
                        text = "← Back",
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = patient?.fullName ?: "Loading...",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if ((patient?.weeksPregnant ?: 0) > 0)
                            "${patient?.weeksPregnant} weeks pregnant"
                        else "Postpartum",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(riskColor)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = patient?.riskLevel ?: "LOW",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White
                        )
                    }
                }
            }
        }

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = SurviveMumRed)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                // Quick stats
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        number = "$visitCount",
                        label = "ANC Visits",
                        color = MotherPrimary
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        number = latestVisit?.let {
                            "${it.bpSystolic}/${it.bpDiastolic}"
                        } ?: "--",
                        label = "Last BP",
                        color = if ((latestVisit?.bpSystolic ?: 0) >= 140)
                            SurviveMumRed else NewbornPrimary
                    )
                }

                // Patient Details Card
                ProfileCard(title = "Patient Details") {
                    ProfileRow("Community", patient?.community ?: "Not set")
                    ProfileRow("Language", when (patient?.language) {
                        "yo" -> "Yoruba"
                        "ha" -> "Hausa"
                        "ig" -> "Igbo"
                        "pcm" -> "Nigerian Pidgin"
                        else -> "English"
                    })
                    ProfileRow("Blood Type", patient?.bloodType ?: "Unknown")
                    ProfileRow("HIV Status", patient?.hivStatus ?: "Unknown")
                    ProfileRow("Gravida", "${patient?.gravida ?: 0}")
                    ProfileRow("Para", "${patient?.para ?: 0}")
                }

                // Latest Visit Card
                if (latestVisit != null) {
                    ProfileCard(title = "Latest ANC Visit") {
                        ProfileRow("Date", latestVisit!!.visitDate)
                        ProfileRow("Week", "${latestVisit!!.weeksAtVisit}")
                        ProfileRow(
                            "Blood Pressure",
                            "${latestVisit!!.bpSystolic}/${latestVisit!!.bpDiastolic} mmHg"
                        )
                        latestVisit!!.weightKg?.let {
                            ProfileRow("Weight", "${it} kg")
                        }
                        if (latestVisit!!.notes.isNotBlank()) {
                            ProfileRow("Notes", latestVisit!!.notes)
                        }
                    }
                }

                // Action Buttons
                Text(
                    text = "Actions",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )

                ActionButton(
                    text = "View Full History",
                    color = MotherPrimary,
                    onClick = { navController.navigate("history/$patientId") }
                )

                ActionButton(
                    text = "View Alerts",
                    color = SurviveMumRed,
                    onClick = { navController.navigate("alerts/$patientId") }
                )

                ActionButton(
                    text = "Newborn Record",
                    color = NewbornPrimary,
                    onClick = { navController.navigate("newborn/$patientId") }
                )

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun ProfileCard(title: String, content: @Composable ColumnScope.() -> Unit) {
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
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
fun ProfileRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
    }
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
        thickness = 0.5.dp
    )
}

@Composable
fun ActionButton(text: String, color: Color, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = Color.White
        )
    }
}
