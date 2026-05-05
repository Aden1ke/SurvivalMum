package com.survivemum.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents the clinical profile of a newborn baby.
 * This entity is the anchor for neonatal monitoring, covering the 
 * high-risk first 28 days of life.
 */
@Entity(tableName = "newborn_records")
data class NewbornRecordEntity(
    @PrimaryKey 
    val newbornId: String,          // Unique identifier for the infant

    val patientId: String,          // Foreign key linking to the Mother (PatientEntity)

    val birthDatetime: String,      // Exact date and time of birth for age calculation

    val birthWeightKg: Float? = null, // Essential for detecting Low Birth Weight (LBW) risks

    /**
     * E.g., NATURAL, CESAREAN, ASSISTED.
     * Helps the AI contextually evaluate recovery and potential complications.
     */
    val deliveryType: String = "UNKNOWN",

    /**
     * Link to the 'cry_events' table for the very first cry recorded.
     * Used by Gemma to assess initial lung capacity and neurological vigor.
     */
    val firstCryEventId: String? = null,

    /**
     * A status check for the critical 28-day survival milestone.
     */
    val day28Status: String? = null,

    val createdAt: String           // Timestamp of when this record was created
)
