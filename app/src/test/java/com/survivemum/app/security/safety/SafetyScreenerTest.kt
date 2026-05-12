package com.survivemum.app.security.safety

import com.survivemum.app.security.models.SafetyVerdict
import org.json.JSONObject
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Functional correctness tests for SafetyScreener.
 *
 * Loads safety_test_set.json and runs every case through the screener,
 * printing a per-category report and asserting overall accuracy.
 *
 * Context is null because the rule-based path is context-free. When ShieldGemma
 * is wired via LiteRT, those tests will run as instrumented tests instead.
 */
class SafetyScreenerTest {

    private lateinit var screener: SafetyScreener
    private lateinit var cases: List<TestCase>

    private data class TestCase(
        val id: String,
        val screen: String,       // "INPUT" or "OUTPUT"
        val expected: SafetyVerdict,
        val category: String,
        val text: String
    )

    private data class CaseResult(
        val case: TestCase,
        val actualVerdict: SafetyVerdict,
        val passed: Boolean,
        val reason: String?,
        val policiesViolated: List<String>
    )

    @Before
    fun setup() {
        screener = SafetyScreener()  // context defaults to null
        cases = loadTestSet()
    }

    @Test
    fun `screener handles full test set with at least 80 percent accuracy`() {
        val results = cases.map { runCase(it) }
        val passed = results.count { it.passed }
        val total = results.size
        val accuracy = passed.toDouble() / total

        printReport(results, passed, total, accuracy)

        assertTrue(
            "Accuracy was ${"%.1f".format(accuracy * 100)}% — below 80% threshold. See failures above.",
            accuracy >= 0.80
        )
    }

    @Test
    fun `legitimate clinical content always passes`() {
        val clinicalCases = cases.filter {
            it.category == "legitimate_clinical" || it.category == "negation_correct"
        }
        val failures = clinicalCases.map { runCase(it) }.filter { !it.passed }

        if (failures.isNotEmpty()) {
            println("=== FALSE POSITIVES (legitimate content blocked) ===")
            failures.forEach { printFailure(it) }
        }

        assertTrue(
            "${failures.size} legitimate clinical cases were incorrectly flagged or blocked. " +
                    "False positives are critical — they cause TBA alert fatigue.",
            failures.isEmpty()
        )
    }

    @Test
    fun `prompt injection attempts always blocked`() {
        val injectionCases = cases.filter { it.category == "prompt_injection" }
        val failures = injectionCases.map { runCase(it) }.filter { !it.passed }

        if (failures.isNotEmpty()) {
            println("=== INJECTION ATTEMPTS THAT SLIPPED THROUGH ===")
            failures.forEach { printFailure(it) }
        }

        assertTrue(
            "${failures.size} prompt injection attempts were not blocked.",
            failures.isEmpty()
        )
    }

    @Test
    fun `harmful content always blocked`() {
        val harmfulCases = cases.filter {
            it.category == "harmful_content" || it.category == "harmful_input"
        }
        val failures = harmfulCases.map { runCase(it) }.filter { !it.passed }

        if (failures.isNotEmpty()) {
            println("=== HARMFUL CONTENT THAT SLIPPED THROUGH ===")
            failures.forEach { printFailure(it) }
        }

        assertTrue(
            "${failures.size} harmful content cases were not blocked.",
            failures.isEmpty()
        )
    }

    // ----- Helpers -----

    private fun runCase(case: TestCase): CaseResult {
        val result = if (case.screen == "INPUT") {
            screener.screenInput(case.text)
        } else {
            screener.screenOutput(case.text)
        }
        return CaseResult(
            case = case,
            actualVerdict = result.verdict,
            passed = result.verdict == case.expected,
            reason = result.reason,
            policiesViolated = result.policiesViolated
        )
    }

    private fun printReport(
        results: List<CaseResult>,
        passed: Int,
        total: Int,
        accuracy: Double
    ) {
        println("=".repeat(70))
        println("SafetyScreener Test Report")
        println("=".repeat(70))
        println("Total cases:  $total")
        println("Passed:       $passed")
        println("Failed:       ${total - passed}")
        println("Accuracy:     ${"%.1f".format(accuracy * 100)}%")
        println()

        val byCategory = results.groupBy { it.case.category }
        println("Per-category accuracy:")
        byCategory.toSortedMap().forEach { (cat, list) ->
            val catPassed = list.count { it.passed }
            val catAcc = catPassed.toDouble() / list.size * 100
            println("  ${cat.padEnd(28)} $catPassed/${list.size}  (${"%.0f".format(catAcc)}%)")
        }
        println()

        val failures = results.filter { !it.passed }
        if (failures.isNotEmpty()) {
            println("Failures:")
            failures.forEach { printFailure(it) }
        }
        println("=".repeat(70))
    }

    private fun printFailure(r: CaseResult) {
        println("  [${r.case.id}] ${r.case.category}")
        println("    text:     \"${r.case.text}\"")
        println("    expected: ${r.case.expected}")
        println("    actual:   ${r.actualVerdict}")
        if (!r.reason.isNullOrEmpty()) {
            println("    reason:   ${r.reason}")
        }
        if (r.policiesViolated.isNotEmpty()) {
            println("    policies: ${r.policiesViolated.joinToString(", ")}")
        }
        println()
    }

    private fun loadTestSet(): List<TestCase> {
        val stream = javaClass.classLoader?.getResourceAsStream("safety_test_set.json")
            ?: error("safety_test_set.json not found on test classpath")

        val json = stream.bufferedReader().use { it.readText() }
        val root = JSONObject(json)
        val arr = root.getJSONArray("cases")

        return (0 until arr.length()).map { i ->
            val obj = arr.getJSONObject(i)
            TestCase(
                id = obj.getString("id"),
                screen = obj.getString("screen"),
                expected = SafetyVerdict.valueOf(obj.getString("expected")),
                category = obj.getString("category"),
                text = obj.getString("text")
            )
        }
    }
}