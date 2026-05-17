package com.survivemum.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a single clinical encounter or health check-up.
 * This entity stores vital signs and measurements used by the AI 
 * to detect trends (like rising BP) that could signal danger.
 */
@Entity(tableName = "visits")
data class VisitEntity(
    @PrimaryKey 
    val visitId: String,            // Unique identifier for the visit

    val patientId: String,          // Links this visit to a specific mother

    val visitDate: String,          // The date the check-up occurred

    val weeksAtVisit: Int = 0,      // Gestational age in weeks (critical for tracking growth)

    // --- Vital Signs & Measurements ---
    val bpSystolic: Int? = null,    // Top number of blood pressure
    val bpDiastolic: Int? = null,   // Bottom number of blood pressure

    val weightKg: Float? = null,    // Mother's weight (used to monitor nutrition/edema)

    val fundalHeightCm: Float? = null, // Growth measurement of the uterus

    val foetusHeartRate: Int? = null,  // Fetal heart rate in beats per minute

    val notes: String = "",         // Additional observations by the TBA or Mother

    /**
     * Identifies how the data was captured:
     * MANUAL_ENTRY: Typed in by user
     * AI_OCR: Scanned from a paper card using the camera
     */
    val dataSource: String = "MANUAL_ENTRY",

    val ocrConfidence: Float? = null, // Reliability score if OCR was used to scan a card

    val createdAt: String           // System timestamp for the database record
)
