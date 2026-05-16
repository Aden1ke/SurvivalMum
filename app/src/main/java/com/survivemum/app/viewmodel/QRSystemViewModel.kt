package com.survivemum.app.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.survivemum.app.data.SurviveMumDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONObject

// ─────────────────────────────────────────────────────────────────────────────
// QRSystemViewModel
//
// Previously: hardcoded PatientRecord("Jane Doe", "SM-29485"...)
// Now:
//   1. loadPatient(patientId) fetches real data from Room
//   2. QR code encodes a JSON payload of the patient's key health data
//   3. Scanning decodes that JSON and reconstructs a PatientRecord
//   4. Timeline built from real VisitEntity records
// ─────────────────────────────────────────────────────────────────────────────

data class PatientRecord(
    val id            : String = "",
    val name          : String = "",
    val age           : Int    = 0,
    val pregnancyWeek : Int    = 0,
    val bloodType     : String = "Unknown",
    val allergies     : String = "None recorded",
    val recentVitals  : String = "",
    val lastScanDate  : String = "",
    val history       : List<TimelineEvent> = emptyList(),
    // The raw JSON string that gets encoded into the QR code
    val qrPayload     : String = ""
)

data class TimelineEvent(
    val date        : String,
    val type        : String,
    val description : String
)

class QRSystemViewModel(application: Application) : AndroidViewModel(application) {

    private val db = SurviveMumDatabase.getDatabase(application)

    private val _patientRecord = MutableStateFlow(PatientRecord())
    val patientRecord: StateFlow<PatientRecord> = _patientRecord.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _scanResult = MutableStateFlow<PatientRecord?>(null)
    val scanResult: StateFlow<PatientRecord?> = _scanResult.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // ── Load real patient from Room ───────────────────────────────────────────
    // Call this from QRCodeScreen's LaunchedEffect with the patientId from NavGraph

