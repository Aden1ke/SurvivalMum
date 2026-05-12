package com.survivemum.app.security.safety

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

/**
 * Data Access Object for the safety audit log.
 *
 * Suspend functions throughout — Room enforces that database calls must not
 * happen on the main thread. The screener calls these from a background
 * coroutine so screening latency is unaffected by audit writes.
 */
@Dao
interface SafetyAuditDao {

    @Insert
    suspend fun insert(entry: SafetyAuditEntry): Long

    /** Most recent entries first — the typical "show me the audit trail" view */
    @Query("SELECT * FROM safety_audit ORDER BY timestamp DESC LIMIT :limit")
    suspend fun recent(limit: Int = 100): List<SafetyAuditEntry>

    /** All UNSAFE blocks — the rejection list, useful for the writeup screenshot */
    @Query("SELECT * FROM safety_audit WHERE verdict = 'UNSAFE' ORDER BY timestamp DESC")
    suspend fun blocked(): List<SafetyAuditEntry>

    /** Audit history for a specific alert — for "why was this dropped?" queries */
    @Query("SELECT * FROM safety_audit WHERE alertId = :alertId ORDER BY timestamp DESC")
    suspend fun forAlert(alertId: String): List<SafetyAuditEntry>

    /** Total decisions to date — useful for showing volume in the writeup */
    @Query("SELECT COUNT(*) FROM safety_audit")
    suspend fun totalCount(): Int
}