package com.survivemum.app.viewmodel

import android.app.Application
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.survivemum.app.data.CryEventEntity
import com.survivemum.app.data.SurviveMumDatabase
import com.survivemum.app.ml.GemmaManager
import com.survivemum.app.ui.components.ReasoningStep
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.UUID
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.sqrt

data class NewbornUIState(
    val breathingRate: Int = 0,
    val cryStatus: String = "Monitoring...",
    val jaundiceRisk: Double = 0.0,
    val isDistressDetected: Boolean = false,
    val confidence: Int = 0,
    val reasoningSteps: List<ReasoningStep> = emptyList(),
    val waveformData: List<Float> = emptyList()
)

class NewbornViewModel(application: Application) : AndroidViewModel(application) {

    private val db = SurviveMumDatabase.getDatabase(application)
    private val gemmaManager = GemmaManager(application)

    private val _uiState = MutableStateFlow(NewbornUIState())
    val uiState: StateFlow<NewbornUIState> = _uiState.asStateFlow()

    private val _isEngineReady = MutableStateFlow(false)
    val isEngineReady: StateFlow<Boolean> = _isEngineReady.asStateFlow()

    // Audio Config
    private val sampleRate = 16000
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioRecord.getMinBufferSize(
        sampleRate, channelConfig, audioFormat
    ).coerceAtLeast(sampleRate * 2)

    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private val silenceThresholdDb = 40.0

    init {
        initializeEngine()
    }

    private fun initializeEngine() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Ensure gemma4.bin is in app/src/main/assets/
                gemmaManager.initializeModel("gemma4.bin")
                _isEngineReady.value = true
                Log.d("NewbornVM", "AI Engine Ready")
                startAudioMonitoring()
            } catch (e: Exception) {
                Log.e("NewbornVM", "AI Engine Init Failed: ${e.message}")
                _uiState.value = _uiState.value.copy(cryStatus = "AI Engine Offline")
            }
        }
    }

    private fun startAudioMonitoring() {
        recordingJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate, channelConfig, audioFormat, bufferSize
                )

                if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                    Log.e("NewbornVM", "AudioRecord initialization failed")
                    return@launch
                }

                audioRecord?.startRecording()

                val buffer = ShortArray(bufferSize / 2)
                val windowSize = sampleRate * 2 // 2-second window
                val windowBuffer = ShortArray(windowSize)
                var windowIdx = 0

                while (isActive) {
                    val read = audioRecord?.read(buffer, 0, buffer.size) ?: break
                    if (read > 0) {
                        // Update UI waveform efficiently
                        updateWaveform(buffer.take(read))

                        // Fill the processing window
                        for (i in 0 until read) {
                            if (windowIdx < windowSize) {
                                windowBuffer[windowIdx] = buffer[i]
                                windowIdx++
                            }
                        }

                        // When window is full, analyze and reset
                        if (windowIdx >= windowSize) {
                            analyseAudioWindow(windowBuffer.copyOf())
                            windowIdx = 0
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("NewbornVM", "Recording Error", e)
            } finally {
                cleanupAudio()
            }
        }
    }

    private suspend fun analyseAudioWindow(samples: ShortArray) {
        val rms = calculateRMS(samples)
        val dbLevel = if (rms > 0) 20 * log10(rms) else 0.0
        val isCry = dbLevel > silenceThresholdDb
        val breathingRate = estimateBreathingRate(samples)

        if (!isCry) {
            _uiState.value = _uiState.value.copy(
                cryStatus = "Quiet Monitoring",
                breathingRate = breathingRate,
                isDistressDetected = false,
                reasoningSteps = listOf(ReasoningStep(1, "Ambient levels normal"))
            )
            return
        }

        val (cryType, confidence, isDistress) = classifyCry(samples, dbLevel)

        _uiState.value = _uiState.value.copy(
            cryStatus = cryType,
            breathingRate = breathingRate,
            isDistressDetected = isDistress,
            confidence = confidence
        )

        persistCryEvent(cryType, confidence, dbLevel, isDistress)

        if (isDistress) {
            runGemmaAssessment(cryType, confidence, breathingRate, dbLevel)
        }
    }

    private fun calculateRMS(samples: ShortArray): Double {
        val sumSq = samples.fold(0.0) { acc, s -> acc + (s.toDouble() * s.toDouble()) }
        return sqrt(sumSq / samples.size)
    }

    private fun estimateBreathingRate(samples: ShortArray): Int {
        var crossings = 0
        for (i in 1 until samples.size) {
            if ((samples[i] >= 0) != (samples[i - 1] >= 0)) crossings++
        }
        return (crossings / 4).coerceIn(12, 60) // Normal newborn range
    }

    private fun classifyCry(samples: ShortArray, dbLevel: Double): Triple<String, Int, Boolean> {
        val peakAmp = samples.maxOf { abs(it.toInt()) }.toDouble()

        return when {
            dbLevel > 75 && peakAmp > 25000 -> Triple("High Distress / Pain", 92, true)
            dbLevel > 65 -> Triple("Hunger / Discomfort", 85, false)
            else -> Triple("Fussing", 70, false)
        }
    }

    private fun updateWaveform(samples: List<Short>) {
        viewModelScope.launch(Dispatchers.Main) {
            val points = samples.chunked(samples.size / 20)
                .map { chunk -> (chunk.maxOfOrNull { abs(it.toInt()) } ?: 0) / 32768f }
            _uiState.value = _uiState.value.copy(waveformData = points)
        }
    }

    private suspend fun persistCryEvent(cryType: String, confidence: Int, dbLevel: Double, isDistress: Boolean) {
        try {
            val user = db.userDao().getCurrentUser() ?: return
            val patient = db.patientDao().getAllPatients(user.userId).first().firstOrNull() ?: return

            val event = CryEventEntity(
                classificationId = UUID.randomUUID().toString(),
                patientId = patient.patientId,
                timestamp = System.currentTimeMillis().toString(),
                layer = "NEWBORN",
                cryType = cryType,
                clinicalFlag = if (isDistress) 1 else 0,
                confidence = confidence.toFloat(),
                fullJson = JSONObject().apply {
                    put("db", dbLevel)
                    put("type", cryType)
                }.toString()
            )
            db.cryEventDao().insertCryEvent(event)
        } catch (e: Exception) {
            Log.e("NewbornVM", "DB Insert Failed", e)
        }
    }

    private suspend fun runGemmaAssessment(cryType: String, conf: Int, breath: Int, db: Double) {
        val prompt = "Newborn cry: $cryType, Confidence: $conf%, Breath: $breath. Immediate actions?"
        val response = gemmaManager.assess("Newborn Care", prompt)

        val steps = response.split("\n")
            .filter { it.isNotBlank() }
            .take(3)
            .mapIndexed { i, line -> ReasoningStep(i + 1, line.trim()) }

        _uiState.value = _uiState.value.copy(reasoningSteps = steps)
    }

    private fun cleanupAudio() {
        audioRecord?.apply {
            if (state == AudioRecord.STATE_INITIALIZED) {
                stop()
            }
            release()
        }
        audioRecord = null
    }

    override fun onCleared() {
        super.onCleared()
        recordingJob?.cancel()
        cleanupAudio()
    }
}