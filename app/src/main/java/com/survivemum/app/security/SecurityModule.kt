package com.survivemum.app.security

import android.content.Context
import com.survivemum.app.ml.GemmaManager
import com.survivemum.app.security.safety.SafetyAuditLog
import com.survivemum.app.security.safety.SafetyScreener

/**
 * Construction container for the security layer.
 *
 * Wires together the safety screener (with optional Gemma layer), audit log,
 * alert dispatcher, battery monitor, and model router in the right order.
 * Built once at app startup and held for the lifetime of the process.
 *
 * Pass GemmaManager in to enable Layer 2 semantic safety screening; pass
 * null to run rule-based screening only. When Gemma is still loading at
 * construction time, that's fine — SafetyScreener will return SAFE for the
 * Gemma layer until Gemma finishes loading, then start producing real
 * semantic verdicts automatically.
 *
 * Why a manual container instead of a DI framework like Hilt:
 *   - Keeps the dependency graph small and readable
 *   - Avoids adding KSP processors for hilt-compiler (we already
 *     fought enough KSP battles for one hackathon)
 *   - The wiring is small enough that Hilt's overhead isn't justified
 *
 * If the project grows beyond ~20 components, consider migrating to Hilt.
 */
class SecurityModule(
    applicationContext: Context,
    gemmaManager: GemmaManager? = null
) {

    /** Persistent audit trail for every safety screening decision. */
    val auditLog: SafetyAuditLog = SafetyAuditLog.create(applicationContext)

    /** Battery + temperature + drain rate tracker. */
    val batteryMonitor: BatteryMonitor = BatteryMonitor(applicationContext)

    /** Routes between Gemma E2B / E4B based on device capability. */
    val modelRouter: ModelRouter = ModelRouter(applicationContext, batteryMonitor)

    /**
     * Safety screener with audit log and (optional) Gemma layer attached.
     * Every screenInput / screenOutput call writes one row to safety_audit.
     */
    val safetyScreener: SafetyScreener = SafetyScreener(
        context = applicationContext,
        auditLog = auditLog,
        gemma = gemmaManager
    )

    /** Queues alerts and delivers them via WhatsApp / SMS, gated by safety screening. */
    val alertDispatcher: AlertDispatcher = AlertDispatcher(
        context = applicationContext,
        safetyScreener = safetyScreener
    )

    init {
        // Start listening for connectivity changes so queued alerts
        // get delivered as soon as the network comes up.
        alertDispatcher.startConnectivityListener()
    }
}