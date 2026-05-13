package com.survivemum.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface VisitDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVisit(visit: VisitEntity)

    // Used by PatientHistoryScreen — returns all visits as a one-shot list
    @Query("SELECT * FROM visits WHERE patientId = :patientId ORDER BY visitDate DESC")
    fun getVisitsForPatient(patientId: String): Flow<List<VisitEntity>>

    // Used by PatientProfileScreen to show the most recent visit in the summary
    @Query("SELECT * FROM visits WHERE patientId = :patientId ORDER BY visitDate DESC LIMIT 1")
    suspend fun getLatestVisit(patientId: String): VisitEntity?

    // Used by PatientProfileScreen stat card
    @Query("SELECT COUNT(*) FROM visits WHERE patientId = :patientId")
    suspend fun getVisitCount(patientId: String): Int

    @Query("DELETE FROM visits WHERE visitId = :visitId")
    suspend fun deleteVisit(visitId: String)
}