package com.survivemum.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a patient (mother) in the SurviveMum system.
 * This entity serves as the primary data model for clinical screening and tracking.
 */
@Entity(tableName = "patients")
data class PatientEntity(
    @PrimaryKey
    val patientId: String,          // Unique identifier (UUID or Hospital ID)

    val tbaId: String,              // ID of the Traditional Birth Attendant managing this patient

    val fullName: String,           // Patient's legal name

    val age: Int = 0,               // Patient's age in years

    val phoneNumber: String? = null,// Optional contact number

    val community: String = "",     // Local area or village name

    val language: String = "en",    // Preferred language for communication (default is English)

    val bloodType: String? = null,  // Clinical blood group (A, B, AB, O and +/-)

    val hivStatus: String? = null,  // Sensitive health data: stored for high-risk screening logic

    val gravida: Int = 0,           // Total number of times the patient has been pregnant

    val para: Int = 0,              // Total number of viable births (past 20-24 weeks gestation)

    val weeksPregnant: Int = 0,     // Current gestational age for active pregnancy tracking

    val expectedDeliveryDate: String? = null, // Calculated EDD (ISO-8601 format)

    val knownConditions: String = "[]",       // JSON string of existing medical conditions (e.g., Asthma, Diabetes)

    val riskLevel: String = "LOW",  // Assessment outcome: LOW, MEDIUM, or HIGH (determined by screening logic)

    val createdAt: String,          // Timestamp when record was created

    val updatedAt: String,          // Timestamp of the last data modification

    val isActive: Int = 1           // 1 for Active, 0 for Archived/Soft-deleted
)