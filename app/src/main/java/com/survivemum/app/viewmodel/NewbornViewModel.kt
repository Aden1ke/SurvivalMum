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

data class NewbornUIState(
    val breathingRate: Int = 0,
    val cryStatus: String = "Monitoring...",
    val jaundiceRisk: Double = 0.0,
    val isDistressDetected: Boolean = false,
    val confidence: Int = 0,
    val reasoningSteps: List<ReasoningStep> = emptyList(),
    val waveformData: List<Float> = emptyList()
)

class NewbornViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(NewbornUIState())
    val uiState: StateFlow<NewbornUIState> = _uiState.asStateFlow()

    private val _isEngineReady = MutableStateFlow(false)
    val isEngineReady: StateFlow<Boolean> = _isEngineReady.asStateFlow()

    init {
        viewModelScope.launch {
            delay(1500)
            _isEngineReady.value = true
            startMonitoring()
        }
    }

    private fun startMonitoring() {
        viewModelScope.launch {
            while (true) {
                _uiState.value = generateSimulatedState()
                delay(1000)
            }
        }
    }

    private fun generateSimulatedState(): NewbornUIState {
        val br = Random.nextInt(30, 60)
        val isDistress = br > 55 || br < 35
        val cryMessages = listOf("Hunger detected", "Fatigue detected", "Pain indicators present", "Normal discomfort")
        
        return NewbornUIState(
            breathingRate = br,
            cryStatus = if (Random.nextBoolean()) cryMessages.random() else "Analyzing sound...",
            jaundiceRisk = (Random.nextInt(5, 45) / 100.0),
            isDistressDetected = isDistress,
            confidence = Random.nextInt(85, 98),
            reasoningSteps = if (isDistress) {
                listOf(
                    ReasoningStep(1, "Detected abnormal breathing rhythm"),
                    ReasoningStep(2, "Audio analysis shows high-frequency stress cry"),
                    ReasoningStep(3, "Matched pattern: Respiratory Distress")
                )
            } else {
                listOf(
                    ReasoningStep(1, "Rhythmic breathing detected"),
                    ReasoningStep(2, "Cry pattern matched: Hunger (low urgency)")
                )
            },
            waveformData = List(20) { Random.nextFloat() }
        )
    }
}
