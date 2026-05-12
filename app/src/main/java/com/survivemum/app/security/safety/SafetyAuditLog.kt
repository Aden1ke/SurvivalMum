package com.survivemum.app.security.safety

import android.content.Context
import android.util.Log
import com.survivemum.app.security.models.SafetyResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Persistent audit log for the SafetyScreener.
 *
 * Every screening decision becomes one row in the safety_audit table.
 * Writes happen asynchronously on a background coroutine — the caller
 * (SafetyScreener) never waits on a disk write. If a write fails, we log
 * the error but never throw: an audit log failure must never block screening.
 *
 * Use SafetyAuditLog.Noop for unit tests where Room isn't available.
 */
interface SafetyAuditLog {

    /**
     * Record a screening decision. Returns immediately; the actual database
     * write happens on a background coroutine.
     */
    fun record(
        screenType: String,
        content: String,
        result: SafetyResult,
        alertId: String? = null
    )

    companion object {
        /**
         * Production implementation backed by Room.
         */
        fun create(context: Context): SafetyAuditLog = RoomBacked(context)

        /**
         * No-op implementation for unit tests. Records nothing, fails never.
         */
        val Noop: SafetyAuditLog = object : SafetyAuditLog {
            override fun record(
                screenType: String,
                content: String,
                result: SafetyResult,
                alertId: String?
            ) {
                // Intentionally empty
            }
        }
    }
}

/**
 * Room-backed implementation. Internal — callers use SafetyAuditLog.create().
 */
private class RoomBacked(context: Context) : SafetyAuditLog {

    companion object {
        private const val TAG = "SafetyAuditLog"
    }

    private val dao = SafetyDatabase.getInstance(context).safetyAuditDao()

    /**
     * SupervisorJob: if one audit write fails, sibling writes keep working.
     * Dispatchers.IO: database writes belong on the IO dispatcher.
     */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun record(
        screenType: String,
        content: String,
        result: SafetyResult,
        alertId: String?
    ) {
        val entry = SafetyAuditEntry.from(
            screenType = screenType,
            content = content,
            result = result,
            alertId = alertId
        )

        scope.launch {
            try {
                dao.insert(entry)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to persist audit entry: ${e.message}")
                // Intentionally swallowed — audit failure must not block screening
            }
        }
    }
}