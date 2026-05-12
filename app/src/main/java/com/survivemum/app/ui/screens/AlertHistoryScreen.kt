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
import androidx.navigation.compose.rememberNavController
import com.survivemum.app.data.AlertEntity
import com.survivemum.app.data.SurviveMumDatabase
import com.survivemum.app.ui.theme.*
import kotlinx.coroutines.flow.first

@Composable
fun AlertHistoryScreen(navController: NavController, patientId: String) {

    val context = LocalContext.current
    var alerts by remember { mutableStateOf<List<AlertEntity>>(emptyList()) }
    var patientName by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var filterCritical by remember { mutableStateOf(false) }

    LaunchedEffect(patientId) {
        val db = SurviveMumDatabase.getDatabase(context)
        patientName = db.patientDao().getPatient(patientId)?.fullName ?: ""
        alerts = db.alertDao().getAlertsForPatient(patientId).first()
        isLoading = false
    }

    val displayedAlerts = if (filterCritical) {
        alerts.filter { it.severity in listOf("CRITICAL", "HIGH") }
    } else alerts

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {

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
                        "← Back",
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Text(
                    text = "Alert History",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White
                )
                Text(
                    text = patientName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = SurviveMumRed
                )
            }
        }

        // Filter row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${displayedAlerts.size} alerts",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            FilterChip(
                selected = filterCritical,
                onClick = { filterCritical = !filterCritical },
                label = {
                    Text(
                        "High Risk Only",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            )
        }

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = SurviveMumRed)
            }
        } else if (displayedAlerts.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (filterCritical) "No high-risk alerts"
                           else "No alerts recorded yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(displayedAlerts.sortedByDescending { it.timestamp }) { alert ->
                    AlertTimelineCard(alert = alert)
                }
                item { Spacer(modifier = Modifier.height(40.dp)) }
            }
        }
    }
}


@Composable
fun AlertTimelineCard(alert: AlertEntity) {
    val severityColor = when (alert.severity) {
        "CRITICAL" -> Color(0xFFD32F2F)
        "HIGH" -> Color(0xFFF57C00)
        "MEDIUM" -> Color(0xFFFBC02D)
        else -> Color(0xFF388E3C)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Severity indicator bar
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(severityColor, RoundedCornerShape(2.dp))
            )
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = alert.severity,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = severityColor
                    )
                    Text(
                        text = alert.timestamp,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = alert.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}