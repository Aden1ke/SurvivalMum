package com.survivemum.app.ml

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject

/**
 * RagEngine — On-Device RAG for SurviveMum
 * Loads WHO guidelines from assets and retrieves the most relevant
 * guideline before every Gemma clinical assessment.
 * Stops hallucination. Grounds every recommendation in real evidence.
 */
class RagEngine(private val context: Context) {

    private var keywordLookup: JSONObject? = null
    private var knowledgeBase: JSONArray? = null
    private var isReady = false

    fun initialize() {
        try {
            // Load keyword lookup — fast retrieval for low-end devices
            val keywordJson = context.assets.open("keyword_lookup.json")
                .bufferedReader().use { it.readText() }
            keywordLookup = JSONObject(keywordJson)

            // Load full knowledge base
            val kbJson = context.assets.open("knowledge_base.json")
                .bufferedReader().use { it.readText() }
            knowledgeBase = JSONArray(kbJson)

            isReady = true
            Log.d("RagEngine", "✅ RAG engine ready — ${knowledgeBase?.length()} clinical documents loaded")

        } catch (e: Exception) {
            Log.e("RagEngine", "Failed to initialize RAG engine: ${e.message}")
        }
    }

    /**
     * Retrieve the most relevant clinical guideline for a given situation.
     * Called before every Gemma prompt.
     */
    fun retrieve(clinicalQuery: String): String {
        if (!isReady) return ""

        val query = clinicalQuery.lowercase()

        // Step 1 — keyword match against lookup table (fast path)
        val keywords = listOf(
            "blood pressure", "headache", "bleeding", "haemorrhage",
            "cry", "newborn", "jaundice", "fever", "fontanelle", "seizure"
        )

        for (keyword in keywords) {
            if (query.contains(keyword)) {
                val result = keywordLookup?.optString(keyword, "") ?: ""
                if (result.isNotBlank()) {
                    Log.d("RagEngine", "Retrieved guideline for keyword: $keyword")
                    return result
                }
            }
        }

        // Step 2 — topic match against full knowledge base (thorough path)
        val topicKeywords = mapOf(
            "preeclampsia" to listOf("bp", "pressure", "oedema", "swelling", "headache", "vision"),
            "haemorrhage" to listOf("bleed", "blood", "placenta", "postpartum", "delivery"),
            "newborn_respiratory" to listOf("breath", "chest", "grunt", "flare", "cyanosis"),
            "newborn_cry" to listOf("cry", "scream", "wail", "silent", "weak"),
            "newborn_jaundice" to listOf("yellow", "jaundice", "skin colour", "sclera"),
            "neonatal_infection" to listOf("fontanelle", "bulge", "meningitis", "stiff", "high pitched"),
            "sepsis" to listOf("fever", "temperature", "infection", "sepsis"),
            "anaemia" to listOf("pale", "pallor", "anaemia", "tired", "fatigue"),
            "referral" to listOf("emergency", "critical", "refer", "transfer", "convulsion")
        )

        var bestTopic: String? = null
        var bestScore = 0

        for ((topic, keys) in topicKeywords) {
            val score = keys.count { query.contains(it) }
            if (score > bestScore) {
                bestScore = score
                bestTopic = topic
            }
        }

        if (bestTopic != null && knowledgeBase != null) {
            for (i in 0 until knowledgeBase!!.length()) {
                val doc = knowledgeBase!!.getJSONObject(i)
                if (doc.getString("topic") == bestTopic) {
                    Log.d("RagEngine", "Retrieved knowledge base doc for topic: $bestTopic")
                    return "[${doc.getString("source")}]\n${doc.getString("text")}"
                }
            }
        }

        // Step 3 — fallback: return emergency referral guideline
        return "WHO Emergency Triage: BP ≥160/110, heavy bleeding, convulsions, " +
                "absent fetal movement, or severe breathing difficulty = immediate referral. " +
                "Do not delay transport."
    }

    fun isReady() = isReady
}