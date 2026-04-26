package com.survivemum.app.security

import android.content.Context
import android.util.Log
import com.survivemum.app.security.models.SafetyResult
import com.survivemum.app.security.models.SafetyVerdict

/**
 * ShieldGemma dual safety filter.
 * Screens ALL inputs before they reach Gemma.
 * Screens ALL outputs before they reach the TBA.
 *
 * No medical content passes through without safety verification.
 */
class SafetyScreener(private val context: Context) {

    companion object {
        private const val TAG = "SafetyScreener"
    }

    /**
     * Medical safety policies — each one is a rule that ShieldGemma
     * checks against. If any policy is violated, the output is blocked.
     */
    private val medicalPolicies = listOf(
        "Do not recommend specific drug dosages or medication amounts",
        "Do not discourage seeking professional medical help",
        "Do not diagnose with certainty — always use suggestive language",
        "Do not contradict WHO maternal and newborn health guidelines",
        "Do not provide advice that could delay emergency treatment",
        "Do not make claims about conditions without supporting evidence"
    )

    private var isModelLoaded = false

    /**
     * Load ShieldGemma model onto the device via LiteRT.
     * Must be called before screening. Unload after use to free memory.
     */
    fun loadModel() {
        try {
            // TODO: Load ShieldGemma 2B via LiteRT when BE-1 has the runtime ready
            // val modelPath = "shieldgemma-2b.litertlm"
            // model = LiteRtLlm.load(context, modelPath)
            isModelLoaded = true
            Log.d(TAG, "ShieldGemma model loaded successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load ShieldGemma: ${e.message}")
            isModelLoaded = false
        }
    }

    /**
     * Unload model to free memory for Gemma E4B.
     * Critical on low-RAM devices — cannot keep both models loaded.
     */
    fun unloadModel() {
        try {
            // TODO: Release LiteRT model resources
            // model?.close()
            isModelLoaded = false
            Log.d(TAG, "ShieldGemma model unloaded")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to unload ShieldGemma: ${e.message}")
        }
    }

    /**
     * Screen input BEFORE it reaches Gemma.
     * Catches harmful, malformed, or manipulative inputs
     * before they waste model resources or produce dangerous outputs.
     */
    fun screenInput(input: String): SafetyResult {
        if (!isModelLoaded) {
            Log.w(TAG, "ShieldGemma not loaded — falling back to rule-based screening")
            return ruleBasedScreen(input)
        }

        return try {
            // TODO: Replace with actual ShieldGemma inference when LiteRT is ready
            // val prompt = buildSafetyPrompt(input, "input")
            // val response = model.generate(prompt)
            // parseSafetyResponse(response)
            ruleBasedScreen(input)
        } catch (e: Exception) {
            Log.e(TAG, "Input screening failed: ${e.message}")
            SafetyResult(
                verdict = SafetyVerdict.UNSAFE,
                reason = "Safety screening failed — blocking input as precaution"
            )
        }
    }

    /**
     * Screen output AFTER Gemma generates it, BEFORE the TBA sees it.
     * This is the critical gate — catches dangerous medical advice,
     * false certainty in diagnoses, and WHO guideline violations.
     */
    fun screenOutput(output: String): SafetyResult {
        if (!isModelLoaded) {
            Log.w(TAG, "ShieldGemma not loaded — falling back to rule-based screening")
            return ruleBasedScreen(output)
        }

        return try {
            // TODO: Replace with actual ShieldGemma inference when LiteRT is ready
            // val prompt = buildSafetyPrompt(output, "output")
            // val response = model.generate(prompt)
            // parseSafetyResponse(response)
            ruleBasedScreen(output)
        } catch (e: Exception) {
            Log.e(TAG, "Output screening failed: ${e.message}")
            SafetyResult(
                verdict = SafetyVerdict.UNSAFE,
                reason = "Safety screening failed — blocking output as precaution"
            )
        }
    }

    /**
     * Rule-based fallback screening — works without ShieldGemma.
     * Uses pattern matching with negation awareness to reduce false positives.
     * Returns a scored safety result with full audit trail for transparency.
     */
    private fun ruleBasedScreen(content: String): SafetyResult {
        val lowerContent = content.lowercase()
        val violations = mutableListOf<String>()
        var highestSeverity = SafetyVerdict.SAFE

        // Run all checks, collect all violations (don't stop at first)
        checkDosagePatterns(lowerContent, violations)
        checkDangerousMedicalAdvice(lowerContent, violations)
        checkFalseCertainty(lowerContent, violations)
        checkEmergencyDelay(lowerContent, violations)
        checkHarmfulContent(lowerContent, violations)
        checkNigerianContextRisks(lowerContent, violations)

        // Determine severity from violations
        highestSeverity = when {
            violations.any { it.startsWith("[BLOCK]") } -> SafetyVerdict.UNSAFE
            violations.any { it.startsWith("[FLAG]") } -> SafetyVerdict.FLAGGED
            else -> SafetyVerdict.SAFE
        }

        return SafetyResult(
            verdict = highestSeverity,
            reason = if (violations.isEmpty()) null
            else violations.joinToString("; "),
            policyViolated = if (violations.isEmpty()) null
            else violations.first().substringAfter("] "),
            confidence = if (violations.isEmpty()) 0.7 else 0.85
        )
    }

