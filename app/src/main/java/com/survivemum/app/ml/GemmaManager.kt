package com.survivemum.app.ml

import android.content.Context
import android.util.Log
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import java.io.File
import java.io.FileOutputStream

/**
 * GemmaManager handles the offline Gemma 4 model.
 * Responsible for context management and inference.
 */
class GemmaManager(private val context: Context) {

    private var llmInference: LlmInference? = null
    private val maxContextTokens = 32768 

    fun initializeModel(modelName: String) {
        try {
            Log.d("GemmaManager", "Starting model initialization check for $modelName...")
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
