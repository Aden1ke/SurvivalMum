package com.survivemum.app.security

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.util.Log
import com.survivemum.app.security.models.BatteryState
import com.survivemum.app.security.models.DeviceCapability

/**
 * BatteryMonitor — tracks battery level, charging state, and temperature.
 * Feeds data to ModelRouter for E2B/E4B decisions.
 * Adjusts camera frame processing rate to conserve power.
 *
 * On an $80 phone, battery management is the difference
 * between the app surviving a full clinic day or dying at noon.
 */
class BatteryMonitor(private val context: Context) {

    companion object {
        private const val TAG = "BatteryMonitor"

        private const val CRITICAL_THRESHOLD = 10
        private const val LOW_THRESHOLD = 25
        private const val MEDIUM_THRESHOLD = 50
    }

    // Track battery readings to calculate drain rate
    private val batteryHistory = mutableListOf<Pair<Long, Int>>()
    private val maxHistorySize = 20

    /**
     * Get current battery percentage (0-100).
     */
    fun getBatteryLevel(): Int {
        val batteryStatus = context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )

        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100

        return if (level >= 0 && scale > 0) {
            (level * 100) / scale
        } else {
            Log.w(TAG, "Could not read battery level")
            50
        }
    }

    /**
     * Check if device is currently charging.
     */
    fun isCharging(): Boolean {
        val batteryStatus = context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )

        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        return status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL
    }

    /**
     * Get battery state — used by ModelRouter for routing decisions.
     */
    fun getBatteryState(): BatteryState {
        if (isCharging()) return BatteryState.CHARGING

        return when (val level = getBatteryLevel()) {
            in 0..CRITICAL_THRESHOLD -> BatteryState.CRITICAL
            in (CRITICAL_THRESHOLD + 1)..LOW_THRESHOLD -> BatteryState.LOW
            in (LOW_THRESHOLD + 1)..MEDIUM_THRESHOLD -> BatteryState.MEDIUM
            else -> BatteryState.GOOD
        }
    }

    /**
     * Recommend camera frame processing rate based on battery.
     * Higher rate = more accurate rPPG but more battery drain.
     * Returns how many frames to skip (1 = every frame, 3 = every 3rd frame).
     */
    fun getRecommendedFrameSkip(): Int {
        return when (getBatteryState()) {
            BatteryState.CHARGING, BatteryState.GOOD -> 1
            BatteryState.MEDIUM -> 3
            BatteryState.LOW -> 5
            BatteryState.CRITICAL -> 10
        }
    }

    /**
     * Should we defer non-urgent assessments to save battery?
     * Only CRITICAL and HIGH priority alerts override low battery.
     */
    fun shouldDeferAssessment(isCriticalAlert: Boolean): Boolean {
        val state = getBatteryState()
        if (isCriticalAlert) return false

        return state == BatteryState.CRITICAL
    }

    /**
     * Record current battery level for drain rate calculation.
     * Call this periodically from the monitoring service.
     */
    fun recordBatteryReading() {
        val level = getBatteryLevel()
        val now = System.currentTimeMillis()
        batteryHistory.add(Pair(now, level))

        if (batteryHistory.size > maxHistorySize) {
            batteryHistory.removeAt(0)
        }
    }

    /**
     * Estimate minutes remaining based on recent drain rate.
     * Returns null if not enough data to estimate.
     *
     * This matters because a TBA needs to know:
     * "Can I finish this assessment before the phone dies?"
     */
    fun estimateMinutesRemaining(): Int? {
        if (batteryHistory.size < 3) return null

        val oldest = batteryHistory.first()
        val newest = batteryHistory.last()

        val timeDiffMinutes = (newest.first - oldest.first) / 60_000.0
        val levelDiff = oldest.second - newest.second

        if (timeDiffMinutes <= 0 || levelDiff <= 0) return null

        val drainPerMinute = levelDiff / timeDiffMinutes
        val remaining = newest.second / drainPerMinute

        return remaining.toInt()
    }

    /**
     * Check device temperature — cheap phones overheat running AI models.
     * Returns temperature in Celsius, or null if unavailable.
     */
    fun getDeviceTemperature(): Float? {
        val batteryStatus = context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )

        val temp = batteryStatus?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1
        return if (temp > 0) temp / 10.0f else null
    }

    /**
     * Is the device overheating?
     * Above 45C is dangerous for sustained AI inference.
     */
    fun isOverheating(): Boolean {
        val temp = getDeviceTemperature() ?: return false
        return temp > 45.0f
    }

    /**
     * Comprehensive device capability — combines battery level,
     * charging state, temperature, and drain rate.
     * ModelRouter calls this for routing decisions.
     */
    fun getDeviceCapability(): DeviceCapability {
        val state = getBatteryState()
        val overheating = isOverheating()
        val minutesLeft = estimateMinutesRemaining()

        return when {
            overheating -> DeviceCapability.THROTTLED
            state == BatteryState.CRITICAL -> DeviceCapability.MINIMAL
            minutesLeft != null && minutesLeft < 15 -> DeviceCapability.MINIMAL
            state == BatteryState.LOW -> DeviceCapability.REDUCED
            minutesLeft != null && minutesLeft < 30 -> DeviceCapability.REDUCED
            else -> DeviceCapability.FULL
        }
    }

    /**
     * Get a human-readable status for logging.
     */
    fun getStatusSummary(): String {
        val level = getBatteryLevel()
        val state = getBatteryState()
        val charging = if (isCharging()) " (charging)" else ""
        val frameSkip = getRecommendedFrameSkip()
        val temp = getDeviceTemperature()?.let { "${it}C" } ?: "unknown"
        val mins = estimateMinutesRemaining()?.let { "${it}min left" } ?: "estimating"

        return "Battery: $level%$charging | State: $state | Temp: $temp | $mins | Frame skip: $frameSkip"
    }
}