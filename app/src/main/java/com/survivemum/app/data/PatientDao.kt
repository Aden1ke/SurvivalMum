package com.survivemum.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for the patients table.
 * This interface defines the interactions between the UI and the database.
 */
@Dao
interface PatientDao {

    /**
     * Retrieves all active patients assigned to a specific TBA.
     * Sorted by risk level (High to Low) and then by most recently updated.
     * Used for the Home Dashboard.
     */
    @Query("SELECT * FROM patients WHERE tbaId = :tbaId AND isActive = 1 ORDER BY riskLevel DESC, updatedAt DESC")
    fun getAllPatients(tbaId: String): Flow<List<PatientEntity>>

    /**
     * Fetches details for a single patient.
     * Returns null if the patient ID does not exist.
     * Used for the Patient Profile screen.
     */
    @Query("SELECT * FROM patients WHERE patientId = :patientId")
    suspend fun getPatient(patientId: String): PatientEntity?

    /**
     * Adds a new patient or updates an existing one if the ID matches.
     * Used during the patient registration/signup flow.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPatient(patient: PatientEntity)

    /**
     * Updates only the risk level and timestamp.
     * Triggered after a clinical screening or vital reading assessment.
     */
    @Query("UPDATE patients SET riskLevel = :riskLevel, updatedAt = :updatedAt WHERE patientId = :patientId")
    suspend fun updateRiskLevel(patientId: String, riskLevel: String, updatedAt: String)

    /**
     * Returns the total count of active patients for a TBA.
     * Used for dashboard statistics and performance tracking.
     */
    @Query("SELECT COUNT(*) FROM patients WHERE tbaId = :tbaId AND isActive = 1")
    suspend fun getPatientCount(tbaId: String): Int

    /**
     * Searches for patients by name within a specific TBA's list.
     * Uses SQL LIKE for partial matching (e.g., searching "Sar" finds "Sarah").
     */
    @Query("SELECT * FROM patients WHERE tbaId = :tbaId AND fullName LIKE '%' || :query || '%'")
    fun searchPatients(tbaId: String, query: String): Flow<List<PatientEntity>>
}