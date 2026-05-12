package com.survivemum.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents an acoustic analysis of an infant's cry.
 * This entity stores the AI's classification of the cry to detect 
 * life-threatening conditions like Meningitis or Respiratory Distress.
 */
@Entity(tableName = "cry_events")
data class CryEventEntity(
    @PrimaryKey 
    val classificationId: String,  // Unique ID for this specific audio analysis

    val patientId: String,         // Links the cry to a specific infant

    val timestamp: String,         // The exact time the cry was recorded

    val layer: String,             // Typically "NEWBORN" or "TODDLER"

    /**
     * The AI's classification (e.g., "HUNGER", "PAIN", "NEUROLOGICAL").
     * Specific high-pitched patterns can trigger emergency flags.
     */
    val cryType: String,

    /**
     * 1 if the cry matches patterns of clinical danger (e.g., Meningitis high-pitched cry).
     */
    val clinicalFlag: Int = 0,

    val clinicalConcern: String? = null, // Description of the danger (e.g., "Potential Neurological Distress")

    val confidence: Float,         // The AI model's certainty in this classification

    val audioDurationSec: Float? = null, // Length of the analyzed audio clip

    /**
     * Gemma's reasoning trace explaining why this cry was flagged.
     * Essential for the "Silent Guardian" logic to be transparent.
     */
    val gemmaTrace: String? = null,

    /**
     * 1 if this cry resulted in an entry in the 'alerts' table.
     */
    val triggeredAlert: Int = 0,

    val fullJson: String           // Raw model output for future improvements
)
