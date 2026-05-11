package com.survivemum.app.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Immutable
data class ReasoningStep(
    val id: Int,
    val message: String,
    val isCompleted: Boolean = true
)

@Composable
fun ThinkingTracePanel(
    steps: List<ReasoningStep>,
    confidence: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "AI REASONING TRACE",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
                
                // Confidence Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "$confidence% CONFIDENCE",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                steps.forEachIndexed { index, step ->
                    ReasoningStepItem(
                        step = step,
                        isLast = index == steps.size - 1
                    )
                }
            }
        }
    }
}

@Composable
fun ReasoningStepItem(
    step: ReasoningStep,
    isLast: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = if (step.isCompleted) Icons.Default.CheckCircle else Icons.Default.Info,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (step.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
            )
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(20.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
            }
        }
        
        Text(
            text = step.message,
            style = MaterialTheme.typography.bodySmall.copy(
                fontStyle = if (!step.isCompleted) FontStyle.Italic else FontStyle.Normal,
                lineHeight = 16.sp
            ),
            color = if (step.isCompleted) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
