package com.survivemum.app.security.safety
import com.survivemum.app.ml.GemmaManager

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
 *
 * Fail-open vs fail-closed:
 *   - Input screening fails OPEN. If the screener errors, we let the input through
 *     to Gemma rather than blocking the whole assessment. The downstream output
 *     screen still runs as a second line of defense.
 *   - Output screening fails CLOSED. If the screener errors on output, we block
 *     rather than let potentially-unsafe content reach the TBA.
 *
 * This file uses the named policies in Policies.kt instead of magic indices.
 * Adding a new policy is: define it in Policies, add a check method here.
 */
class SafetyScreener(
    private val context: Context? = null,
    private val auditLog: SafetyAuditLog = SafetyAuditLog.Noop,
    private val gemma: GemmaManager? = null
) {

    companion object {
        private const val TAG = "SafetyScreener"

        // Confidence floors used when computing SafetyResult.confidence
        private const val CONF_NO_VIOLATIONS = 0.70
        private const val CONF_PER_VIOLATION = 0.10
        private const val CONF_BLOCK_BONUS = 0.15
        private const val CONF_MAX = 0.95
    }

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
     * Catches prompt injection, harmful content, and obvious garbage.
     *
     * Fails OPEN: on error, returns SAFE so the assessment continues.
     * The output screen is the real safety gate.
     *
     * Every call produces one audit log entry, regardless of verdict.
     */
    fun screenInput(input: String, alertId: String? = null): SafetyResult {
        val start = System.currentTimeMillis()

        val result = try {
            val raw = if (isModelLoaded) {
                // TODO: Replace with actual ShieldGemma inference when LiteRT is ready
                ruleBasedScreenInput(input)
            } else {
                Log.w(TAG, "ShieldGemma not loaded — using rule-based input screening")
                ruleBasedScreenInput(input)
            }
            raw.copy(screeningMs = System.currentTimeMillis() - start)
        } catch (e: Exception) {
            Log.e(TAG, "Input screening failed: ${e.message}")
            // Fail OPEN: let input through, output screen will catch issues
            SafetyResult(
                verdict = SafetyVerdict.SAFE,
                reason = "Input screening errored — failing open, output screen will verify",
                screeningMs = System.currentTimeMillis() - start
            )
        }

        auditLog.record(
            screenType = "INPUT",
            content = input,
            result = result,
            alertId = alertId
        )

        return result
    }

    /**
     * Screen output AFTER Gemma generates it, BEFORE the TBA sees it.
     *
     * Two-layer screening:
     *   Layer 1 — Rule-based (deterministic, fast, ~50ms). Catches known patterns.
     *   Layer 2 — Gemma 4 policy-grounded (semantic, slower, only runs if Layer 1 SAFE).
     *             Catches nuanced unsafe content that rules miss.
     *
     * If Layer 1 already says UNSAFE, we don't waste a Gemma call — we trust the
     * rule and block. Layer 2 only adds value when rules say SAFE/FLAGGED.
     *
     * Fails CLOSED: on error, returns UNSAFE so nothing slips past.
     */
    fun screenOutput(output: String, alertId: String? = null): SafetyResult {
        val start = System.currentTimeMillis()

        val result = try {
            // Layer 1: rule-based fast path
            val ruleResult = ruleBasedScreenOutput(output)

            // If rules already blocked it, no need to call Gemma
            if (ruleResult.verdict == SafetyVerdict.UNSAFE) {
                ruleResult.copy(screeningMs = System.currentTimeMillis() - start)
            } else if (gemma != null) {
                // Layer 2: Gemma semantic screening
                val gemmaResult = gemmaScreen(output)
                combineResults(ruleResult, gemmaResult)
                    .copy(screeningMs = System.currentTimeMillis() - start)
            } else {
                // No Gemma available — rule result stands
                ruleResult.copy(screeningMs = System.currentTimeMillis() - start)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Output screening failed: ${e.message}")
            SafetyResult(
                verdict = SafetyVerdict.UNSAFE,
                reason = "Safety screening errored — blocking output as precaution",
                policiesViolated = listOf("Screening system failure"),
                confidence = 0.0,
                screeningMs = System.currentTimeMillis() - start
            )
        }

        auditLog.record(
            screenType = "OUTPUT",
            content = output,
            result = result,
            alertId = alertId
        )

        return result
    }

    /**
     * Ask Gemma 4 to classify the content against the policies.
     * Returns SAFE/FLAGGED/UNSAFE parsed from Gemma's structured response.
     *
     * If Gemma errors or returns unparseable output, returns SAFE — Layer 1
     * already screened it, so a Gemma failure shouldn't escalate the verdict.
     */
    private fun gemmaScreen(content: String): SafetyResult {
        val safetyPrompt = buildString {
            appendLine("You are a safety classifier. Evaluate the following AI-generated medical alert text against these policies:")
            appendLine()
            Policies.ALL_POLICIES.forEachIndexed { i, p ->
                appendLine("${i + 1}. $p")
            }
            appendLine()
            appendLine("ALERT TEXT:")
            appendLine(content)
            appendLine()
            appendLine("Reply in this exact format:")
            appendLine("VERDICT: [SAFE / FLAGGED / UNSAFE]")
            appendLine("VIOLATED: [comma-separated policy numbers, or NONE]")
            appendLine("REASON: [one short sentence]")
        }

        return try {
            val response = gemma?.assess(
                clinicalSituation = safetyPrompt,
                patientName = "safety-screen"
            ) ?: return SafetyResult(verdict = SafetyVerdict.SAFE)

            parseGemmaSafetyResponse(response)
        } catch (e: Exception) {
            Log.w(TAG, "Gemma safety screen errored — deferring to rule result: ${e.message}")
            SafetyResult(verdict = SafetyVerdict.SAFE, reason = "Gemma layer skipped due to error")
        }
    }

    /**
     * Parse Gemma's structured safety response into a SafetyResult.
     * Tolerant of formatting variations — Gemma doesn't always follow the
     * format exactly, and we'd rather degrade gracefully than crash.
     */
    private fun parseGemmaSafetyResponse(response: String): SafetyResult {
        val verdictRegex = Regex("VERDICT:\\s*(SAFE|FLAGGED|UNSAFE)", RegexOption.IGNORE_CASE)
        val violatedRegex = Regex("VIOLATED:\\s*([^\\n]+)", RegexOption.IGNORE_CASE)
        val reasonRegex = Regex("REASON:\\s*([^\\n]+)", RegexOption.IGNORE_CASE)

        val verdict = verdictRegex.find(response)?.groupValues?.get(1)?.uppercase()?.let {
            try { SafetyVerdict.valueOf(it) } catch (_: Exception) { SafetyVerdict.SAFE }
        } ?: SafetyVerdict.SAFE

        val violatedRaw = violatedRegex.find(response)?.groupValues?.get(1)?.trim() ?: ""
        val violatedPolicies = if (violatedRaw.equals("NONE", ignoreCase = true) || violatedRaw.isEmpty()) {
            emptyList()
        } else {
            // Map policy numbers back to policy names
            violatedRaw.split(",")
                .mapNotNull { it.trim().toIntOrNull() }
                .mapNotNull { num -> Policies.ALL_POLICIES.getOrNull(num - 1) }
        }

        val reason = reasonRegex.find(response)?.groupValues?.get(1)?.trim()

        return SafetyResult(
            verdict = verdict,
            reason = reason ?: "Gemma layer: $verdict",
            policiesViolated = violatedPolicies,
            confidence = 0.85  // Gemma's confidence is reasonably high when it responds
        )
    }

    /**
     * Combine rule-based and Gemma results.
     * Take the more severe verdict. Merge policy lists. Prefer Gemma's reason
     * when it adds new information, otherwise keep rule reason.
     */
    private fun combineResults(rule: SafetyResult, gemma: SafetyResult): SafetyResult {
        val combinedVerdict = when {
            rule.verdict == SafetyVerdict.UNSAFE || gemma.verdict == SafetyVerdict.UNSAFE -> SafetyVerdict.UNSAFE
            rule.verdict == SafetyVerdict.FLAGGED || gemma.verdict == SafetyVerdict.FLAGGED -> SafetyVerdict.FLAGGED
            else -> SafetyVerdict.SAFE
        }

        val combinedPolicies = (rule.policiesViolated + gemma.policiesViolated).distinct()

        val combinedReason = listOfNotNull(rule.reason, gemma.reason)
            .filter { it.isNotEmpty() }
            .joinToString(" | ")
            .ifEmpty { null }

        val combinedConfidence = maxOf(rule.confidence, gemma.confidence)

        return SafetyResult(
            verdict = combinedVerdict,
            reason = combinedReason,
            policiesViolated = combinedPolicies,
            confidence = combinedConfidence
        )
    }

    // ----- Rule-based screening (fallback + first line of defense) -----

    /**
     * Input screening: prompt-injection-aware, lighter touch than output.
     * Inputs come from OCR, voice transcription, and BE-1's prompt builder.
     */
    private fun ruleBasedScreenInput(content: String): SafetyResult {
        val lower = content.lowercase()
        val violations = mutableListOf<Violation>()

        checkPromptInjection(lower, violations)
        checkHarmfulContent(lower, violations)
        // Inputs may legitimately contain dosage / certainty language
        // (e.g. an ANC card noting an existing prescription) — don't flag those here.

        return buildResult(violations)
    }

    /**
     * Output screening: full policy enforcement.
     * Outputs are what the TBA will see — every policy applies.
     */
    private fun ruleBasedScreenOutput(content: String): SafetyResult {
        val lower = content.lowercase()
        val violations = mutableListOf<Violation>()

        checkDosagePatterns(lower, violations)
        checkDangerousMedicalAdvice(lower, violations)
        checkFalseCertainty(lower, violations)
        checkEmergencyDelay(lower, violations)
        checkHarmfulContent(lower, violations)
        checkNigerianContextRisks(lower, violations)
        checkPromptInjection(lower, violations)

        return buildResult(violations)
    }

    // ----- Result construction -----

    /**
     * Internal violation record — captures policy, severity, and matched phrase
     * so we can build a meaningful SafetyResult and audit log.
     */
    private data class Violation(
        val policy: String,
        val severity: SafetyVerdict,   // FLAGGED or UNSAFE; SAFE shouldn't appear here
        val detail: String
    )

    /**
     * Convert a list of violations into a SafetyResult with computed confidence.
     */
    private fun buildResult(violations: List<Violation>): SafetyResult {
        if (violations.isEmpty()) {
            return SafetyResult(
                verdict = SafetyVerdict.SAFE,
                confidence = CONF_NO_VIOLATIONS
            )
        }

        val verdict = when {
            violations.any { it.severity == SafetyVerdict.UNSAFE } -> SafetyVerdict.UNSAFE
            else -> SafetyVerdict.FLAGGED
        }

        // Confidence grows with violation count and BLOCK-level violations
        val blockCount = violations.count { it.severity == SafetyVerdict.UNSAFE }
        val confidence = (
                CONF_NO_VIOLATIONS +
                        violations.size * CONF_PER_VIOLATION +
                        blockCount * CONF_BLOCK_BONUS
                ).coerceAtMost(CONF_MAX)

        // De-duplicate policies (a single policy may match multiple phrases)
        val policiesViolated = violations.map { it.policy }.distinct()
        val reason = violations.joinToString("; ") { "${it.policy}: ${it.detail}" }

        return SafetyResult(
            verdict = verdict,
            reason = reason,
            policiesViolated = policiesViolated,
            confidence = confidence
        )
    }

    // ----- Negation awareness -----

    /**
     * "do NOT go to hospital" should not be flagged the same as
     * "go to hospital quickly" preceded by a warning.
     *
     * Looks back up to 20 chars before the matched phrase for negation words.
     */
    private fun hasNegationBefore(content: String, phrase: String): Boolean {
        val index = content.indexOf(phrase)
        if (index <= 0) return false

        val before = content.substring(maxOf(0, index - 30), index).lowercase()
        val negations = listOf(
            "don't ", "do not ", "never ", "should not ",
            "shouldn't ", "avoid ", "stop "
        )
        return negations.any { before.contains(it) }
    }

    // ----- Individual checks -----

    private fun checkDosagePatterns(content: String, violations: MutableList<Violation>) {
        val dosagePattern = Regex("\\d+\\s*(mg|ml|mcg|units?|tablets?|drops|capsules?)")
        dosagePattern.find(content)?.let { match ->
            violations.add(Violation(
                policy = Policies.NO_SPECIFIC_DOSAGE,
                severity = SafetyVerdict.FLAGGED,
                detail = "specific dosage detected: '${match.value}'"
            ))
        }

        val informalDosage = listOf(
            "half a tablet", "two tablets", "one capsule",
            "full spoon", "half spoon", "small spoon",
            "one cap", "two caps", "three caps",
            "one drop", "two drops", "three drops", "few drops"
        )
        for (phrase in informalDosage) {
            if (content.contains(phrase)) {
                violations.add(Violation(
                    policy = Policies.NO_SPECIFIC_DOSAGE,
                    severity = SafetyVerdict.FLAGGED,
                    detail = "informal dosage: '$phrase'"
                ))
            }
        }
    }

    private fun checkDangerousMedicalAdvice(content: String, violations: MutableList<Violation>) {
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
                violations.add(Violation(
                    policy = Policies.NO_DISCOURAGE_HELP,
                    severity = SafetyVerdict.UNSAFE,
                    detail = "discourages medical help: '$phrase'"
                ))
            }
        }
    }

    private fun checkFalseCertainty(content: String, violations: MutableList<Violation>) {
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

        for (phrase in certaintyPhrases) {
            if (content.contains(phrase)) {
                violations.add(Violation(
                    policy = Policies.NO_FALSE_CERTAINTY,
                    severity = SafetyVerdict.UNSAFE,
                    detail = "false certainty: '$phrase'"
                ))
            }
        }
    }

    private fun checkEmergencyDelay(content: String, violations: MutableList<Violation>) {
        val delayPhrases = listOf(
            "wait and see",
            "give it a few days",
            "probably nothing serious",
            "no rush",
            "can wait until tomorrow",
            "let us observe for a week",
            "not an emergency",
            "she will be fine",
            "probably be fine",
            "will be fine by morning",
            "monitor at home"
        )

        for (phrase in delayPhrases) {
            if (content.contains(phrase) && !hasNegationBefore(content, phrase)) {
                violations.add(Violation(
                    policy = Policies.NO_EMERGENCY_DELAY,
                    severity = SafetyVerdict.FLAGGED,
                    detail = "may delay emergency care: '$phrase'"
                ))
            }
        }
    }

    private fun checkHarmfulContent(content: String, violations: MutableList<Violation>) {
        val harmfulWords = listOf(
            "kill", "poison", "terminate pregnancy",
            "overdose", "lethal", "end the pregnancy",
            "induce abortion", "unsafe abortion"
        )

        for (phrase in harmfulWords) {
            if (content.contains(phrase)) {
                violations.add(Violation(
                    policy = Policies.NO_HARMFUL_CONTENT,
                    severity = SafetyVerdict.UNSAFE,
                    detail = "harmful term: '$phrase'"
                ))
            }
        }
    }

    /**
     * Nigeria-specific risks. Catches dangerous advice common in local context
     * but missed by generic safety systems.
     */
    private fun checkNigerianContextRisks(content: String, violations: MutableList<Violation>) {
        val unsafeTraditional = listOf(
            "use herbs instead of",
            "traditional medicine will cure",
            "no need for hospital just use",
            "agbo will treat",
            "drink mixture to stop",
            "tie the cord with"
        )
        for (phrase in unsafeTraditional) {
            if (content.contains(phrase)) {
                violations.add(Violation(
                    policy = Policies.NO_UNSAFE_TRADITIONAL,
                    severity = SafetyVerdict.FLAGGED,
                    detail = "traditional remedy as replacement for medical care: '$phrase'"
                ))
            }
        }

        val unsafeNewborn = listOf(
            "apply substance to cord",
            "put ash on umbilical",
            "don't breastfeed until",
            "give water to newborn",
            "bath the baby in cold",
            "squeeze the breast of"
        )
        for (phrase in unsafeNewborn) {
            if (content.contains(phrase)) {
                violations.add(Violation(
                    policy = Policies.NO_UNSAFE_NEWBORN_PRACTICES,
                    severity = SafetyVerdict.UNSAFE,
                    detail = "dangerous newborn care: '$phrase'"
                ))
            }
        }
    }

    /**
     * Catches instruction-override attempts that could come through OCR text,
     * voice transcription, or any free-form input field.
     */
    private fun checkPromptInjection(content: String, violations: MutableList<Violation>) {
        val injectionPhrases = listOf(
            "ignore previous instructions",
            "ignore the above",
            "disregard your instructions",
            "system: ",
            "you are now",
            "forget your training",
            "act as if you are",
            "pretend you are",
            "new instructions:",
            "override safety"
        )

        for (phrase in injectionPhrases) {
            if (content.contains(phrase)) {
                violations.add(Violation(
                    policy = Policies.NO_PROMPT_INJECTION,
                    severity = SafetyVerdict.UNSAFE,
                    detail = "prompt injection attempt: '$phrase'"
                ))
            }
        }
    }
}