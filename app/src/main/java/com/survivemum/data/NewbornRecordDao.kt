package com.survivemum.data

import androidx.room.*

/**
 * Data Access Object for neonatal records.
 * Manages the foundational health data and survival milestones for newborns.
 */
@Dao
interface NewbornRecordDao {

    /**
     * Retrieves the specific record for an infant linked to a Mother (patientId).
     * Used to display the baby's birth profile, weight, and delivery details.
     */
    @Query("SELECT * FROM newborn_records WHERE patientId = :patientId LIMIT 1")
    suspend fun getNewbornRecord(patientId: String): NewbornRecordEntity?

    /**
     * Creates or updates the birth record for a new infant.
     * Essential for establishing the baseline health status 100% offline.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNewbornRecord(record: NewbornRecordEntity)

    /**
     * Updates the critical survival status at the end of the neonatal period.
     * This tracks the success of the "Silent Guardian" in protecting the child.
     */
    @Query("UPDATE newborn_records SET day28Status = :status WHERE patientId = :patientId")
    suspend fun updateDay28Status(patientId: String, status: String)
}
