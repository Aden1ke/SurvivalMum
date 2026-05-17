package com.survivemum.app.ml

import android.content.Context
import android.util.Log
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import java.io.File
import java.io.FileOutputStream

/**
 * GemmaManager handles the offline Gemma 4 model.
 * Responsible for context management and inference.
 * RAG is prepended to every clinical prompt to stop hallucination.
 */
class GemmaManager(private val context: Context) {

    private var llmInference: LlmInference? = null
    private val maxContextTokens = 32768 

    fun initializeModel(modelName: String) {
        try {
            Log.d("GemmaManager", "Starting model initialization check for $modelName...")
            val localModelPath = getLocalModelPath(modelName)
            
    private val ragEngine = RagEngine(context)
    private val maxContextTokens = 32768

    fun initializeModel(modelName: String) {
        // Initialize RAG first — it's fast
        ragEngine.initialize()

        try {
            Log.d("GemmaManager", "Starting model initialization for $modelName...")
            val localModelPath = getLocalModelPath(modelName)

            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(localModelPath)
                .setMaxTokens(maxContextTokens)
                .build()

            llmInference = LlmInference.createFromOptions(context, options)
            Log.d("GemmaManager", "Gemma 4 successfully loaded into memory.")
        } catch (e: Throwable) {
            Log.e("GemmaManager", "CRITICAL ERROR: Failed to load Gemma 4 engine. This usually means the device architecture is unsupported or the 1.3GB model file is corrupted.", e)
        }
    }

    /**
     * Ensures the model is available on the local filesystem.
     * MediaPipe LLM Inference often requires an absolute path rather than an asset URI.
     */
    private fun getLocalModelPath(modelName: String): String {
        val file = File(context.filesDir, modelName)
        if (!file.exists()) {
            Log.d("GemmaManager", "First-time setup: Copying 1.3GB model to internal storage. DO NOT CLOSE THE APP.")
            Log.d("GemmaManager", "✅ Gemma 4 successfully loaded into memory.")

        } catch (e: Throwable) {
            Log.e("GemmaManager", "CRITICAL ERROR: Failed to load Gemma 4. " +
                    "Check model file exists in assets and device is compatible.", e)
        }
    }

    private fun getLocalModelPath(modelName: String): String {
        val file = File(context.filesDir, modelName)
        if (!file.exists()) {
            Log.d("GemmaManager", "First-time setup: Copying model to internal storage. DO NOT CLOSE THE APP.")
            context.assets.open(modelName).use { inputStream ->
                FileOutputStream(file).use { outputStream ->
                    val buffer = ByteArray(8 * 1024)
                    var bytesRead: Int
                    var totalCopied = 0L
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        totalCopied += bytesRead
                        // Log every 100MB
                        if (totalCopied % (100 * 1024 * 1024) < buffer.size) {
                            Log.d("GemmaManager", "Copy progress: ${totalCopied / (1024 * 1024)} MB copied...")
                        }
                    }
                }
            }
            Log.d("GemmaManager", "Model storage copy complete.")
                        if (totalCopied % (100 * 1024 * 1024) < buffer.size) {
                            Log.d("GemmaManager", "Copy progress: ${totalCopied / (1024 * 1024)} MB...")
                        }
                    }
                }
            }
            Log.d("GemmaManager", "✅ Model copy complete.")
        }
        return file.absolutePath
    }

    fun assess(pregnancyHistory: String, currentQuery: String): String {
        val systemPrompt = """
            You are SurviveMum AI, an expert maternal health guardian.
            
            PREGNANCY HISTORY:
            $pregnancyHistory
            
            INSTRUCTIONS:
            1. Analyze trends.
            2. Provide Thinking Traces.
            3. Flag danger signs immediately.
        """.trimIndent()

        val fullPrompt = "$systemPrompt\n\nCURRENT OBSERVATION:\n$currentQuery\n\nASSESSMENT:"
        
        return try {
            llmInference?.generateResponse(fullPrompt) ?: "AI Model is loading or failed to initialize."
        } catch (e: Exception) {
            "Inference Error: ${e.message}"
        }
    }
}
    /**
     * Main clinical assessment function.
     * RAG retrieves relevant WHO guideline FIRST.
     * Gemma reasons ONLY from those guidelines.
     */
    fun assess(clinicalSituation: String, patientName: String = "the patient"): String {
        // Step 1 — RAG retrieves the most relevant guideline
        val retrievedGuideline = if (ragEngine.isReady()) {
            ragEngine.retrieve(clinicalSituation)
        } else {
            "WHO Emergency Guidelines: Refer immediately for BP ≥140/90, heavy bleeding, or convulsions."
        }

        // Step 2 — Build the grounded prompt
        val prompt = buildPrompt(
            clinicalSituation = clinicalSituation,
            guideline = retrievedGuideline,
            patientName = patientName
        )

        // Step 3 — Gemma inference
        return try {
            llmInference?.generateResponse(prompt)
                ?: "AI Model is loading. Please wait."
        } catch (e: Exception) {
            Log.e("GemmaManager", "Inference error: ${e.message}")
            "Inference Error: ${e.message}"
        }
    }

    private fun buildPrompt(
        clinicalSituation: String,
        guideline: String,
        patientName: String
    ): String {
        return """
You are SurviveMum, a clinical AI guardian for patients who cannot speak.
You monitor mothers and newborns in rural Nigeria where no doctor is present.

RETRIEVED CLINICAL GUIDELINE:
$guideline

CLINICAL SITUATION FOR ${patientName.uppercase()}:
$clinicalSituation

Using ONLY the guideline above, show your reasoning inside <thinking> tags.
Then provide your assessment in this exact format:

<thinking>
[Step by step clinical reasoning]
[Which guideline threshold is matched]
[What indicators are present]
</thinking>

RISK_LEVEL: [CRITICAL / HIGH / MEDIUM / LOW]
ALERT_TYPE: [specific condition detected]
GUIDELINE_CITED: [source of guideline used]
ACTION: [specific instruction for the TBA in plain language]
        """.trimIndent()
    }

    /**
     * Cry classification assessment.
     * Separate prompt optimised for newborn cry analysis.
     */
    fun assessCry(pitchHz: Float, burstDurationSeconds: Float): String {
        val situation = "Newborn cry analysis. Pitch: ${pitchHz}Hz. " +
                "Burst duration: ${burstDurationSeconds}s."

        return assess(
            clinicalSituation = situation,
            patientName = "newborn"
        )
    }
}
