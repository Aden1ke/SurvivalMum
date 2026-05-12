package com.survivemum.app.data

import androidx.room.*

/**
 * Data Access Object for User operations.
 * Manages authentication and user profile retrieval.
 */
@Dao
interface UserDao {

    /**
     * Saves or updates a user profile.
     * Used during the initial onboarding or when syncing profile updates from the server.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    /**
     * Finds a user by their specific ID.
     */
    @Query("SELECT * FROM users WHERE userId = :userId")
    suspend fun getUser(userId: String): UserEntity?

    /**
     * Finds a user by their phone number.
     * Crucial for the login flow to verify if an account exists before checking the PIN.
     */
    @Query("SELECT * FROM users WHERE phoneNumber = :phone")
    suspend fun getUserByPhone(phone: String): UserEntity?

    /**
     * Updates the last login timestamp.
     * Called immediately after a successful PIN verification.
     */
    @Query("UPDATE users SET lastLoginAt = :timestamp WHERE userId = :userId")
    suspend fun updateLastLogin(userId: String, timestamp: String)

    /**
     * Retrieves the current user logged into the device.
     * Used to determine if the app should open the Dashboard or the Login screen on startup.
     */
    @Query("SELECT * FROM users LIMIT 1")
    suspend fun getCurrentUser(): UserEntity?
}