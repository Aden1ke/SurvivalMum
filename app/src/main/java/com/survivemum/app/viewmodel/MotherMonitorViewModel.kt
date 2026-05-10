package com.survivemum.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.survivemum.app.ui.components.ReasoningStep
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

data class MotherMonitorUIState(
    val hr: Int = 0,
    val rr: Int = 0,
    val spo2: Int = 0,
    val bp: String = "120/80",
    val riskLevel: RiskLevel = RiskLevel.NORMAL,
    val isScanning: Boolean = true,
    val reasoningSteps: List<ReasoningStep> = emptyList(),
    val trendData: List<Float> = emptyList()
)

enum class RiskLevel {
    NORMAL, WARNING, CRITICAL
}

class MotherMonitorViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(MotherMonitorUIState())
    val uiState: StateFlow<MotherMonitorUIState> = _uiState.asStateFlow()

    init {
        startMonitoring()
        startRiskSimulation()
    }

    private fun startMonitoring() {
        viewModelScope.launch {
            while (true) {
                _uiState.value = _uiState.value.copy(
                    hr = generateHR(_uiState.value.riskLevel),
                    rr = generateRR(_uiState.value.riskLevel),
                    spo2 = generateSpO2(_uiState.value.riskLevel),
                    trendData = List(15) { Random.nextFloat() }
                )
                delay(1500)
            }
        }
    }

    private fun startRiskSimulation() {
        viewModelScope.launch {
            // Start Normal
            _uiState.value = _uiState.value.copy(
                riskLevel = RiskLevel.NORMAL,
                reasoningSteps = listOf(ReasoningStep(1, "Continuous maternal signal tracking active"))
            )
            
            delay(8000) // Transition to Warning
            _uiState.value = _uiState.value.copy(
                riskLevel = RiskLevel.WARNING,
                reasoningSteps = listOf(
                    ReasoningStep(1, "Maternal signal tracking active"),
                    ReasoningStep(2, "Detected slight elevation in heart rate")
                )
            )

            delay(8000) // Transition to Critical
            _uiState.value = _uiState.value.copy(
                riskLevel = RiskLevel.CRITICAL,
                bp = "145/95",
                reasoningSteps = listOf(
                    ReasoningStep(1, "Maternal signal tracking active"),
                    ReasoningStep(2, "Elevated heart rate confirmed"),
                    ReasoningStep(3, "Blood pressure trend escalating"),
                    ReasoningStep(4, "Matched PPH Early Warning indicators")
                )
            )
        }
    }

    private fun generateHR(risk: RiskLevel) = when (risk) {
        RiskLevel.NORMAL -> Random.nextInt(70, 85)
        RiskLevel.WARNING -> Random.nextInt(90, 105)
        RiskLevel.CRITICAL -> Random.nextInt(110, 130)
    }

    private fun generateRR(risk: RiskLevel) = when (risk) {
        RiskLevel.NORMAL -> Random.nextInt(12, 18)
        RiskLevel.WARNING -> Random.nextInt(20, 24)
        RiskLevel.CRITICAL -> Random.nextInt(26, 32)
    }

    private fun generateSpO2(risk: RiskLevel) = when (risk) {
        RiskLevel.NORMAL -> Random.nextInt(97, 100)
        RiskLevel.WARNING -> Random.nextInt(95, 97)
        RiskLevel.CRITICAL -> Random.nextInt(90, 94)
    }
}
