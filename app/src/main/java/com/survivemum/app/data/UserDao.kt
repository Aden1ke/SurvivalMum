package com.survivemum.app.data

import androidx.room.*

@Dao
interface UserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    // Returns the most-recently-active user of a specific type.
    // This is the fix for issue 5: a TBA PIN no longer unlocks a mother account
    // because we scope the lookup to the userType the login screen was opened with.
    @Query("""
        SELECT * FROM users 
        WHERE userType = :userType 
          AND isActive = 1 
        ORDER BY lastLoginAt DESC 
        LIMIT 1
    """)
    suspend fun getUserByType(userType: String): UserEntity?

    // Still used by HomeDashboardScreen and SettingsScreen to load the
    // currently active user's name/preferences after login.
    @Query("""
        SELECT * FROM users 
        WHERE isActive = 1 
        ORDER BY lastLoginAt DESC 
        LIMIT 1
    """)
    suspend fun getCurrentUser(): UserEntity?

    @Query("""
        UPDATE users 
        SET lastLoginAt = :timestamp 
        WHERE userId = :userId
    """)
    suspend fun updateLastLogin(userId: String, timestamp: String)

    @Query("""
        UPDATE users 
        SET isActive = 0 
        WHERE userId = :userId
    """)
    suspend fun deactivateUser(userId: String)

    @Query("SELECT * FROM users WHERE userId = :userId LIMIT 1")
    suspend fun getUser(userId: String): UserEntity?

    // Sign out: marks all users inactive so next launch hits UserTypeScreen
    @Query("UPDATE users SET isActive = 0")
    suspend fun signOutAll()
}