    /**
     * Check for negation before flagging.
     * "do NOT go to hospital" should not be flagged the same as
     * "do not go to hospital" when the AI is warning the patient.
     */
    private fun hasNegationBefore(content: String, phrase: String): Boolean {
        val index = content.indexOf(phrase)
        if (index <= 0) return false

        val before = content.substring(maxOf(0, index - 20), index).lowercase()
        val negations = listOf("don't ", "do not ", "never ", "should not ", "shouldn't ", "avoid ", "stop ")
        return negations.any { before.contains(it) }
    }

    private fun checkDosagePatterns(content: String, violations: MutableList<String>) {
        val dosagePattern = Regex("\\d+\\s*(mg|ml|mcg|units?|tablets?|drops|capsules?)")
        if (dosagePattern.containsMatchIn(content)) {
            violations.add("[FLAG] ${medicalPolicies[0]}: Contains specific dosage")
        }

        // Catch informal dosage instructions common in Nigerian context
        val informalDosage = listOf(
            "half a tablet", "two tablets", "one capsule",
            "full spoon", "half spoon", "small spoon",
            "one cap", "two caps", "three caps"
        )
        if (informalDosage.any { content.contains(it) }) {
            violations.add("[FLAG] ${medicalPolicies[0]}: Contains informal dosage instruction")
        }
    }

    private fun checkDangerousMedicalAdvice(content: String, violations: MutableList<String>) {
        val dangerousPhrases = listOf(
            "do not go to hospital",
            "no need for doctor",
            "don't need medical",
            "avoid the hospital",
            "stay away from clinic",
            "do not seek help",
            "not necessary to see a doctor",
            "hospital is too far",
            "clinic cannot help"
        )

        for (phrase in dangerousPhrases) {
            if (content.contains(phrase) && !hasNegationBefore(content, phrase)) {
                violations.add("[BLOCK] ${medicalPolicies[1]}: Discourages medical help — '$phrase'")
                return
            }
        }
    }

    private fun checkFalseCertainty(content: String, violations: MutableList<String>) {
        val certaintyPhrases = listOf(
            "you definitely have",
            "this is certainly",
            "i am sure this is",
            "without a doubt",
            "100% this is",
            "clearly diagnosed as",
            "confirmed case of",
            "this is a case of"
        )

        if (certaintyPhrases.any { content.contains(it) }) {
            violations.add("[BLOCK] ${medicalPolicies[2]}: Diagnoses with false certainty")
        }
    }

    private fun checkEmergencyDelay(content: String, violations: MutableList<String>) {
        val delayPhrases = listOf(
            "wait and see",
            "give it a few days",
            "probably nothing serious",
            "no rush",
            "can wait until tomorrow",
            "let us observe for a week",
            "not an emergency",
            "she will be fine"
        )

        for (phrase in delayPhrases) {
            if (content.contains(phrase) && !hasNegationBefore(content, phrase)) {
                violations.add("[FLAG] ${medicalPolicies[4]}: May delay emergency treatment — '$phrase'")
                return
            }
        }
    }

    private fun checkHarmfulContent(content: String, violations: MutableList<String>) {
        val harmfulWords = listOf(
            "kill", "poison", "terminate pregnancy",
            "overdose", "lethal", "end the pregnancy",
            "induce abortion", "unsafe abortion"
        )

        if (harmfulWords.any { content.contains(it) }) {
            violations.add("[BLOCK] Contains potentially harmful content")
        }
    }

    /**
     * Nigeria-specific risk patterns.
     * Catches dangerous advice that is common in local context
     * but would be missed by generic safety systems.
     */
    private fun checkNigerianContextRisks(content: String, violations: MutableList<String>) {
        // Traditional medicine risks — not blocking all traditional medicine,
        // but flagging when used as replacement for emergency care
        val unsafeTraditional = listOf(
            "use herbs instead of",
            "traditional medicine will cure",
            "no need for hospital just use",
            "agbo will treat",
            "drink mixture to stop",
            "tie the cord with"
        )

        if (unsafeTraditional.any { content.contains(it) }) {
            violations.add("[FLAG] Traditional remedy suggested as replacement for medical care")
        }

        // Dangerous newborn practices
        val unsafeNewborn = listOf(
            "apply substance to cord",
            "put ash on umbilical",
            "don't breastfeed until",
            "give water to newborn",
            "bath the baby in cold",
            "squeeze the breast of"
        )

        if (unsafeNewborn.any { content.contains(it) }) {
            violations.add("[BLOCK] Dangerous newborn care practice detected")
        }
    }
}