    fun loadPatient(patientId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                val patient = db.patientDao().getPatient(patientId)
                if (patient == null) {
                    _errorMessage.value = "Patient not found"
                    _isLoading.value = false
                    return@launch
                }

                // Fetch visit history for the timeline
                val visits = db.visitDao()
                    .getVisitsForPatient(patientId)
                    .first()
                    .sortedByDescending { it.visitDate }

                // Fetch alerts for the timeline
                val alerts = db.alertDao()
                    .getAlertsForPatient(patientId)
                    .first()
                    .sortedByDescending { it.timestamp }

                // Build timeline from visits + alerts interleaved
                val visitEvents = visits.map { visit ->
                    TimelineEvent(
                        date = visit.visitDate,
                        type = "ANC VISIT",
                        description = "Week ${visit.weeksAtVisit} — " +
                                "BP: ${visit.bpSystolic}/${visit.bpDiastolic} mmHg" +
                                (visit.weightKg?.let { ", Weight: ${it}kg" } ?: "") +
                                (if (visit.notes.isNotBlank()) "\n${visit.notes}" else "")
                    )
                }

                val alertEvents = alerts.take(5).map { alert ->
                    TimelineEvent(
                        date = alert.timestamp.take(10),
                        type = "ALERT — ${alert.severity}",
                        description = alert.message
                    )
                }

                val timeline = (visitEvents + alertEvents)
                    .sortedByDescending { it.date }
                    .take(10) // cap at 10 events for QR screen

                // Latest vitals summary
                val latestVisit = visits.firstOrNull()
                val latestVitals = latestVisit?.let {
                    "BP ${it.bpSystolic}/${it.bpDiastolic}" +
                            (it.weightKg?.let { w -> ", Weight ${w}kg" } ?: "")
                } ?: "No vitals recorded"

                // Build the QR JSON payload — compact but complete enough
                // for another TBA to scan and immediately see key health data
                val qrJson = buildQRPayload(patient, visits, alerts)

                val record = PatientRecord(
                    id            = patient.patientId.take(8).uppercase(),
                    name          = patient.fullName,
                    age           = 0, // DOB not in PatientEntity yet — add dob field to add this
                    pregnancyWeek = patient.weeksPregnant,
                    bloodType     = patient.bloodType ?: "Unknown",
                    allergies     = "None recorded", // add allergies field to PatientEntity
                    recentVitals  = latestVitals,
                    lastScanDate  = latestVisit?.visitDate ?: "Never",
                    history       = timeline,
                    qrPayload     = qrJson
                )

                _patientRecord.value = record
                Log.d("QRSystemVM", "Loaded patient: ${patient.fullName}, ${visits.size} visits")

            } catch (e: Exception) {
                Log.e("QRSystemVM", "Failed to load patient", e)
                _errorMessage.value = "Failed to load patient data: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ── Load current user's own record (for Mother dashboard) ─────────────────

    fun loadCurrentUserRecord() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val user = db.userDao().getCurrentUser() ?: return@launch
                val patients = db.patientDao().getAllPatients(user.userId).first()
                val patient = patients.firstOrNull() ?: return@launch
                loadPatient(patient.patientId)
            } catch (e: Exception) {
                Log.e("QRSystemVM", "Failed to load current user record", e)
            }
        }
    }

    // ── Build compact JSON for the QR code ───────────────────────────────────

    private fun buildQRPayload(
        patient: com.survivemum.app.data.PatientEntity,
        visits: List<com.survivemum.app.data.VisitEntity>,
        alerts: List<com.survivemum.app.data.AlertEntity>
    ): String {
        return try {
            val latestVisit = visits.firstOrNull()
            JSONObject().apply {
                put("app",     "SurviveMum")
                put("version", "1.0")
                put("id",      patient.patientId.take(8).uppercase())
                put("name",    patient.fullName)
                put("weeks",   patient.weeksPregnant)
                put("risk",    patient.riskLevel)
                put("blood",   patient.bloodType ?: "Unknown")
                put("community", patient.community)
                put("lang",    patient.language)
                latestVisit?.let {
                    put("lastBP",   "${it.bpSystolic}/${it.bpDiastolic}")
                    put("lastVisit", it.visitDate)
                    put("lastWeek", it.weeksAtVisit)
                }
                put("alertCount", alerts.size)
                put("highAlerts", alerts.count { it.severity in listOf("HIGH", "CRITICAL") })
                put("generated", System.currentTimeMillis())
            }.toString()
        } catch (e: Exception) {
            "{\"error\":\"QR generation failed\"}"
        }
    }

    // ── QR scanner callbacks ──────────────────────────────────────────────────

    fun startScanning() {
        _isScanning.value = true
        _scanResult.value = null
    }

    fun stopScanning() {
        _isScanning.value = false
    }

    // Called when the QR scanner library returns a decoded string
    fun handleScanSuccess(qrData: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _isScanning.value = false
            try {
                val json = JSONObject(qrData)

                // Validate it's a SurviveMum QR code
                if (json.optString("app") != "SurviveMum") {
                    _errorMessage.value = "This QR code is not a SurviveMum patient record"
                    return@launch
                }

                val scannedRecord = PatientRecord(
                    id            = json.optString("id", "Unknown"),
                    name          = json.optString("name", "Unknown patient"),
                    pregnancyWeek = json.optInt("weeks", 0),
                    bloodType     = json.optString("blood", "Unknown"),
                    recentVitals  = "BP: ${json.optString("lastBP", "N/A")}",
                    lastScanDate  = json.optString("lastVisit", "Unknown"),
                    history       = emptyList() // history not encoded in QR (too large)
                )

                _scanResult.value = scannedRecord
                Log.d("QRSystemVM", "Scan success: ${scannedRecord.name}")

            } catch (e: Exception) {
                Log.e("QRSystemVM", "QR decode failed", e)
                _errorMessage.value = "Could not read QR code: ${e.message}"
            }
        }
    }

    fun handleScanFailure(error: String) {
        _isScanning.value = false
        _errorMessage.value = "Scan failed: $error"
        Log.e("QRSystemVM", "Scan failed: $error")
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun clearScanResult() {
        _scanResult.value = null
    }
}