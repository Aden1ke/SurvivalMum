package com.survivemum.app.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facedetector.FaceDetector
import com.google.mediapipe.tasks.vision.facedetector.FaceDetectorResult

/**
 * Helper class for MediaPipe Face Detection.
 * Used to identify the ROI for rPPG.
 */
class FaceDetectorHelper(private val context: Context) {
    private var faceDetector: FaceDetector? = null

    init {
        setupFaceDetector()
    }

    private fun setupFaceDetector() {
        try {
            val baseOptionsBuilder = BaseOptions.builder()
                .setModelAssetPath("face_detection_short_range.tflite")
            
            val optionsBuilder = FaceDetector.FaceDetectorOptions.builder()
                .setBaseOptions(baseOptionsBuilder.build())
                .setRunningMode(RunningMode.IMAGE)

            faceDetector = FaceDetector.createFromOptions(context, optionsBuilder.build())
        } catch (e: Throwable) {
            Log.e("FaceDetectorHelper", "Critical: Failed to initialize FaceDetector native libs. This device may not be compatible with MediaPipe Vision.", e)
        }
    }

    fun detect(imageProxy: ImageProxy): Pair<FaceDetectorResult, Bitmap>? {
        if (faceDetector == null) return null
        
        return try {
            val bitmap = imageProxyToBitmap(imageProxy)
            val mpImage = BitmapImageBuilder(bitmap).build()
            
            val result = faceDetector?.detect(mpImage)
            if (result != null) {
                Pair(result, bitmap)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("FaceDetectorHelper", "Detection failed", e)
            null
        }
    }

    private fun imageProxyToBitmap(image: ImageProxy): Bitmap {
        val bitmap = image.toBitmap()
        if (image.imageInfo.rotationDegrees != 0) {
            val matrix = Matrix()
            matrix.postRotate(image.imageInfo.rotationDegrees.toFloat())
            return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        }
        return bitmap
    }
}
