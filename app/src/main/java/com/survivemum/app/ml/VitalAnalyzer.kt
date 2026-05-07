package com.survivemum.app.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Log
import androidx.camera.core.ImageProxy
import com.google.mediapipe.tasks.vision.facedetector.FaceDetectorResult
import kotlin.math.sqrt

/**
 * VitalAnalyzer extracts heart rate using rPPG with the CHROM algorithm.
 * Robust for dark skin tones by using chrominance differences.
 */
class VitalAnalyzer(
    context: Context,
    private val onVitalsDetected: (heartRate: Double, spo2: Double) -> Unit
) : androidx.camera.core.ImageAnalysis.Analyzer {

    private val faceDetectorHelper = try {
        FaceDetectorHelper(context)
    } catch (e: Throwable) {
        Log.e("VitalAnalyzer", "Native library error in FaceDetectorHelper", e)
        null
    }

    private val rSignal = mutableListOf<Double>()
    private val gSignal = mutableListOf<Double>()
    private val bSignal = mutableListOf<Double>()
    private val windowSize = 150 // ~5 seconds at 30fps

    override fun analyze(image: ImageProxy) {
        try {
            val resultPair = faceDetectorHelper?.detect(image)
            if (resultPair != null) {
                processFaceResult(resultPair.first, resultPair.second)
            }
        } catch (e: Throwable) {
            Log.e("VitalAnalyzer", "Analysis failed due to native or logic error", e)
        } finally {
            image.close()
        }
    }

    private fun processFaceResult(result: FaceDetectorResult, bitmap: Bitmap) {
        if (result.detections().isEmpty()) return

        val detection = result.detections()[0]
        val boundingBox = detection.boundingBox()

        val left = (boundingBox.left).toInt().coerceAtLeast(0)
        val top = (boundingBox.top).toInt().coerceAtLeast(0)
        val width = (boundingBox.width()).toInt().coerceAtMost(bitmap.width - left)
        val height = (boundingBox.height()).toInt().coerceAtMost(bitmap.height - top)

        val roi = Rect(
            (left + (width * 0.25)).toInt(),
            (top + (height * 0.05)).toInt(),
            (left + (width * 0.75)).toInt(),
            (top + (height * 0.25)).toInt()
        )

        if ((roi.width() > 0) && (roi.height() > 0)) {
            val (r, g, b) = extractAverageColor(bitmap, roi)
            rSignal.add(r)
            gSignal.add(g)
            bSignal.add(b)

            if (rSignal.size > windowSize) {
                rSignal.removeAt(0)
                gSignal.removeAt(0)
                bSignal.removeAt(0)
                computeVitals()
            }
        }
    }

    private fun extractAverageColor(bitmap: Bitmap, rect: Rect): Triple<Double, Double, Double> {
        var rSum = 0L
        var gSum = 0L
        var bSum = 0L
        val count = rect.width() * rect.height()

        val pixels = IntArray(count)
        bitmap.getPixels(pixels, 0, rect.width(), rect.left, rect.top, rect.width(), rect.height())

        for (pixel in pixels) {
            rSum += (pixel shr 16) and 0xFF
            gSum += (pixel shr 8) and 0xFF
            bSum += pixel and 0xFF
        }
        return Triple(rSum.toDouble() / count, gSum.toDouble() / count, bSum.toDouble() / count)
    }

    private fun computeVitals() {
        val nR = normalize(rSignal)
        val nG = normalize(gSignal)
        val nB = normalize(bSignal)

        val xSignals = mutableListOf<Double>()
        val ySignals = mutableListOf<Double>()

        for (i in nR.indices) {
            xSignals.add(3 * nR[i] - 2 * nG[i])
            ySignals.add(1.5 * nR[i] + nG[i] - 1.5 * nB[i])
        }

        val alpha = standardDeviation(xSignals) / standardDeviation(ySignals)
        val sSignals = xSignals.zip(ySignals).map { (x, y) -> x - alpha * y }

        val bpm = calculateBpm(sSignals)
        val spo2 = calculateSpo2(nR, nB)
        
        onVitalsDetected(bpm, spo2)
    }

    private fun normalize(signal: List<Double>): List<Double> {
        val avg = signal.average()
        return if (avg != 0.0) signal.map { it / avg } else signal
    }

    private fun standardDeviation(signal: List<Double>): Double {
        val avg = signal.average()
        return sqrt(signal.asSequence().map { (it - avg) * (it - avg) }.average()).coerceAtLeast(0.0001)
    }

    private fun calculateBpm(signal: List<Double>): Double {
        var peaks = 0
        val threshold = 0.001 
        for (i in 1 until signal.size - 1) {
            if (signal[i] > signal[i - 1] && signal[i] > signal[i + 1] && signal[i] > threshold) {
                peaks++
            }
        }
        val minutes = windowSize.toDouble() / (30 * 60)
        return if (minutes > 0) peaks / minutes else 0.0
    }

    private fun calculateSpo2(nR: List<Double>, nB: List<Double>): Double {
        val rAC = standardDeviation(nR)
        val rDC = nR.average()
        val bAC = standardDeviation(nB)
        val bDC = nB.average()
        
        val r = (rAC / rDC) / (bAC / bDC)
        return (110 - 15 * r).coerceIn(70.0, 100.0)
    }
}
