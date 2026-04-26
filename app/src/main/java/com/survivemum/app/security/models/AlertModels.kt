package com.survivemum.app.security.models

import java.util.UUID

/**
 * Alert priority levels — determines routing and send urgency.
 * CRITICAL and HIGH override battery-saving to ensure delivery.
 */
enum class AlertPriority {
    CRITICAL,  // Preeclampsia, respiratory distress — send immediately
    HIGH,      // Abnormal vitals trend — send within minutes
    MEDIUM,    // Missed ANC visit — send when convenient
    LOW        // Routine follow-up — batch and send
}

/**
 * Clinical alert types — what danger was detected.
 */
enum class AlertType {
    PREECLAMPSIA,
    RESPIRATORY_DISTRESS,
    JAUNDICE,
    CRY_ABNORMAL,
    VITAL_TREND,
    MISSED_VISIT,
    DEVELOPMENTAL_DELAY,
    MOVEMENT_ASYMMETRY
}

/**
 * What action the TBA should take.
 */
enum class RecommendedAction {
    REFER_HOSPITAL,
    MONITOR_CLOSELY,
    SCHEDULE_VISIT,
    IMMEDIATE_TRANSPORT
}

/**
 * Result from ShieldGemma safety screening.
 */
enum class SafetyVerdict {
    SAFE,
    UNSAFE,
    FLAGGED  // Not blocked, but logged for review
}

data class SafetyResult(
    val verdict: SafetyVerdict,
    val reason: String? = null,
    val policyViolated: String? = null,
    val confidence: Double = 0.0
)

/**
 * Battery states — drives model routing and frame rate decisions.
 */
enum class BatteryState {
    CRITICAL,   // 0-10%
    LOW,        // 10-25%
    MEDIUM,     // 25-50%
    GOOD,       // 50-100%
    CHARGING    // Any level, plugged in
}

/**
 * Which Gemma model to use.
 */
enum class ModelSize {
    E2B,   // Small, fast, low battery cost
    E4B    // Big, smart, high battery cost
}

/**
 * Types of requests the AI can receive — determines model routing.
 */
enum class RequestType {
    SIMPLE_QUERY,
    VITAL_DISPLAY,
    CRY_CLASSIFICATION,
    CLINICAL_ASSESSMENT,
    PREECLAMPSIA_CHECK,
    EMERGENCY_TRIAGE
}

/**
 * Device capability level — combines battery, temperature,
 * and drain rate into one decision for ModelRouter.
 */
enum class DeviceCapability {
    FULL,       // Everything available — use E4B freely
    REDUCED,    // Conserve — prefer E2B, E4B for clinical only
    MINIMAL,    // Emergency only — E2B default, E4B for life-threatening
    THROTTLED   // Overheating — E2B only, reduce frame rate, alert TBA
}