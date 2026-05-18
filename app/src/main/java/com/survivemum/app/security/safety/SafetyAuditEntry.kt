package com.survivemum.app.security.safety

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.survivemum.app.security.models.SafetyVerdict

/**
 * One row per safety screening decision.
 *
 * This is the persistent audit trail for SurviveMum's safety layer.
 * Every call to SafetyScreener.screenInput or screenOutput produces an entry,
 * regardless of verdict. SAFE entries are kept (not just rejections) because
 * the absence of an audit row would itself be a red flag.
 *
 * The schema is intentionally flat for easy SQL inspection and CSV export.
 * A judge or auditor can run `SELECT * FROM safety_audit ORDER BY timestamp DESC`
 * and immediately understand what the screener saw and decided.
 */
@Entity(tableName = "safety_audit")
data class SafetyAuditEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** "INPUT" or "OUTPUT" — which screening method was called */
    val screenType: String,

    /** Verdict as a string ("SAFE", "FLAGGED", "UNSAFE") for SQL readability */
    val verdict: String,

    /** Human-readable reason from SafetyResult, or null for clean SAFE results */
    val reason: String?,

    /** Comma-separated list of policies violated; empty string if none */
    val policiesViolated: String,

    /** 0.0–1.0 confidence in the verdict */
    val confidence: Double,

    /** How long the screening call took in milliseconds */
    val screeningMs: Long,

    /**
     * The text that was screened. Stored truncated to 500 chars to keep
     * the audit log from ballooning on long Gemma outputs.
     */
    val contentSnippet: String,

    /** Optional alert ID this screening was tied to, when known */
    val alertId: String?,

    /** Wall-clock time of the decision */
    val timestamp: Long = System.currentTimeMillis()
) {
    companion object {
        /**
         * Factory: build an audit entry from a SafetyResult plus context.
         * Keeps the conversion logic in one place so SafetyScreener doesn't
         * have to know about Room.
         */
        fun from(
            screenType: String,
            content: String,
            result: com.survivemum.app.security.models.SafetyResult,
            alertId: String? = null
        ): SafetyAuditEntry {
            val snippet = if (content.length > 500) {
                content.take(497) + "..."
            } else {
                content
            }

            return SafetyAuditEntry(
                screenType = screenType,
                verdict = result.verdict.name,
                reason = result.reason,
                policiesViolated = result.policiesViolated.joinToString(", "),
                confidence = result.confidence,
                screeningMs = result.screeningMs,
                contentSnippet = snippet,
                alertId = alertId
            )
        }
    }
}