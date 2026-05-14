package com.survivemum.app.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.survivemum.app.data.SurviveMumDatabase
import com.survivemum.app.data.VitalReadingEntity
import com.survivemum.app.ml.VitalAnalyzer
import com.survivemum.app.model.VitalsState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.UUID

class VitalsViewModel(application: Application) : AndroidViewModel(application) {

    private val db = SurviveMumDatabase.getDatabase(application)

    private val _uiState = MutableStateFlow(VitalsState())
    val uiState: StateFlow<VitalsState> = _uiState.asStateFlow()

    private val _isEngineReady = MutableStateFlow(false)
    val isEngineReady: StateFlow<Boolean> = _isEngineReady.asStateFlow()

    private val _alertTriggered = MutableStateFlow(false)
    val alertTriggered: StateFlow<Boolean> = _alertTriggered.asStateFlow()

    val analyzer: VitalAnalyzer = VitalAnalyzer(
        context = application,
        onVitalsDetected = { heartRate, spo2 ->
            onRealVitalsReceived(heartRate, spo2)
        }
    )

    init {
        _isEngineReady.value = true
    }

    private fun onRealVitalsReceived(heartRate: Double, spo2: Double) {
        val hr    = heartRate.toInt().coerceIn(0, 250)
        val spo2i = spo2.toInt().coerceIn(0, 100)

        val status = when {
            hr > 100 || hr < 50 -> "HIGH"
            spo2i < 94          -> "HIGH"
            else                -> "NORMAL"
        }

        val hrStatus = when {
            hr in 50..100 -> "STABLE"
            hr in 40..120 -> "NOISY"
            else          -> "UNAVAILABLE"
        }

        _uiState.value = VitalsState(
            hr        = hr,
            spo2      = spo2i,
            rr        = _uiState.value.rr,
            temp      = _uiState.value.temp,
            bp        = _uiState.value.bp,
            timestamp = System.currentTimeMillis(),
            status    = status
        )
        _alertTriggered.value = status == "HIGH"

        Log.d("VitalsViewModel", "HR: $hr bpm  SpO2: $spo2i%  Status: $status")

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val user    = db.userDao().getCurrentUser() ?: return@launch
                val patient = db.patientDao()
                    .getAllPatients(user.userId)
                    .first()
                    .firstOrNull() ?: return@launch

                val isAlert = status == "HIGH"

                // fullJson is a required field in VitalReadingEntity
                val fullJson = JSONObject().apply {
                    put("hr",        hr)
                    put("spo2",      spo2i)
                    put("hrStatus",  hrStatus)
                    put("status",    status)
                    put("timestamp", System.currentTimeMillis())
                    put("source",    "rPPG_CHROM")
                }.toString()

                db.vitalReadingDao().insertReading(
                    VitalReadingEntity(
                        readingId       = UUID.randomUUID().toString(),
                        patientId       = patient.patientId,
                        timestamp       = System.currentTimeMillis().toString(),
                        layer           = "MOTHER",          // ← required, NOT source
                        heartRateBpm    = hr.toFloat(),       // ← Float, NOT heartRate: Int
                        heartRateStatus = hrStatus,
                        spo2Estimate    = spo2i.toFloat(),    // ← spo2Estimate, NOT spo2
                        confidence      = if (hrStatus == "STABLE") 0.85f else 0.55f, // ← required
                        triggeredAlert  = if (isAlert) 1 else 0,
                        fullJson        = fullJson            // ← required
                    )
                )
            } catch (e: Exception) {
                Log.e("VitalsViewModel", "Failed to persist vital reading", e)
            }
        }
    }

    fun toggleAlert() {
        _alertTriggered.value = !_alertTriggered.value
    }

    fun updateRR(rr: Int) {
        _uiState.value = _uiState.value.copy(rr = rr)
    }

    fun updateTemp(temp: Double) {
        _uiState.value = _uiState.value.copy(temp = temp)
    }

    fun updateBP(bp: String) {
        _uiState.value = _uiState.value.copy(bp = bp)
    }
}