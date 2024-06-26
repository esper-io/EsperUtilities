package io.esper.android.tflite

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Log
import io.esper.android.files.app.application
import io.esper.android.files.util.Constants
import io.esper.android.files.util.activity
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.Rot90Op
import org.tensorflow.lite.task.core.BaseOptions
import org.tensorflow.lite.task.vision.detector.Detection
import org.tensorflow.lite.task.vision.detector.ObjectDetector
import java.io.File

class ObjectDetectorHelper(
    private var threshold: Float = 0.4f,
    private var numThreads: Int = 3,
    private var maxResults: Int = 4,
    val context: Context,
    val objectDetectorListener: DetectorListener?
) {

    private val TAG = "ObjectDetectorHelper"
    private var objectDetector: ObjectDetector? = null

    init {
        setupObjectDetector()
    }

    fun clearObjectDetector() {
        objectDetector = null
    }

    private fun setupObjectDetector() {
        val optionsBuilder = ObjectDetector.ObjectDetectorOptions.builder().setScoreThreshold(threshold)
            .setMaxResults(maxResults)
        val baseOptionsBuilder = BaseOptions.builder().setNumThreads(numThreads)
        if (CompatibilityList().isDelegateSupportedOnThisDevice) {
            baseOptionsBuilder.useGpu()
        } else {
            // If GPU is not supported, fall back to NNAPI
            try {
                baseOptionsBuilder.useNnapi()
            } catch (e: Exception) {
                // If NNAPI is not supported, fall back to CPU
                Log.e(TAG, "NNAPI not supported. Falling back to CPU.")
            }
        }
        optionsBuilder.setBaseOptions(baseOptionsBuilder.build())

        val sharedPreferences = context.getSharedPreferences(Constants.SHARED_MANAGED_CONFIG_VALUES, Context.MODE_PRIVATE)
        val modelPath = sharedPreferences.getString(Constants.SHARED_MANAGED_CONFIG_TFLITE_MODEL_PATH, null)
        Log.d(TAG, "Model path: $modelPath")
        if (modelPath.isNullOrEmpty() || modelPath.isBlank()) {
            objectDetectorListener?.onError("Tflite model not found. Please check the model path in the console.")
        }

        val modelFile = modelPath?.let { File(it) }
        if (modelFile == null || !modelFile.exists()) {
            objectDetectorListener?.onError("Tflite model not found. Please check the model path in the console.")
            application.activity?.finish()
        }
        try {
            objectDetector = ObjectDetector.createFromFileAndOptions(modelFile, optionsBuilder.build())
        } catch (e: IllegalStateException) {
            Log.e(TAG, "TFLite failed to load model with error: " + e.message)
        }
    }

    fun detect(image: Bitmap, imageRotation: Int) {
        if (objectDetector == null) {
            setupObjectDetector()
        }
        var inferenceTime = SystemClock.uptimeMillis()
        val imageProcessor = ImageProcessor.Builder().add(Rot90Op(-imageRotation / 90)).build()
        val tensorImage = imageProcessor.process(TensorImage.fromBitmap(image))
        val results = objectDetector?.detect(tensorImage)
        inferenceTime = SystemClock.uptimeMillis() - inferenceTime
        objectDetectorListener?.onResults(results, inferenceTime, tensorImage.height, tensorImage.width)
    }

    interface DetectorListener {
        fun onError(error: String)
        fun onResults(results: MutableList<Detection>?, inferenceTime: Long, imageHeight: Int, imageWidth: Int)
    }
}