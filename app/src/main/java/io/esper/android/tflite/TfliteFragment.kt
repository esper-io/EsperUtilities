package io.esper.android.tflite

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.hbisoft.hbrecorder.Constants.MAX_FILE_SIZE_REACHED_ERROR
import com.hbisoft.hbrecorder.Constants.SETTINGS_ERROR
import com.hbisoft.hbrecorder.HBRecorder
import com.hbisoft.hbrecorder.HBRecorderListener
import com.shasin.notificationbanner.Banner
import io.esper.android.files.app.application
import io.esper.android.files.databinding.FragmentCameraBinding
import io.esper.android.files.util.Constants
import io.esper.android.files.util.GeneralUtils
import io.esper.android.files.util.UploadDownloadUtils
import org.tensorflow.lite.task.vision.detector.Detection
import java.io.File
import java.util.LinkedList
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class TfliteFragment : Fragment(), ObjectDetectorHelper.DetectorListener, HBRecorderListener {

    private val TAG = "TfliteFragment"
    private var callback: ScreenRecordingCallback? = null
    private lateinit var fragmentCameraBinding: FragmentCameraBinding
    private lateinit var objectDetectorHelper: ObjectDetectorHelper
    private lateinit var bitmapBuffer: Bitmap
    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var currentCameraSelector: CameraSelector? = null
    private var hbRecorder: HBRecorder? = null
    private val SCREEN_RECORD_REQUEST_CODE = 777

    /** Blocking camera operations are performed using this executor */
    private lateinit var cameraExecutor: ExecutorService

    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor.shutdown()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return try {
            fragmentCameraBinding = FragmentCameraBinding.inflate(inflater, container, false)
            fragmentCameraBinding.root
        } catch (e: Exception) {
            Log.e(TAG, "Error inflating view", e)
            context?.let { GeneralUtils.triggerRebirth(it) }
            null
        }
    }

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        try {
            context?.let { context ->
                try {
                    objectDetectorHelper = ObjectDetectorHelper(
                        context = context, objectDetectorListener = this
                    )

                    // Initialize our background executor
                    cameraExecutor = Executors.newSingleThreadExecutor()

                    // Wait for the views to be properly laid out
                    fragmentCameraBinding.viewFinder.post {
                        // Set up the camera and its use cases
                        setUpCamera(context)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error setting up camera", e)
                    GeneralUtils.triggerRebirth(context)
                }

                updateUi()
                hbRecorder = HBRecorder(context, this)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing camera", e)
            context?.let { GeneralUtils.triggerRebirth(it) }
        }
    }

    private fun updateUi() {
        with(fragmentCameraBinding) {
            val sharedPrefManaged = requireContext().getSharedPreferences(
                Constants.SHARED_MANAGED_CONFIG_VALUES, Context.MODE_PRIVATE
            )
            val file =
                sharedPrefManaged.getString(Constants.SHARED_MANAGED_CONFIG_TFLITE_MODEL_PATH, null)
                    ?.let { File(it) }
            modelTextDisplay.text = "ML Model: ${file?.name ?: "Not found"}"

            overlay.visibility = View.VISIBLE
            imgShowHideOverlay.setOnClickListener {
                overlay.visibility = if (overlay.visibility == View.VISIBLE) {
                    View.INVISIBLE
                } else {
                    View.VISIBLE
                }
            }

            imgCameraSwitch.setOnClickListener {
                when (getCurrentCamera()) {
                    CameraCharacteristics.LENS_FACING_FRONT -> context?.let { it1 ->
                        bindCameraUseCases(
                            it1, false
                        )
                    }

                    CameraCharacteristics.LENS_FACING_BACK -> context?.let { it1 ->
                        bindCameraUseCases(
                            it1, true
                        )
                    }
                }
            }

            imgRecord.visibility = if (context?.let { GeneralUtils.getApiKey(it) } != null) {
                View.VISIBLE
            } else {
                View.GONE
            }
            imgRecord.setOnClickListener {
                try {
                    if (hbRecorder?.isBusyRecording == true) {
                        hbRecorder?.stopScreenRecording()
                    } else {
                        hbRecorder?.setMaxDuration(15)
                        hbRecorder?.isAudioEnabled(true)
                        startRecordingScreen()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            val bottomSheetBehavior =
                BottomSheetBehavior.from(fragmentCameraBinding.hiddenButtonsLayout)
            bottomSheetBehavior.isHideable = true
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED // Initially expanded

            // Swipe gesture to show the hidden layout
            fragmentCameraBinding.modelTextDisplay.setOnClickListener {
                if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_HIDDEN) {
                    bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                } else {
                    bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
                }
            }
        }
    }

    private fun setUpCamera(context: Context) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            bindCameraUseCases(context)
        }, ContextCompat.getMainExecutor(context))
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun bindCameraUseCases(context: Context, frontCameraOn: Boolean = false) {
        val cameraProvider =
            cameraProvider ?: throw IllegalStateException("Camera initialization failed.")
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

        val frontCameraId = cameraManager.cameraIdList.find { cameraId ->
            cameraManager.getCameraCharacteristics(cameraId)
                .get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT
        }
        val backCameraId = cameraManager.cameraIdList.find { cameraId ->
            cameraManager.getCameraCharacteristics(cameraId)
                .get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
        }

        val cameraSelector = when {
            frontCameraOn && frontCameraId != null -> {
                Log.d(TAG, "Using front camera")
                CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_FRONT).build()
            }

            !frontCameraOn && backCameraId != null -> {
                Log.d(TAG, "Using back camera")
                CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()
            }

            backCameraId != null -> {
                Log.d(TAG, "Using default back camera")
                CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()
            }

            frontCameraId != null -> {
                Log.d(TAG, "Using default front camera")
                CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_FRONT).build()
            }

            else -> {
                CameraSelector.DEFAULT_BACK_CAMERA
            }
        }

        currentCameraSelector = cameraSelector

        preview = Preview.Builder().setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setTargetRotation(fragmentCameraBinding.viewFinder.display.rotation).build()

        imageAnalyzer = ImageAnalysis.Builder().setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setTargetRotation(fragmentCameraBinding.viewFinder.display.rotation)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888).build().also {
                it.setAnalyzer(cameraExecutor) { image ->
                    if (!::bitmapBuffer.isInitialized) {
                        bitmapBuffer = Bitmap.createBitmap(
                            image.width, image.height, Bitmap.Config.ARGB_8888
                        )
                    }
                    detectObjects(image)
                }
            }

        cameraProvider.unbindAll()

        try {
            camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)
            preview?.setSurfaceProvider(fragmentCameraBinding.viewFinder.surfaceProvider)
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

    private fun detectObjects(image: ImageProxy) {
        image.use { bitmapBuffer.copyPixelsFromBuffer(image.planes[0].buffer) }
        val imageRotation = image.imageInfo.rotationDegrees
        objectDetectorHelper.detect(bitmapBuffer, imageRotation)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        imageAnalyzer?.targetRotation = fragmentCameraBinding.viewFinder.display.rotation
    }

    override fun onResults(
        results: MutableList<Detection>?, inferenceTime: Long, imageHeight: Int, imageWidth: Int
    ) {
        activity?.runOnUiThread {
            fragmentCameraBinding.overlay.setResults(
                results ?: LinkedList(), imageHeight, imageWidth
            )
            fragmentCameraBinding.overlay.invalidate()
        }
    }

    override fun onError(error: String) {
        activity?.runOnUiThread {
            Toast.makeText(application, error, Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("RestrictedApi")
    private fun getCurrentCamera(): Int? {
        return currentCameraSelector?.lensFacing
    }

    override fun HBRecorderOnStart() {
        Log.e("HBRecorderOnStart", "HBRecorderOnStart")
        try {
            startPulseAnimation(fragmentCameraBinding.imgRecord)
            Banner.make(view, activity, Banner.INFO, "Recording Started", Banner.BOTTOM, 3000)
                .show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun HBRecorderOnComplete() {
        Log.e("HBRecorderOnComplete", "HBRecorderOnComplete")
        try {
            stopPulseAnimation(fragmentCameraBinding.imgRecord)
            Banner.make(view, activity, Banner.INFO, "Recording Complete", Banner.BOTTOM, 3000)
                .show()
            context?.let {
                UploadDownloadUtils.upload(
                    hbRecorder!!.filePath,
                    hbRecorder!!.fileName,
                    it,
                    activity as LifecycleOwner,
                    true
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun HBRecorderOnError(errorCode: Int, reason: String?) {
        stopPulseAnimation(fragmentCameraBinding.imgRecord)
        when (errorCode) {
            SETTINGS_ERROR -> {
                Toast.makeText(context, "Screen Cast Permission Denied", Toast.LENGTH_SHORT).show()
            }

            MAX_FILE_SIZE_REACHED_ERROR -> {
                Toast.makeText(context, "Max File Size Reached", Toast.LENGTH_SHORT).show()
            }

            else -> {
                Toast.makeText(context, "Error: $errorCode", Toast.LENGTH_SHORT).show()
                Log.e("HBRecorderOnError", "Error: $errorCode")
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun HBRecorderOnPause() {
        Log.e("HBRecorderOnPause", "HBRecorderOnPause")
        hbRecorder?.pauseScreenRecording()
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun HBRecorderOnResume() {
        Log.e("HBRecorderOnResume", "HBRecorderOnResume")
        hbRecorder?.resumeScreenRecording()
    }

    private fun startPulseAnimation(imageView: ImageView) {
        val scaleX = ObjectAnimator.ofFloat(imageView, View.SCALE_X, 0.75f, 1.0f, 0.75f)
        val scaleY = ObjectAnimator.ofFloat(imageView, View.SCALE_Y, 0.75f, 1.0f, 0.75f)
        scaleX.duration = 1000
        scaleY.duration = 1000
        scaleX.repeatCount = ObjectAnimator.INFINITE
        scaleY.repeatCount = ObjectAnimator.INFINITE

        AnimatorSet().apply {
            playTogether(scaleX, scaleY)
            start()
        }.also { imageView.tag = it }
    }

    private fun stopPulseAnimation(imageView: ImageView) {
        (imageView.tag as? AnimatorSet)?.cancel()
        imageView.scaleX = 0.75f
        imageView.scaleY = 0.75f
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is ScreenRecordingCallback) {
            callback = context
        } else {
            throw RuntimeException("$context must implement ScreenRecordingCallback")
        }
    }

    private fun startRecordingScreen() {
        callback?.requestScreenRecordingPermission()
    }

    fun onScreenRecordingPermissionResult(resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            // Start screen recording
            hbRecorder!!.startScreenRecording(data, resultCode)
        }
    }

    interface ScreenRecordingCallback {
        fun requestScreenRecordingPermission()
    }
}
