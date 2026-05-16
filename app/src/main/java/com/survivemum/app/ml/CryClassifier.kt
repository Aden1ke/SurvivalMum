package com.survivemum.app.ml

import android.content.Context
import org.json.JSONObject

class CryClassifier(private val context: Context) {

    private lateinit var patterns: JSONObject

    fun initialize() {
        val json = context.assets.open("cry_patterns.json")
            .bufferedReader().use { it.readText() }
        patterns = JSONObject(json)
    }

    fun classify(pitchHz: Float, burstDurationSeconds: Float): CryResult {
        return when {
            pitchHz > 400 && burstDurationSeconds < 0.5f -> CryResult(
                label = "DISTRESS",
                description = "High-pitched short bursts — possible meningitis",
                severity = "CRITICAL"
            )
            pitchHz < 250 && burstDurationSeconds > 2.0f -> CryResult(
                label = "RESPIRATORY",
                description = "Weak breathy cry — respiratory distress",
                severity = "CRITICAL"
            )
            pitchHz in 300f..500f && burstDurationSeconds > 1.0f -> CryResult(
                label = "PAIN",
                description = "Intense sustained cry — pain response",
                severity = "HIGH"
            )
            else -> CryResult(
                label = "NORMAL",
                description = "Normal healthy cry",
                severity = "LOW"
            )
        }
    }
}

data class CryResult(
    val label: String,
    val description: String,
    val severity: String
)