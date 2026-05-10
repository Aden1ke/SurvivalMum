package com.survivemum.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.survivemum.app.ui.components.AIStatusIndicator
import com.survivemum.app.viewmodel.PatientRecord
import com.survivemum.app.viewmodel.QRSystemViewModel
import com.survivemum.app.viewmodel.TimelineEvent

@Composable
fun QRCodeScreen(
    navController: NavController,
    viewModel: QRSystemViewModel = viewModel()
) {
    val patientRecord by viewModel.patientRecord.collectAsState()
    var showScanner by remember { mutableStateOf(false) }

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
                            text = "HEALTH RECORDS",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                        )
                    }
                    AIStatusIndicator()
                }
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showScanner = true },
                icon = { Icon(Icons.Default.QrCodeScanner, contentDescription = null) },
                text = { Text("SCAN RECORD") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. Patient Identity Card
            item {
                PatientIdentityCard(patientRecord)
            }

            // 2. Portable QR Record
            item {
                QRGeneratorCard(patientRecord.id)
            }

            // 3. Health Timeline
            item {
                Text(
                    text = "PATIENT JOURNEY",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            items(patientRecord.history) { event ->
                TimelineItem(event)
            }
        }
    }

    // Scanner Overlay Simulation
    if (showScanner) {
        ScannerOverlay(onClose = { showScanner = false })
    }
}

@Composable
fun PatientIdentityCard(record: PatientRecord) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                Column {
                    Text(record.name, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Text("ID: ${record.id} • ${record.age} yrs • Week ${record.pregnancyWeek}", style = MaterialTheme.typography.labelSmall)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoBadge(Icons.Default.Bloodtype, "Type ${record.bloodType}")
                InfoBadge(Icons.Default.Warning, record.allergies, isAlert = true)
            }
        }
    }
}

@Composable
fun QRGeneratorCard(id: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "PORTABLE OFFLINE RECORD",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            // Mock QR Code Representation
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .background(Color.White)
                    .padding(8.dp)
            ) {
                Icon(
                    Icons.Default.QrCode,
                    contentDescription = "QR Code",
                    modifier = Modifier.fillMaxSize(),
                    tint = Color.Black
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Share this code with health workers for immediate history access.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(horizontal = 16.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
fun TimelineItem(event: TimelineEvent) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                event.date,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.secondary
            )
            Box(modifier = Modifier.width(1.dp).height(40.dp).background(MaterialTheme.colorScheme.outlineVariant))
        }
        
        Card(
            modifier = Modifier.weight(1f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(event.type, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black), color = MaterialTheme.colorScheme.primary)
                Text(event.description, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
fun InfoBadge(icon: ImageVector, text: String, isAlert: Boolean = false) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(if (isAlert) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(12.dp),
            tint = if (isAlert) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSecondaryContainer
        )
        Text(
            text,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = if (isAlert) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

@Composable
fun ScannerOverlay(onClose: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.9f))
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "SCAN PATIENT RECORD",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                color = Color.White
            )
            Spacer(modifier = Modifier.height(24.dp))
            
            // Scanner Frame
            Box(
                modifier = Modifier
                    .size(260.dp)
                    .border(2.dp, Color.Cyan, RoundedCornerShape(12.dp))
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            Button(onClick = onClose, colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)) {
                Text("CANCEL")
            }
        }
    }
}
