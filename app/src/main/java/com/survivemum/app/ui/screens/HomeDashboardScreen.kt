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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.survivemum.app.data.PatientEntity
import com.survivemum.app.data.SurviveMumDatabase
import com.survivemum.app.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun HomeDashboardScreen(navController: NavController, userType: String) {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var patients by remember { mutableStateOf<List<PatientEntity>>(emptyList()) }
    var currentUserName by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val db = SurviveMumDatabase.getDatabase(context)
        val user = db.userDao().getCurrentUser()
        currentUserName = user?.fullName ?: "TBA"
        if (user != null) {
            db.patientDao().getAllPatients(user.userId)
                .collect { list ->
                    patients = list
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

        // Top Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurviveMumDark)
                .padding(20.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "SurviveMum",
                            style = MaterialTheme.typography.titleLarge,
                            color = SurviveMumRed
                        )
                        Text(
                            text = "Welcome, $currentUserName",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                    IconButton(
                        onClick = { navController.navigate("settings") }
                    ) {
                        Text(text = "⚙", fontSize = 22.sp, color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Offline Status
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(NewbornPrimary)
                    )
                    Text(
                        text = "Offline — Gemma 4 running locally",
                        style = MaterialTheme.typography.labelSmall,
                        color = NewbornPrimary
                    )
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
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                // Stats row
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(
                            modifier = Modifier.weight(1f),
                            number = "${patients.size}",
                            label = "Active Patients",
                            color = MotherPrimary
                        )
                        StatCard(
                            modifier = Modifier.weight(1f),
                            number = "${patients.count { it.riskLevel == "HIGH" || it.riskLevel == "CRITICAL" }}",
                            label = "High Risk",
                            color = SurviveMumRed
                        )
                    }
                }

                // Add Patient Button — TBA only
                if (userType == "TBA") {
                    item {
                        Button(
                            onClick = {
                                navController.navigate("signup/PATIENT")
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SurviveMumRed
                            )
                        ) {
                            Text(
                                text = "+ Add New Patient",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White
                            )
                        }
                    }
                }

                // Section header
                item {
                    Text(
                        text = if (userType == "TBA") "Your Patients"
                               else "Your Pregnancy",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                if (patients.isEmpty()) {
                    item {
                        EmptyPatientsCard(userType = userType)
                    }
                } else {
                    items(patients) { patient ->
                        PatientCard(
                            patient = patient,
                            onClick = {
                                navController.navigate("patient/${patient.patientId}")
                            }
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    number: String,
    label: String,
    color: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = number,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun PatientCard(patient: PatientEntity, onClick: () -> Unit) {
    val riskColor = when (patient.riskLevel) {
        "CRITICAL" -> AlertCritical
        "HIGH" -> AlertHigh
        "MEDIUM" -> AlertMedium
        else -> AlertLow
    }
    val riskBg = when (patient.riskLevel) {
        "CRITICAL" -> AlertCriticalBg
        "HIGH" -> AlertHighBg
        "MEDIUM" -> AlertMediumBg
        else -> AlertLowBg
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = patient.fullName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (patient.weeksPregnant > 0)
                        "${patient.weeksPregnant} weeks pregnant"
                    else "Postpartum",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (patient.community.isNotBlank()) {
                    Text(
                        text = patient.community,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(riskBg)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = patient.riskLevel,
                    style = MaterialTheme.typography.labelMedium,
                    color = riskColor
                )
            }
        }
    }
}

@Composable
fun EmptyPatientsCard(userType: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "No patients yet",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (userType == "TBA")
                    "Tap Add New Patient to register\nyour first mother"
                else
                    "Your pregnancy profile\nwill appear here",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
