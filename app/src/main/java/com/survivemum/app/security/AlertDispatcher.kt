package com.survivemum.app.security

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import android.util.Log
import com.survivemum.app.security.models.Alert
import com.survivemum.app.security.models.AlertPriority
import com.survivemum.app.security.models.RecommendedAction
import com.survivemum.app.security.safety.SafetyScreener
import com.survivemum.app.security.models.SafetyResult
import com.survivemum.app.security.models.SafetyVerdict

/**
 * AlertDispatcher — queues alerts and delivers them through available channels.
 *
 * Design philosophy: In rural Nigeria, connectivity is a privilege, not a guarantee.
 * This component treats offline as the default state and connectivity as an opportunity.
 *
 * Delivery strategy:
 * 1. Queue every alert locally first (always works)
 * 2. If internet exists → send via WhatsApp (preferred in Nigerian healthcare)
 * 3. If only cellular exists → send CRITICAL/HIGH via SMS
 * 4. If nothing exists → hold in queue, listen for connectivity, send when available
 *
 * The TBA never manages delivery manually — the system handles everything silently.
 */
class AlertDispatcher(
    private val context: Context,
    private val safetyScreener: SafetyScreener
) {

    companion object {
        private const val TAG = "AlertDispatcher"
        private const val MAX_RETRY_ATTEMPTS = 3
    }

    private val alertQueue = mutableListOf<Alert>()
    private val retryCount = mutableMapOf<String, Int>()
    private var isListening = false

    /**
     * Queue an alert for delivery, gated by safety screening.
     *
     * Every alert passes through SafetyScreener.screenOutput before queuing:
     *   - SAFE     → queued and delivered normally
     *   - FLAGGED  → queued and delivered, but the safety result is recorded
     *                on the alert so the audit trail captures it
     *   - UNSAFE   → original alert is DROPPED. A fallback alert is queued
     *                instead so the TBA is told to assess manually rather than
     *                receiving a silent drop.
     *
     * Screening text is built from the alert's summary and reasoning fields,
     * which are the only free-form content Gemma generated. Other fields
     * (priority, type, patient ID) are structured and not screened.
     */
    fun queueAlert(alert: Alert) {
        val screeningText = buildString {
            append(alert.summary)
            append("\n\n")
            append(alert.reasoning)
        }

        val safetyResult = safetyScreener.screenOutput(screeningText)

        when (safetyResult.verdict) {
            SafetyVerdict.SAFE -> {
                // Normal flow — record the screening result on the alert
                val screened = alert.copy(safetyResult = safetyResult)
                enqueueAndDeliver(screened)
                Log.d(TAG, "Alert ${alert.alertId} passed safety screen — queued")
            }

            SafetyVerdict.FLAGGED -> {
                // Borderline content — deliver but record the flag for audit
                val screened = alert.copy(safetyResult = safetyResult)
                enqueueAndDeliver(screened)
                Log.w(
                    TAG,
                    "Alert ${alert.alertId} FLAGGED but queued: ${safetyResult.reason}"
                )
            }

            SafetyVerdict.UNSAFE -> {
                // Drop the original alert — its content is unsafe to deliver.
                // Replace with a fallback so the TBA isn't left with nothing.
                Log.e(
                    TAG,
                    "Alert ${alert.alertId} BLOCKED by safety screen: ${safetyResult.reason}"
                )
                val fallback = buildFallbackAlert(alert, safetyResult)
                enqueueAndDeliver(fallback)
            }
        }
    }

    /**
     * Internal: add to queue, prioritize, and attempt delivery.
     * Extracted from the old queueAlert body so the screening branches above
     * can each call it without duplicating logic.
     */
    private fun enqueueAndDeliver(alert: Alert) {
        alertQueue.add(alert)
        retryCount[alert.alertId] = 0
        prioritizeQueue()
        attemptDelivery()
    }

    /**
     * Build a fallback alert when the original was blocked by safety screening.
     *
     * The fallback preserves patient ID, priority, recipient, and timestamp so
     * the TBA still knows WHO to check on and HOW URGENTLY. The clinical content
     * is replaced with a manual-assessment prompt because we cannot trust Gemma's
     * generated reasoning when the screener flagged it as unsafe.
     *
     * The original alert's safety result is preserved on the fallback so the
     * audit log can show why the original was blocked.
     */
    private fun buildFallbackAlert(original: Alert, blockedResult: SafetyResult): Alert {
        return original.copy(
            alertId = java.util.UUID.randomUUID().toString(),  // new ID — distinct event
            summary = "Manual assessment needed",
            reasoning = "The AI flagged a potential concern for this patient but " +
                    "the generated assessment did not pass safety review. Please " +
                    "assess patient ${original.patientId} manually and consult " +
                    "your usual referral protocol.",
            confidence = 0.0,           // we are not confident — that's the point
            safetyResult = blockedResult, // preserve the blocked result for audit
            sent = false,
            sentAt = null
        )
    }

    /**
     * Sort queue by priority — CRITICAL first, LOW last.
     * Within same priority, older alerts go first.
     */
    private fun prioritizeQueue() {
        val sorted = mutableListOf<Alert>()
        val critical = mutableListOf<Alert>()
        val high = mutableListOf<Alert>()
        val medium = mutableListOf<Alert>()
        val low = mutableListOf<Alert>()

        for (alert in alertQueue) {
            when (alert.priority) {
                AlertPriority.CRITICAL -> critical.add(alert)
                AlertPriority.HIGH -> high.add(alert)
                AlertPriority.MEDIUM -> medium.add(alert)
                AlertPriority.LOW -> low.add(alert)
            }
        }

        sorted.addAll(critical)
        sorted.addAll(high)
        sorted.addAll(medium)
        sorted.addAll(low)

        alertQueue.clear()
        alertQueue.addAll(sorted)
    }

    /**
     * Attempt to deliver all pending alerts through
     * the best available channel.
     */
    private fun attemptDelivery() {
        if (hasInternet()) {
            deliverAllPending()
        } else if (hasCellular()) {
            deliverCriticalViaSMS()
        } else {
            Log.d(TAG, "No connectivity — ${getUnsentCount()} alerts waiting")
        }
    }

    /**
     * Deliver all unsent alerts — called when internet is available.
     */
    fun deliverAllPending() {
        val unsent = getUnsentAlerts()
        if (unsent.isEmpty()) return

        Log.d(TAG, "Delivering ${unsent.size} pending alerts")

        for (alert in unsent) {
            if (hasExceededRetries(alert)) {
                Log.w(TAG, "Alert ${alert.alertId} exceeded max retries — skipping")
                continue
            }

            val delivered = deliverSingleAlert(alert)
            if (delivered) {
                markAsDelivered(alert)
            } else {
                incrementRetry(alert)
            }
        }
    }

    /**
     * Deliver a single alert through the best available channel.
     * Preference: WhatsApp → SMS → hold in queue.
     */
    private fun deliverSingleAlert(alert: Alert): Boolean {
        if (alert.recipientPhone == null) {
            Log.w(TAG, "No recipient phone for alert ${alert.alertId}")
            return false
        }

        return try {
            if (hasInternet()) {
                deliverViaWhatsApp(alert)
            } else if (hasCellular()) {
                deliverViaSMS(alert)
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Delivery failed for ${alert.alertId}: ${e.message}")
            false
        }
    }

    /**
     * Deliver alert via WhatsApp Intent.
     * WhatsApp is the dominant messaging platform in Nigerian healthcare —
     * hospital staff, PHC workers, and even some TBAs use it daily.
     */
    private fun deliverViaWhatsApp(alert: Alert): Boolean {
        val phone = alert.recipientPhone ?: return false
        val message = formatFullAlert(alert)

        return try {
            val formattedPhone = phone.replace("+", "").replace(" ", "")

            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(
                "https://api.whatsapp.com/send?phone=$formattedPhone&text=${Uri.encode(message)}"
            )
            intent.setPackage("com.whatsapp")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                Log.d(TAG, "WhatsApp alert sent to $phone")
                true
            } else {
                Log.d(TAG, "WhatsApp not installed — falling back to SMS")
                deliverViaSMS(alert)
            }
        } catch (e: Exception) {
            Log.e(TAG, "WhatsApp delivery failed: ${e.message}")
            deliverViaSMS(alert)
        }
    }

    /**
     * Deliver alert via SMS — works without internet, only needs cellular signal.
     * SMS is the most reliable channel in rural areas where smartphones
     * may not have WhatsApp or data plans.
     */
    private fun deliverViaSMS(alert: Alert): Boolean {
        val phone = alert.recipientPhone ?: return false
        val message = formatForSMS(alert)

        return try {
            // TODO: Enable when SEND_SMS permission is added to AndroidManifest.xml
            // val smsManager = context.getSystemService(android.telephony.SmsManager::class.java)
            // if (message.length > 160) {
            //     val parts = smsManager.divideMessage(message)
            //     smsManager.sendMultipartTextMessage(phone, null, parts, null, null)
            // } else {
            //     smsManager.sendTextMessage(phone, null, message, null, null)
            // }
            Log.d(TAG, "SMS alert to $phone: $message")
            true
        } catch (e: Exception) {
            Log.e(TAG, "SMS delivery failed: ${e.message}")
            false
        }
    }

    /**
     * Format alert for SMS — concise, actionable, under 160 chars where possible.
     * Hospital workers receiving these need to act fast.
     *
     * Example output:
     * [URGENT] SurviveMum: Preeclampsia risk | HR:88 SpO2:95 BP:140/90 | REFER
     */
    private fun formatForSMS(alert: Alert): String {
        val priority = getPriorityLabel(alert.priority)
        val action = getActionLabel(alert.recommendedAction)
        val vitals = buildVitalsString(alert)

        return "[$priority] SurviveMum: ${alert.summary}$vitals | $action"
    }

    /**
     * Format full alert for WhatsApp — more detail since no character limit.
     * Includes reasoning trace so the receiving health worker
     * understands WHY the alert was triggered.
     */
    private fun formatFullAlert(alert: Alert): String {
        val priority = getPriorityLabel(alert.priority)
        val action = getFullActionLabel(alert.recommendedAction)
        val vitals = buildVitalsString(alert)

        val dateFormat = java.text.SimpleDateFormat(
            "dd/MM/yyyy HH:mm",
            java.util.Locale.getDefault()
        )
        val timeString = dateFormat.format(java.util.Date(alert.timestamp))

        val lines = mutableListOf<String>()
        lines.add("SurviveMum Alert [$priority]")
        lines.add("Patient: ${alert.patientId}")
        lines.add("Issue: ${alert.summary}")
        if (vitals.isNotEmpty()) {
            lines.add("Vitals:$vitals")
        }
        lines.add("AI Reasoning: ${alert.reasoning}")
        lines.add("Action: $action")
        lines.add("Time: $timeString")

        return lines.joinToString("\n")
    }

    /**
     * Get short priority label for SMS.
     */
    private fun getPriorityLabel(priority: AlertPriority): String {
        return when (priority) {
            AlertPriority.CRITICAL -> "URGENT"
            AlertPriority.HIGH -> "HIGH"
            AlertPriority.MEDIUM -> "MEDIUM"
            AlertPriority.LOW -> "INFO"
            else -> "INFO"
        }
    }

    /**
     * Get short action label for SMS.
     */
    private fun getActionLabel(action: RecommendedAction): String {
        return when (action) {
            RecommendedAction.IMMEDIATE_TRANSPORT -> "TRANSPORT NOW"
            RecommendedAction.REFER_HOSPITAL -> "REFER"
            RecommendedAction.MONITOR_CLOSELY -> "MONITOR"
            RecommendedAction.SCHEDULE_VISIT -> "SCHEDULE"
            else -> "REVIEW"
        }
    }

    /**
     * Get detailed action label for WhatsApp.
     */
    private fun getFullActionLabel(action: RecommendedAction): String {
        return when (action) {
            RecommendedAction.IMMEDIATE_TRANSPORT -> "IMMEDIATE TRANSPORT REQUIRED"
            RecommendedAction.REFER_HOSPITAL -> "Refer to nearest hospital"
            RecommendedAction.MONITOR_CLOSELY -> "Monitor closely, reassess in 1 hour"
            RecommendedAction.SCHEDULE_VISIT -> "Schedule follow-up visit"
            else -> "Review and take appropriate action"
        }
    }

    /**
     * Build a compact vitals string for alert messages.
     */
    private fun buildVitalsString(alert: Alert): String {
        val v = alert.vitalSigns ?: return ""

        val parts = mutableListOf<String>()
        if (v.heartRate != null) parts.add("HR:${v.heartRate}")
        if (v.spO2 != null) parts.add("SpO2:${v.spO2}")
        if (v.bloodPressureSystolic != null && v.bloodPressureDiastolic != null) {
            parts.add("BP:${v.bloodPressureSystolic}/${v.bloodPressureDiastolic}")
        }
        if (v.temperature != null) parts.add("T:${v.temperature}C")
        if (v.respiratoryRate != null) parts.add("RR:${v.respiratoryRate}")

        return if (parts.isNotEmpty()) " ${parts.joinToString(" ")}" else ""
    }

    /**
     * Deliver only CRITICAL and HIGH alerts via SMS.
     * Called when cellular exists but no internet.
     * We don't waste SMS credits on routine alerts.
     */
    private fun deliverCriticalViaSMS() {
        for (alert in alertQueue) {
            if (alert.sent) continue
            if (alert.recipientPhone == null) continue
            if (alert.priority != AlertPriority.CRITICAL && alert.priority != AlertPriority.HIGH) continue
            if (hasExceededRetries(alert)) continue

            if (deliverViaSMS(alert)) {
                markAsDelivered(alert)
            } else {
                incrementRetry(alert)
            }
        }
    }

    /**
     * Mark alert as delivered — update timestamp.
     */
    private fun markAsDelivered(alert: Alert) {
        val index = alertQueue.indexOf(alert)
        if (index >= 0) {
            alertQueue[index] = alert.copy(
                sent = true,
                sentAt = System.currentTimeMillis()
            )
            retryCount.remove(alert.alertId)
            Log.d(TAG, "Alert ${alert.alertId} delivered successfully")
        }
        // TODO: Update in FS's SQLite database
    }

    /**
     * Track retry attempts to prevent infinite retry loops.
     */
    private fun incrementRetry(alert: Alert) {
        val current = retryCount[alert.alertId] ?: 0
        retryCount[alert.alertId] = current + 1
    }

    private fun hasExceededRetries(alert: Alert): Boolean {
        return (retryCount[alert.alertId] ?: 0) >= MAX_RETRY_ATTEMPTS
    }

    /**
     * Start listening for connectivity changes.
     * When network appears, automatically deliver pending alerts.
     * This is the core of the "opportunistic delivery" pattern.
     */
    fun startConnectivityListener() {
        if (isListening) return

        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                Log.d(TAG, "Network detected — delivering pending alerts")
                deliverAllPending()
            }

            override fun onLost(network: Network) {
                Log.d(TAG, "Network lost — alerts queued")
            }
        }

        cm.registerNetworkCallback(request, callback)

        isListening = true
        Log.d(TAG, "Connectivity listener active")
    }

    /**
     * Check if internet connectivity exists.
     */
    private fun hasInternet(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    /**
     * Check if cellular network is available (for SMS).
     */
    private fun hasCellular(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
    }

    /**
     * Get count of unsent alerts.
     */
    fun getUnsentCount(): Int {
        var count = 0
        for (alert in alertQueue) {
            if (!alert.sent) count++
        }
        return count
    }

    /**
     * Get all unsent alerts.
     */
    fun getUnsentAlerts(): List<Alert> {
        val unsent = mutableListOf<Alert>()
        for (alert in alertQueue) {
            if (!alert.sent) unsent.add(alert)
        }
        return unsent
    }

    /**
     * Clear delivered alerts older than specified hours.
     * Keeps queue manageable on low-storage devices.
     */
    fun clearOldAlerts(hoursOld: Int = 48) {
        val cutoff = System.currentTimeMillis() - (hoursOld * 3_600_000L)
        val iterator = alertQueue.iterator()
        while (iterator.hasNext()) {
            val alert = iterator.next()
            if (alert.sent && (alert.sentAt ?: 0) < cutoff) {
                iterator.remove()
            }
        }
    }
}