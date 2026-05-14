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

    private val db           = SurviveMumDatabase.getDatabase(application)
    private val gemmaManager = GemmaManager(application)

    private val _uiState = MutableStateFlow(NewbornUIState())
    val uiState: StateFlow<NewbornUIState> = _uiState.asStateFlow()

    private val _isEngineReady = MutableStateFlow(false)
    val isEngineReady: StateFlow<Boolean> = _isEngineReady.asStateFlow()

    private val sampleRate    = 16000
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat   = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize    = AudioRecord.getMinBufferSize(
        sampleRate, channelConfig, audioFormat
    ).coerceAtLeast(sampleRate * 2)

    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job?        = null
    private val silenceThresholdDb        = 40.0

    init {
        viewModelScope.launch(Dispatchers.IO) {
            gemmaManager.initializeModel("gemma4.bin")
            _isEngineReady.value = true
            Log.d("NewbornVM", "Engine ready")
            startAudioMonitoring()
        }
    }

    // ── Audio monitoring loop ─────────────────────────────────────────────────

    private fun startAudioMonitoring() {
        recordingJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate, channelConfig, audioFormat, bufferSize
                )

                if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                    Log.e("NewbornVM", "AudioRecord failed — check RECORD_AUDIO permission")
                    return@launch
                }

                audioRecord?.startRecording()
                Log.d("NewbornVM", "Audio monitoring started")

                val buffer        = ShortArray(bufferSize / 2)
                val windowSamples = mutableListOf<Short>()
                val windowSize    = sampleRate * 2 // 2 seconds

                while (isActive) {
                    val read = audioRecord?.read(buffer, 0, buffer.size) ?: break
                    if (read > 0) {
                        windowSamples.addAll(buffer.take(read).toList())
                        updateWaveform(buffer.take(read))
                        if (windowSamples.size >= windowSize) {
                            analyseAudioWindow(windowSamples.toShortArray())
                            windowSamples.clear()
                        }
                    }
                }
            } catch (e: SecurityException) {
                Log.e("NewbornVM", "RECORD_AUDIO permission not granted", e)
            } catch (e: Exception) {
                Log.e("NewbornVM", "Audio error", e)
            } finally {
                audioRecord?.stop()
                audioRecord?.release()
                audioRecord = null
            }
        }
    }

    // ── Analyse 2-second window ───────────────────────────────────────────────

    private suspend fun analyseAudioWindow(samples: ShortArray) {
        val rms           = calculateRMS(samples)
        val dbLevel       = if (rms > 0) 20 * log10(rms) else 0.0
        val isCry         = dbLevel > silenceThresholdDb
        val breathingRate = estimateBreathingRate(samples)

        if (!isCry) {
            _uiState.value = _uiState.value.copy(
                cryStatus          = "No cry detected",
                breathingRate      = breathingRate,
                isDistressDetected = false,
                confidence         = 90,
                reasoningSteps     = listOf(
                    ReasoningStep(1, "Ambient sound level normal"),
                    ReasoningStep(2, "No cry pattern — baby appears settled")
                )
            )
            return
        }

        val (cryType, confidence, isDistress) = classifyCry(samples, dbLevel)

        _uiState.value = _uiState.value.copy(
            cryStatus          = cryType,
            breathingRate      = breathingRate,
            isDistressDetected = isDistress,
            confidence         = confidence
        )

        persistCryEvent(cryType, confidence, dbLevel, isDistress)

        if (isDistress) {
            runGemmaAssessment(cryType, confidence, breathingRate, dbLevel)
        } else {
            _uiState.value = _uiState.value.copy(
                reasoningSteps = listOf(
                    ReasoningStep(1, "Cry pattern detected"),
                    ReasoningStep(2, "Matched: $cryType"),
                    ReasoningStep(3, "Confidence: $confidence% — monitoring continues")
                )
            )
        }
    }

    // ── Signal processing ─────────────────────────────────────────────────────

    private fun calculateRMS(samples: ShortArray): Double {
        if (samples.isEmpty()) return 0.0
        val sumSq = samples.sumOf { it.toDouble() * it.toDouble() }
        return sqrt(sumSq / samples.size)
    }

    private fun estimateBreathingRate(samples: ShortArray): Int {
        var crossings = 0
        for (i in 1 until samples.size) {
            if ((samples[i] >= 0) != (samples[i - 1] >= 0)) crossings++
        }
        return (crossings / 2.0 / 2.0).toInt().coerceIn(0, 80)
    }

    private fun classifyCry(samples: ShortArray, dbLevel: Double): Triple<String, Int, Boolean> {
        val avgAmp     = samples.map { abs(it.toInt()) }.average()
        val peakAmp    = samples.maxOfOrNull { abs(it.toInt()) }?.toDouble() ?: 0.0
        val peakToAvg  = if (avgAmp > 0) peakAmp / avgAmp else 1.0

        var peaks = 0
        val threshold = avgAmp * 1.5
        for (i in 1 until samples.size - 1) {
            val curr = abs(samples[i].toInt()).toDouble()
            if (curr > threshold &&
                curr > abs(samples[i - 1].toInt()).toDouble() &&
                curr > abs(samples[i + 1].toInt()).toDouble()
            ) peaks++
        }

        return when {
            dbLevel > 75 && peakToAvg > 4.0 -> Triple("High-pitched distress cry", 91, true)
            dbLevel > 70 && peakToAvg > 3.0 -> Triple("Pain cry detected", 88, true)
            peaks > 20 && peakToAvg < 2.5   -> Triple("Hunger cry", 82, false)
            dbLevel in 45.0..65.0           -> Triple("Discomfort — needs attention", 75, false)
            else                            -> Triple("Cry detected — classifying", 60, false)
        }
    }

    private fun updateWaveform(samples: List<Short>) {
        val step   = (samples.size / 20).coerceAtLeast(1)
        val points = (0 until 20).map { i ->
            val idx = (i * step).coerceAtMost(samples.size - 1)
            (abs(samples[idx].toInt()) / 32768f).coerceIn(0f, 1f)
        }
        viewModelScope.launch(Dispatchers.Main) {
            _uiState.value = _uiState.value.copy(waveformData = points)
        }
    }

    // ── Persist using CORRECT CryEventEntity field names ─────────────────────
    // CryEventEntity fields:
    //   @PrimaryKey classificationId: String   ← NOT eventId
    //   layer: String                          ← required, "NEWBORN"
    //   clinicalFlag: Int                      ← NOT isDistress
    //   confidence: Float                      ← NOT Int
    //   fullJson: String                       ← required

    private suspend fun persistCryEvent(
        cryType   : String,
        confidence: Int,
        dbLevel   : Double,
        isDistress: Boolean
    ) {
        try {
            val user    = db.userDao().getCurrentUser() ?: return
            val patient = db.patientDao()
                .getAllPatients(user.userId)
                .first()
                .firstOrNull() ?: return

            val clinicalConcern = if (isDistress) "Distress cry — immediate attention required" else null

            val fullJson = JSONObject().apply {
                put("cryType",        cryType)
                put("confidence",     confidence)
                put("amplitudeDb",    dbLevel)
                put("isDistress",     isDistress)
                put("clinicalConcern", clinicalConcern ?: "none")
                put("timestamp",      System.currentTimeMillis())
            }.toString()

            db.cryEventDao().insertCryEvent(
                CryEventEntity(
                    classificationId = UUID.randomUUID().toString(), // ← correct PK name
                    patientId        = patient.patientId,
                    timestamp        = System.currentTimeMillis().toString(),
                    layer            = "NEWBORN",                    // ← required field
                    cryType          = cryType,
                    clinicalFlag     = if (isDistress) 1 else 0,    // ← NOT isDistress
                    clinicalConcern  = clinicalConcern,
                    confidence       = confidence.toFloat(),          // ← Float, not Int
                    audioDurationSec = 2.0f,
                    triggeredAlert   = if (isDistress) 1 else 0,
                    fullJson         = fullJson                       // ← required field
                )
            )
        } catch (e: Exception) {
            Log.e("NewbornVM", "Failed to persist cry event", e)
        }
    }

    // ── Gemma assessment ──────────────────────────────────────────────────────

    private suspend fun runGemmaAssessment(
        cryType      : String,
        confidence   : Int,
        breathingRate: Int,
        dbLevel      : Double
    ) {
        try {
            val user    = db.userDao().getCurrentUser()
            val patient = user?.let {
                db.patientDao().getAllPatients(it.userId).first().firstOrNull()
            }

            val context = patient?.let {
                "Newborn of ${it.fullName}. Mother was ${it.weeksPregnant} weeks at delivery."
            } ?: "Newborn — patient details unavailable."

            val response = gemmaManager.assess(
                pregnancyHistory = context,
                currentQuery = """
                    Newborn cry analysis:
                    Cry type: $cryType
                    Confidence: $confidence%
                    Sound level: ${"%.1f".format(dbLevel)} dB
                    Breathing rate: $breathingRate breaths/min
                    This is a distress cry. Give 3 immediate actions for the birth attendant.
                """.trimIndent()
            )

            val steps = response
                .split("\n")
                .filter { it.isNotBlank() }
                .take(4)
                .mapIndexed { i, line ->
                    ReasoningStep(i + 1, line.trim().removePrefix("${i + 1}. "))
                }

            _uiState.value = _uiState.value.copy(
                reasoningSteps = steps.ifEmpty {
                    listOf(ReasoningStep(1, response.take(150)))
                }
            )
        } catch (e: Exception) {
            Log.e("NewbornVM", "Gemma assessment failed", e)
        }
    }

    override fun onCleared() {
        super.onCleared()
        recordingJob?.cancel()
        audioRecord?.stop()
        audioRecord?.release()
    }
}