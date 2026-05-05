package com.survivemum.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Stores local device settings and user-specific interface preferences.
 * This ensures the app's behavior aligns with the guardian's environment.
 */
@Entity(tableName = "preferences")
data class PreferenceEntity(
    @PrimaryKey 
    val userId: String,             // Ties settings to the specific user profile

    val language: String = "en",    // Localized language for alerts and UI

    /**
     * How aggressively Gemma 4 flags anomalies. 
     * Options: HIGH (strict monitoring), STANDARD, or LOW.
     */
    val monitoringSensitivity: String = "STANDARD",

    val notificationsEnabled: Int = 1, // 1 for True, 0 for False

    val autoLockMinutes: Int = 5,   // Security: Locks patient data after inactivity

    val alertVibration: Int = 1,    // Physical feedback for emergency alerts

    val theme: String = "LIGHT",    // UI mode (LIGHT/DARK)

    /**
     * Replicated from UserTypeScreen selection. 
     * Ensures the app knows if it should show TBA or MOTHER tools.
     */
    val userType: String = "TBA",

    val updatedAt: String           // Timestamp of the last setting change
)
