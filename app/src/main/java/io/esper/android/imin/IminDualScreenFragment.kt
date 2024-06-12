package io.esper.android.imin

import android.app.Presentation
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import io.esper.android.files.R
import io.esper.android.files.util.Constants
import io.esper.android.files.util.GeneralUtils
import io.esper.android.files.util.finish
import io.esper.devicesdk.EsperDeviceSDK
import io.esper.devicesdk.constants.AppOpsPermissions
import me.zhanghai.android.systemuihelper.SystemUiHelper
import java.io.File

class IminDualScreenFragment : Fragment() {
    private lateinit var systemUiHelper: SystemUiHelper
    private val activity: AppCompatActivity by lazy { requireActivity() as AppCompatActivity }
    private var playerPosition: Long = 0

    private val slideshowHandler = Handler(Looper.getMainLooper())
    private var photoFiles: List<File> = emptyList()
    private var currentPhotoIndex = 0

    private var presentation: Presentation? = null
    private var videoView: VideoView? = null

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        systemUiHelper = SystemUiHelper(
            activity, SystemUiHelper.LEVEL_IMMERSIVE, SystemUiHelper.FLAG_IMMERSIVE_STICKY
        )
        if (!Settings.canDrawOverlays(requireContext())) {
            setupSDKActions()
        } else {
            init(savedInstanceState)
        }
    }

    private fun showSecondByDisplayManager(context: Context) {
        val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        val presentationDisplay = IminUtils.getPresentationDisplay(displayManager)
        if (presentationDisplay != null) {
            presentation = DifferentDisplay(context.applicationContext, presentationDisplay)
            presentation?.show()

            // Initialize video view for presentation
            videoView = presentation?.findViewById(R.id.secondaryDisplayVideoView)
        } else {
            Toast.makeText(context, "No secondary display found!", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun setupSDKActions() {
        GeneralUtils.isEsperDeviceSDKActivated(requireContext()) { activated ->
            if (activated) {
                Log.d(Constants.IminDualScreenFragmentTag, "Esper Device SDK is activated")
                GeneralUtils.getEsperSDK(requireContext())
                    .setAppOpMode(AppOpsPermissions.OP_SYSTEM_ALERT_WINDOW, true,
                        object : EsperDeviceSDK.Callback<Void> {
                            override fun onResponse(p0: Void?) {
                                Log.i(Constants.IminDualScreenFragmentTag, "Set AppOpMode success")
                                requireActivity().runOnUiThread {
                                    init()
                                }
                            }

                            override fun onFailure(t: Throwable) {
                                requireActivity().runOnUiThread {
                                    Toast.makeText(
                                        requireContext(),
                                        "Please allow the screen overlay permission!",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION))
                                }
                            }
                        })
            }
        }
    }

    private fun init(savedInstanceState: Bundle? = null) {
        showSecondByDisplayManager(requireContext())
        if (GeneralUtils.isIminUsingVideos()) {
            setupPlayer(savedInstanceState)
        } else {
            setupPhoto()
        }
        activity.moveTaskToBack(true)
    }

    private fun setupPhoto() {
        val sharedPrefManaged = requireContext().getSharedPreferences(
            Constants.SHARED_MANAGED_CONFIG_VALUES, Context.MODE_PRIVATE
        )
        val photosPath = sharedPrefManaged.getString(Constants.SHARED_MANAGED_CONFIG_IMIN_APP_PATH, null)
        photoFiles = photosPath?.let { File(it) }?.let { getPhotoFiles(it) } ?: emptyList()

        if (photoFiles.isNotEmpty()) {
            startSlideshow()
        } else {
            Log.e(Constants.IminDualScreenFragmentTag, "No photo files found")
        }
    }

    private fun startSlideshow() {
        val slideshowRunnable = object : Runnable {
            override fun run() {
                if (photoFiles.isNotEmpty()) {
                    presentation?.findViewById<ImageView>(R.id.photoView)?.setImageURI(
                        Uri.fromFile(photoFiles[currentPhotoIndex])
                    )
                    currentPhotoIndex = (currentPhotoIndex + 1) % photoFiles.size
                    slideshowHandler.postDelayed(this, SLIDESHOW_INTERVAL)
                }
            }
        }
        slideshowHandler.post(slideshowRunnable)
    }

    private fun setupPlayer(savedInstanceState: Bundle? = null) {
        val sharedPrefManaged = requireContext().getSharedPreferences(
            Constants.SHARED_MANAGED_CONFIG_VALUES, Context.MODE_PRIVATE
        )
        val videosPath = sharedPrefManaged.getString(Constants.SHARED_MANAGED_CONFIG_IMIN_APP_PATH, null)
        val videoFiles = videosPath?.let { File(it) }?.let { getVideoFiles(it) } ?: emptyList()

        if (videoFiles.isNotEmpty()) {
            playVideoFiles(videoFiles, savedInstanceState)
        } else {
            Log.e(Constants.IminDualScreenFragmentTag, "No video files found")
        }
    }

    private fun playVideoFiles(videoFiles: List<File>, savedInstanceState: Bundle? = null) {
        if (videoFiles.isEmpty()) {
            Log.e(Constants.IminDualScreenFragmentTag, "No video files found")
            return
        }
        var currentIndex = 0
        val videoUri = Uri.parse(videoFiles[currentIndex].toString())
        videoView?.setVideoURI(videoUri)
        videoView?.setOnPreparedListener { _ ->
            if (savedInstanceState != null) {
                playerPosition = savedInstanceState.getLong(PLAYER_POSITION_KEY, 0)
                videoView?.seekTo(playerPosition.toInt())
            }
            videoView?.start()
        }
        videoView?.setOnErrorListener { _, what, extra ->
            Log.e(Constants.IminDualScreenFragmentTag, "Error playing video: what=$what, extra=$extra")
            true
        }
        videoView?.setOnCompletionListener {
            currentIndex++
            if (currentIndex < videoFiles.size) {
                val nextVideoUri = Uri.parse(videoFiles[currentIndex].toString())
                videoView?.setVideoURI(nextVideoUri)
                videoView?.start()
            } else {
                Log.d(Constants.IminDualScreenFragmentTag, "All videos played, starting over.")
                currentIndex = 0
                val nextVideoUri = Uri.parse(videoFiles[currentIndex].toString())
                videoView?.setVideoURI(nextVideoUri)
                videoView?.start()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        slideshowHandler.removeCallbacksAndMessages(null)
    }

    private fun getVideoFiles(dir: File): List<File> {
        return dir.listFiles { file ->
            file.isFile && file.extension in listOf(
                "mp4", "mkv", "avi"
            )
        }?.toList() ?: emptyList()
    }

    private fun getPhotoFiles(dir: File): List<File> {
        return dir.listFiles { file ->
            file.isFile && file.extension in listOf(
                "jpg", "jpeg", "png", "gif"
            )
        }?.toList() ?: emptyList()
    }

    override fun onDestroy() {
        super.onDestroy()
        presentation?.cancel()
        presentation = null
    }

    companion object {
        private const val PLAYER_POSITION_KEY = "player_position"
        private const val SLIDESHOW_INTERVAL = 5000L // 5 seconds
    }
}
