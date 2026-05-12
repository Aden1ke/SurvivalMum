package com.survivemum.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.survivemum.app.ui.components.AIStatusIndicator

@Composable
fun DashboardScreen(navController: NavController) {
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
                    Text(
                        text = "MISSION CONTROL",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                    )
                    AIStatusIndicator()
                }
                Text(
                    text = "System Operational • Gemma 4 Live",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Lifecycle Status Grid
            item {
                Text(
                    text = "MONITORING SUMMARY",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MonitoringSummaryCard(
                        label = "Mother",
                        status = "STABLE",
                        icon = Icons.Default.Face,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f).clickable {
                            navController.navigate(Screen.MotherMonitor.route)
                        }
                    )
                    MonitoringSummaryCard(
                        label = "Newborn",
                        status = "WARNING",
                        icon = Icons.Default.ChildCare,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f).clickable {
                            navController.navigate(Screen.NewbornMonitor.route)
                        }
                    )
                }
            }

            // Quick Actions
            item {
                Text(
                    text = "CRITICAL ACTIONS",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CriticalActionButton(
                        label = "Emergency Scan",
                        description = "Start immediate AI vital assessment",
                        icon = Icons.Default.Camera,
                        onClick = { }
                    )
                    CriticalActionButton(
                        label = "Generate Referral",
                        description = "Create hospital transfer document",
                        icon = Icons.Default.Description,
                        onClick = { }
                    )
                }
            }

            // Patient Timeline Preview
            item {
                Text(
                    text = "PORTABLE HEALTH RECORD",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            item {
                Surface(
                    onClick = { navController.navigate(Screen.QRCode.route) },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(Icons.Default.QrCode, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Column {
                            Text("Jane Doe (SM-29485)", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                            Text("Last updated: May 09, 2026", style = MaterialTheme.typography.labelSmall)
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(Icons.Default.ChevronRight, contentDescription = null)
                    }
                }
            }

            // AI Activity Feed
            item {
                Text(
                    text = "RECENT AI ACTIVITY",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            items(3) { index ->
                AIActivityCard(
                    title = if (index == 0) "Newborn Cry Analysis" else "Mother Vital Trend",
                    time = "${index * 5 + 2}m ago",
                    result = if (index == 0) "Potential Distress Detected" else "Vitals Stable"
                )
            }
        }
    }
}

@Composable
fun MonitoringSummaryCard(
    label: String,
    status: String,
    icon: ImageVector,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, contentDescription = null, tint = color)
            Spacer(modifier = Modifier.height(8.dp))
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text(
                status,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = color
            )
        }
    }
}

@Composable
fun CriticalActionButton(
    label: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column {
                Text(label, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                Text(description, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
fun AIActivityCard(title: String, time: String, result: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                Text(result, style = MaterialTheme.typography.bodySmall)
            }
            Text(time, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
        }
    }
}
