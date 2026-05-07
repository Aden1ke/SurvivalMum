package com.survivemum.app.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
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
        val baseOptionsBuilder = BaseOptions.builder()
            .setDelegate(com.google.mediapipe.tasks.core.Delegate.GPU)
        
        val optionsBuilder = FaceDetector.FaceDetectorOptions.builder()
            .setBaseOptions(baseOptionsBuilder.build())
            .setRunningMode(RunningMode.IMAGE) // Synchronous mode for rPPG alignment

        faceDetector = FaceDetector.createFromOptions(context, optionsBuilder.build())
    }

    fun detect(imageProxy: ImageProxy): Pair<FaceDetectorResult, Bitmap>? {
        val bitmap = imageProxyToBitmap(imageProxy)
        val mpImage = BitmapImageBuilder(bitmap).build()
        
        val result = faceDetector?.detect(mpImage)
        return if (result != null) {
            Pair(result, bitmap)
        } else {
            null
        }
    }

    private fun imageProxyToBitmap(image: ImageProxy): Bitmap {
        // Simple conversion - for a production app, use a more efficient YUV to RGB converter
        // CameraX 1.4+ provides ImageProxy.toBitmap() but we are using 1.4.1 which should have it
        // If not, we use the manual way.
        
        val bitmap = image.toBitmap()
        
        // Handle rotation if not handled by toBitmap()
        if (image.imageInfo.rotationDegrees != 0) {
            val matrix = Matrix()
            matrix.postRotate(image.imageInfo.rotationDegrees.toFloat())
            return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        }
        return bitmap
    }
}
