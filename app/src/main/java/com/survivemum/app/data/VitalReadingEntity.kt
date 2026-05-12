package com.survivemum.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents realtime health indicators captured by the AI's "eyes and ears."
 * This entity is the foundation for detecting neonatal jaundice, 
 * respiratory distress, and maternal pallor 100% offline.
 */
@Entity(tableName = "vital_readings")
data class VitalReadingEntity(
    @PrimaryKey 
    val readingId: String,          // Unique ID for each AI scan session

    val patientId: String,          // Links the reading to a mother or infant

    val timestamp: String,          // The exact moment of the AI observation

    /**
     * The target of the observation: MOTHER, NEWBORN, or TODDLER.
     */
    val layer: String,

    //  Physiological Estimates 
    val heartRateBpm: Float? = null, // Estimated beats per minute via camera (rPPG)
    val heartRateStatus: String = "UNAVAILABLE", // Quality flag (e.g., STABLE, NOISY)

    val respiratoryRate: Float? = null, // Estimated breaths per minute via chest movement
    val respiratoryStatus: String = "UNAVAILABLE",

    val spo2Estimate: Float? = null, // Experimental oxygen saturation estimation

    //  Visual Indicators (Computer Vision) 
    val facialOedema: String = "UNAVAILABLE", // Swelling (preeclampsia flag)
    
    val facialPallor: String = "UNAVAILABLE", // Paleness (anemia indicator)

    val jaundiceIndicator: String = "UNAVAILABLE", // Skin yellowing (neonatal jaundice flag)

    //  AI Metadata 
    /**
     * How certain the Gemma 4 model is about these readings.
     * A reading with 0.9 confidence is treated as a higher priority than 0.4.
     */
    val confidence: Float,

    /**
     * 1 if the camera has been calibrated for the patient's specific skin tone.
     * Essential for ethical, unbiased AI in maternal health.
     */
    val skinCalibrated: Int = 1,

    /**
     * 1 if this specific reading was serious enough to create a record 
     * in the 'alerts' table.
     */
    val triggeredAlert: Int = 0,

    val fullJson: String            // Complete model output for future research
)
