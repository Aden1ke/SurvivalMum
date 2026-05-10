package com.survivemum.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PatientRecord(
    val id: String = "SM-29485",
    val name: String = "Jane Doe",
    val age: Int = 26,
    val pregnancyWeek: Int = 38,
    val bloodType: String = "O+",
    val allergies: String = "Penicillin",
    val recentVitals: String = "BP 120/80, HR 72",
    val lastScanDate: String = "2026-05-09",
    val history: List<TimelineEvent> = emptyList()
)

data class TimelineEvent(
    val date: String,
    val type: String,
    val description: String
)

class QRSystemViewModel : ViewModel() {

    private val _patientRecord = MutableStateFlow(PatientRecord())
    val patientRecord: StateFlow<PatientRecord> = _patientRecord.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _scanResult = MutableStateFlow<PatientRecord?>(null)
    val scanResult: StateFlow<PatientRecord?> = _scanResult.asStateFlow()

    init {
        // Load default mock data
        _patientRecord.value = PatientRecord(
            history = listOf(
                TimelineEvent("APR 02", "ANC SCAN", "Fetal movement normal"),
                TimelineEvent("APR 15", "VITAL ALERT", "Elevated BP detected"),
                TimelineEvent("MAY 01", "REFERRAL", "Referred to District Hospital")
            )
        )
    }

    fun startScanning() {
        _isScanning.value = true
    }

    fun stopScanning() {
        _isScanning.value = false
    }

    fun handleScanSuccess(data: String) {
        viewModelScope.launch {
            _isScanning.value = false
            delay(1000) // Simulate processing
            // In a real app, parse QR data here
            _scanResult.value = _patientRecord.value
        }
    }
}
