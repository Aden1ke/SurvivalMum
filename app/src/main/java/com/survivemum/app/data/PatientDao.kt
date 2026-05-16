package com.survivemum.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PatientDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPatient(patient: PatientEntity)

    // FIX: Changed 'userId' to 'tbaId' to match PatientEntity
    @Query("SELECT * FROM patients WHERE tbaId = :tbaId AND isActive = 1 ORDER BY updatedAt DESC")
    fun getAllPatients(tbaId: String): Flow<List<PatientEntity>>

    @Query("SELECT * FROM patients WHERE patientId = :patientId LIMIT 1")
    suspend fun getPatient(patientId: String): PatientEntity?

    @Query("UPDATE patients SET riskLevel = :riskLevel, updatedAt = :updatedAt WHERE patientId = :patientId")
    suspend fun updateRiskLevel(patientId: String, riskLevel: String, updatedAt: String)

    @Query("DELETE FROM patients WHERE patientId = :patientId")
    suspend fun deletePatient(patientId: String)
}