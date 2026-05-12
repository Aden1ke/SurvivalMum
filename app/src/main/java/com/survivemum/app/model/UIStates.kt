package com.survivemum.app.model

import androidx.compose.runtime.Immutable
import com.survivemum.app.ui.components.ReasoningStep

/*@Immutable
data class VitalsState(
    val hr: Int = 0,
    val spo2: Int = 0,
    val rr: Int = 0,
    val temp: Double = 37.0,
    val bp: String = "120/80",
    val timestamp: Long = System.currentTimeMillis()
)*/

@Immutable
data class NewbornUIState(
    val breathingRate: Int = 0,
    val cryStatus: String = "Monitoring...",
    val jaundiceRisk: Double = 0.0,
    val isDistressDetected: Boolean = false,
    val confidence: Int = 0,
    val reasoningSteps: List<ReasoningStep> = emptyList(),
    val waveformData: List<Float> = emptyList()
)

@Immutable
data class MotherMonitorUIState(
    val hr: Int = 0,
    val rr: Int = 0,
    val spo2: Int = 0,
    val bp: String = "120/80",
    val riskLevel: com.survivemum.app.viewmodel.RiskLevel = com.survivemum.app.viewmodel.RiskLevel.NORMAL,
    val isScanning: Boolean = true,
    val reasoningSteps: List<ReasoningStep> = emptyList(),
    val trendData: List<Float> = emptyList()
)
