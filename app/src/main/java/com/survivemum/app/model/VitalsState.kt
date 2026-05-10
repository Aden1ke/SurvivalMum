package com.survivemum.app.model

/**
 * Data model representing maternal vitals.
 * Optimized for lightweight processing on low-end devices.
 */
data class VitalsState(
    val hr: Int = 0,           // Heart Rate
    val spo2: Int = 0,         // SpO2 percentage
    val rr: Int = 0,           // Respiratory Rate
    val temp: Double = 0.0,    // Temperature in Celsius
    val bp: String = "0/0",    // Blood Pressure
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "NORMAL" // NORMAL, HIGH, LOW
)
