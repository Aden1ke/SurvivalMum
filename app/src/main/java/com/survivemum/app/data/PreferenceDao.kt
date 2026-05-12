package com.survivemum.app.data

import androidx.room.*

/**
 * Data Access Object for application and user preferences.
 * Manages how the "Silent Guardian" logic adapts to the guardian's environment.
 */
@Dao
interface PreferenceDao {

    /**
     * Retrieves the saved settings for a specific user.
     * Used at app startup to configure the UI theme, language, and AI sensitivity.
     */
    @Query("SELECT * FROM preferences WHERE userId = :userId")
    suspend fun getPreferences(userId: String): PreferenceEntity?

    /**
     * Saves or updates the entire preference set.
     * Used when the user first completes the onboarding or hits "Save" in Settings.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun savePreferences(preferences: PreferenceEntity)

    /**
     * Specifically updates the UI language (e.g., switching from English to a local dialect).
     * Essential for ensuring the AI's life-saving alerts are understood by the guardian.
     */
    @Query("UPDATE preferences SET language = :language, updatedAt = :updatedAt WHERE userId = :userId")
    suspend fun updateLanguage(userId: String, language: String, updatedAt: String)

    /**
     * Adjusts the 'Monitoring Sensitivity' of the on-device AI.
     * Allows a TBA to toggle how strictly Gemma 4 flags anomalies in vitals or cries.
     */
    @Query("UPDATE preferences SET monitoringSensitivity = :sensitivity, updatedAt = :updatedAt WHERE userId = :userId")
    suspend fun updateSensitivity(userId: String, sensitivity: String, updatedAt: String)
}
