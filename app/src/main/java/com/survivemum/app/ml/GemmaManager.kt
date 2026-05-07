package com.survivemum.app.ml

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import java.io.File

/**
 * GemmaManager handles the offline Gemma 4 model.
 * Responsible for 128K context management and inference.
 */
class GemmaManager(private val context: Context) {

    private var llmInference: LlmInference? = null
    private val maxContextTokens = 128000 

    fun initializeModel(modelPath: String) {
        val options = LlmInference.LlmInferenceOptions.builder()
            .setModelPath(modelPath)
            .setMaxTokens(maxContextTokens)
            .build()

        llmInference = LlmInference.createFromOptions(context, options)
    }

    /**
     * Generates a response considering the long pregnancy context.
     * @param pregnancyHistory Structured clinical history (vitals, ANC card data, etc.)
     * @param currentQuery The current observation or TBA question
     */
    fun assess(pregnancyHistory: String, currentQuery: String): String {
        val systemPrompt = """
            You are SurviveMum AI, an expert maternal and neonatal health guardian.
            You have a 128K long context window allowing you to see the entire pregnancy history.
            
            PREGNANCY HISTORY:
            $pregnancyHistory
            
            INSTRUCTIONS:
            1. Analyze trends in blood pressure, weight, and fetal movement.
            2. Cross-reference observations with WHO guidelines.
            3. Provide clear clinical reasoning (Thinking Traces).
            4. Flag danger signs immediately.
        """.trimIndent()

        val fullPrompt = "$systemPrompt\n\nCURRENT OBSERVATION/QUESTION:\n$currentQuery\n\nASSESSMENT:"
        
        return try {
            llmInference?.generateResponse(fullPrompt) ?: "Model not initialized"
        } catch (e: Exception) {
            "Inference Error: ${e.message}"
        }
    }

    /**
     * Multimodal support (Interleaved).
     * Since on-device multimodal often uses specialized embedding or tag-based injection.
     */
    fun assessMultimodal(
        pregnancyHistory: String,
        ancCardData: String,
        vitals: String,
        images: List<File> // In a real scenario, these would be encoded or processed
    ): String {
        // Construct interleaved prompt representation
        val prompt = StringBuilder()
        prompt.append("HISTORY:\n$pregnancyHistory\n")
        prompt.append("LATEST VITALS:\n$vitals\n")
        prompt.append("ANC CARD DATA (OCR'd):\n$ancCardData\n")
        
        // In some multimodal LLMs, images are represented by special tokens
        images.forEachIndexed { index, _ ->
            prompt.append("[IMAGE_$index] ")
        }
        
        prompt.append("\nBased on the above history and the current visual/audio evidence, provide a health assessment.")
        
        return generateResponse(prompt.toString())
    }

    private fun generateResponse(prompt: String): String {
        return llmInference?.generateResponse(prompt) ?: "Model not initialized"
    }
}
