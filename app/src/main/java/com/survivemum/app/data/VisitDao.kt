package com.survivemum.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for clinical visit records.
 * Provides the data foundation for trend analysis and historical timelines.
 */
@Dao
interface VisitDao {

    /**
     * Retrieves every recorded visit for a specific patient, ordered by date.
     * Powers the 'Patient History Timeline' screen to show health trends over time.
     */
    @Query("SELECT * FROM visits WHERE patientId = :patientId ORDER BY visitDate ASC")
    fun getVisitsForPatient(patientId: String): Flow<List<VisitEntity>>

    /**
     * Fetches the most recent clinical data for a patient.
     * Used on the 'Patient Profile' screen to display the 'Current Status' (e.g., latest BP).
     */
    @Query("SELECT * FROM visits WHERE patientId = :patientId ORDER BY visitDate DESC LIMIT 1")
    suspend fun getLatestVisit(patientId: String): VisitEntity?

    /**
     * Calculates the total number of visits attended by a patient.
     * Used for clinical statistics (e.g., '3 of 8 recommended ANC visits completed').
     */
    @Query("SELECT COUNT(*) FROM visits WHERE patientId = :patientId")
    suspend fun getVisitCount(patientId: String): Int

    /**
     * Saves a single new visit record to the offline database.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVisit(visit: VisitEntity)

    /**
     * Batch inserts multiple visits at once.
     * Crucial for the initial setup or when syncing historical paper records via AI/OCR.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVisits(visits: List<VisitEntity>)
}
