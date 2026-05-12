package com.survivemum.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a registered user (TBA) in the system.
 * This stores profile information and security credentials for local authentication.
 */
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val userId: String,          // Unique ID for the user (usually from the backend)

    val fullName: String,        // The full name of the TBA or health worker

    val phoneNumber: String? = null, // Primary contact, used as a login identifier

    val facilityName: String = "", // The healthcare facility or center they are assigned to

    val community: String = "",    // The primary local community they serve

    val language: String = "en",   // Default UI language preference

    val pinHash: String,         // Hashed security PIN. Security note: Never store raw PINs locally.
    val userType: String = "TBA", // diffrentiate between uses "TBA" or "MOTHER"

    val createdAt: String,       // Timestamp of account creation

    val lastLoginAt: String? = null, // Tracking for session management and security audits

    val isActive: Int = 1        // 1 for active users, 0 for deactivated accounts
)
