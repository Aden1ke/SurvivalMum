package com.survivemum.app.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.survivemum.app.data.SurviveMumDatabase
import com.survivemum.app.ml.GemmaManager
import com.survivemum.app.ml.VitalAnalyzer
import com.survivemum.app.ui.components.ReasoningStep
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch


enum class RiskLevel { NORMAL, WARNING, CRITICAL }


data class MotherMonitorUIState(
    val hr: Int = 0,
    val rr: Int = 0,
    val spo2: Int = 0,
    val bp: String = "--/--",
    val riskLevel: RiskLevel = RiskLevel.NORMAL,
    val isScanning: Boolean = true,
    val reasoningSteps: List<ReasoningStep> = emptyList(),
    val trendData: List<Float> = emptyList()
)

class MotherMonitorViewModel(application: Application) : AndroidViewModel(application) {

    private val db          = SurviveMumDatabase.getDatabase(application)
    private val gemmaManager = GemmaManager(application)

    private val _uiState = MutableStateFlow(MotherMonitorUIState())
    val uiState: StateFlow<MotherMonitorUIState> = _uiState.asStateFlow()

    private val _isEngineReady = MutableStateFlow(false)
    val isEngineReady: StateFlow<Boolean> = _isEngineReady.asStateFlow()

    // Accumulate HR readings to build a trend graph
    private val hrHistory = mutableListOf<Float>()

    // The real rPPG analyzer — pass its analyze() to CameraX ImageAnalysis
    val analyzer: VitalAnalyzer = VitalAnalyzer(
        context = application,
        onVitalsDetected = { heartRate, spo2 ->
            onVitalsReceived(heartRate.toInt(), spo2.toInt())
        }
    )

    init {
        // Load Gemma model in background
        viewModelScope.launch(Dispatchers.IO) {
            gemmaManager.initializeModel("gemma4.bin")
            _isEngineReady.value = true
            Log.d("MotherMonitorVM", "Gemma engine ready")
        }
    }

    // ── Called by VitalAnalyzer on each completed 5-second rPPG window ───────

    private fun onVitalsReceived(hr: Int, spo2: Int) {
        val risk = assessRisk(hr, spo2, _uiState.value.bp)

        // Update trend history (keep last 15 readings for the chart)
        hrHistory.add(hr.toFloat())
        if (hrHistory.size > 15) hrHistory.removeAt(0)

        _uiState.value = _uiState.value.copy(
            hr        = hr,
            spo2      = spo2,
            riskLevel = risk,
            trendData = hrHistory.toList(),
            isScanning = true
        )

        Log.d("MotherMonitorVM", "Vitals received — HR: $hr, SpO2: $spo2, Risk: $risk")

        // Ask Gemma to reason about these vitals whenever risk escalates
        if (risk != RiskLevel.NORMAL) {
            runGemmaAssessment(hr, spo2)
        }
    }

    // ── Clinical threshold assessment ─────────────────────────────────────────

    private fun assessRisk(hr: Int, spo2: Int, bp: String): RiskLevel {
        val systolic = bp.split("/").firstOrNull()?.toIntOrNull() ?: 0

        return when {
            // Critical thresholds (PPH / pre-eclampsia indicators)
            hr > 120 || hr < 40       -> RiskLevel.CRITICAL
            spo2 < 90                 -> RiskLevel.CRITICAL
            systolic >= 160           -> RiskLevel.CRITICAL

            // Warning thresholds
            hr > 100 || hr < 55       -> RiskLevel.WARNING
            spo2 < 94                 -> RiskLevel.WARNING
            systolic in 140..159      -> RiskLevel.WARNING

            else                      -> RiskLevel.NORMAL
        }
    }

    // ── Gemma AI reasoning — runs on IO thread, never blocks UI ──────────────

    private fun runGemmaAssessment(hr: Int, spo2: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Build patient context from DB for better Gemma reasoning
                val user    = db.userDao().getCurrentUser()
                val patient = user?.let {
                    db.patientDao().getAllPatients(it.userId).first().firstOrNull()
                }
                val visits  = patient?.let {
                    db.visitDao().getVisitsForPatient(it.patientId).first()
                } ?: emptyList()

                val history = if (visits.isNotEmpty()) {
                    visits.takeLast(3).joinToString("\n") { v ->
                        "Visit ${v.visitDate}: BP ${v.bpSystolic}/${v.bpDiastolic}, Week ${v.weeksAtVisit}"
                    }
                } else {
                    "No prior visit history available."
                }

                val patientInfo = patient?.let {
                    "Patient: ${it.fullName}, ${it.weeksPregnant} weeks pregnant, " +
                            "Risk: ${it.riskLevel}, Community: ${it.community}"
                } ?: "Patient details not available."

                val response = gemmaManager.assess(
                    pregnancyHistory = "$patientInfo\n\nRecent visits:\n$history",
                    currentQuery = """
                        Current vitals from camera rPPG:
                        Heart Rate: $hr bpm
                        SpO2: $spo2%
                        Blood Pressure: ${_uiState.value.bp}
                        
                        Risk classification: ${_uiState.value.riskLevel}
                        
                        Provide:
                        1. What these vitals indicate
                        2. Immediate danger signs to watch
                        3. Recommended action for the TBA
                    """.trimIndent()
                )

                // Parse Gemma response into reasoning steps for the UI
                val steps = response
                    .split("\n")
                    .filter { it.isNotBlank() }
                    .take(4)
                    .mapIndexed { index, line ->
                        ReasoningStep(index + 1, line.trim().removePrefix("${index + 1}. "))
                    }

                _uiState.value = _uiState.value.copy(
                    reasoningSteps = steps.ifEmpty {
                        listOf(ReasoningStep(1, response.take(120)))
                    }
                )

            } catch (e: Exception) {
                Log.e("MotherMonitorVM", "Gemma assessment failed", e)
                _uiState.value = _uiState.value.copy(
                    reasoningSteps = listOf(
                        ReasoningStep(1, "AI assessment unavailable: ${e.message?.take(60)}")
                    )
                )
            }
        }
    }

    // ── Called from UI when BP is entered manually ────────────────────────────
    // (BP cannot be measured by camera — requires a cuff)
    fun updateBP(bp: String) {
        _uiState.value = _uiState.value.copy(bp = bp)
        // Re-assess risk with updated BP
        val risk = assessRisk(_uiState.value.hr, _uiState.value.spo2, bp)
        _uiState.value = _uiState.value.copy(riskLevel = risk)
    }

    fun updateRR(rr: Int) {
        _uiState.value = _uiState.value.copy(rr = rr)
    }
}