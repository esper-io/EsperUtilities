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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.ui.PlayerView
import io.esper.android.files.R
import io.esper.android.files.databinding.IminAppLayoutPhotoBinding
import io.esper.android.files.databinding.IminAppLayoutVideoBinding
import io.esper.android.files.util.Constants
import io.esper.android.files.util.GeneralUtils
import io.esper.devicesdk.EsperDeviceSDK
import io.esper.devicesdk.constants.AppOpsPermissions
import me.zhanghai.android.systemuihelper.SystemUiHelper
import java.io.File

class IminDualScreenFragment : Fragment() {
    private lateinit var photoBinding: IminAppLayoutPhotoBinding
    private lateinit var videoBinding: IminAppLayoutVideoBinding
    private lateinit var systemUiHelper: SystemUiHelper
    private val activity: AppCompatActivity by lazy { requireActivity() as AppCompatActivity }
    private var player: SimpleExoPlayer? = null
    private var playerPosition: Long = 0

    private val slideshowHandler = Handler(Looper.getMainLooper())
    private var photoFiles: List<File> = emptyList()
    private var currentPhotoIndex = 0

    private var presentation: Presentation? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = if (GeneralUtils.isIminUsingVideos()) {
        IminAppLayoutVideoBinding.inflate(inflater, container, false)
            .also { videoBinding = it }.root
    } else {
        IminAppLayoutPhotoBinding.inflate(inflater, container, false)
            .also { photoBinding = it }.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val activity = requireActivity() as AppCompatActivity
        systemUiHelper = SystemUiHelper(
            activity, SystemUiHelper.LEVEL_IMMERSIVE, SystemUiHelper.FLAG_IMMERSIVE_STICKY
        )
        setHasOptionsMenu(true)
        if (!Settings.canDrawOverlays(requireContext())) {
            setupSDKActions()
            showSecondByDisplayManager(requireContext())
        } else {
            init(savedInstanceState)
        }
    }

    fun showSecondByDisplayManager(context: Context) {
        val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        val presentationDisplay = IminUtils.getPresentationDisplay(displayManager)
        if (presentationDisplay != null) {
            presentation = DifferentDisplay(context.applicationContext, presentationDisplay)
            presentation?.show()
        } else {
            Toast.makeText(context, "No secondary display found!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupSDKActions() {
        GeneralUtils.isEsperDeviceSDKActivated(requireContext()) { activated ->
            if (activated) {
                Log.d(
                    Constants.IminDualScreenFragmentTag,
                    "setupSDKActions: Esper Device SDK is activated"
                )
                GeneralUtils.getEsperSDK(requireContext())
                    .setAppOpMode(AppOpsPermissions.OP_SYSTEM_ALERT_WINDOW,
                        true,
                        object : EsperDeviceSDK.Callback<Void> {
                            override fun onResponse(p0: Void?) {
                                Log.i(
                                    Constants.IminDualScreenFragmentTag,
                                    "setupSDKActions: setAppOpMode onResponse: OP_WRITE_SETTINGS -> success"
                                )
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
        if (GeneralUtils.isIminUsingVideos()) {
            setupPlayer(savedInstanceState)
        } else {
            setupPhoto()
        }
    }

    private fun setupPhoto() {
        val sharedPrefManaged = requireContext().getSharedPreferences(
            Constants.SHARED_MANAGED_CONFIG_VALUES, Context.MODE_PRIVATE
        )
        val photosPath =
            sharedPrefManaged.getString(Constants.SHARED_MANAGED_CONFIG_IMIN_APP_PATH, null)
        photoFiles = photosPath?.let { File(it) }?.let { getPhotoFiles(it) } ?: emptyList()

        if (photoFiles.isNotEmpty()) {
            startSlideshow()
        }
    }

    private fun startSlideshow() {
        val slideshowRunnable = object : Runnable {
            override fun run() {
                if (photoFiles.isNotEmpty()) {
                    photoBinding.photoView.setImageURI(Uri.fromFile(photoFiles[currentPhotoIndex]))
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
        player = SimpleExoPlayer.Builder(activity).build()
        player!!.setAudioAttributes(AudioAttributes.DEFAULT, true)
        player!!.playWhenReady = true
        player!!.repeatMode = Player.REPEAT_MODE_ALL
        videoBinding.playerView.player = player
        presentation?.findViewById<PlayerView>(R.id.player_view)?.player = player

        if (savedInstanceState != null) {
            playerPosition = savedInstanceState.getLong(PLAYER_POSITION_KEY, 0)
        }

        val sharedPrefManaged = requireContext().getSharedPreferences(
            Constants.SHARED_MANAGED_CONFIG_VALUES, Context.MODE_PRIVATE
        )
        val videosPath =
            sharedPrefManaged.getString(Constants.SHARED_MANAGED_CONFIG_IMIN_APP_PATH, null)
        val mediaItems = videosPath?.let { File(it) }?.let {
            getVideoFiles(it).map { file ->
                MediaItem.fromUri(Uri.parse(file.toString()))
            }
        }
        mediaItems?.let { player!!.setMediaItems(it, false) }
        player!!.prepare()
        player!!.seekTo(playerPosition)
        player!!.play()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong(PLAYER_POSITION_KEY, player?.currentPosition ?: 0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        player?.release()
        player = null
        slideshowHandler.removeCallbacksAndMessages(null)
    }

    override fun onPause() {
        super.onPause()
        player?.playWhenReady = false
    }

    override fun onResume() {
        super.onResume()
        player?.playWhenReady = true
    }

    override fun onStop() {
        super.onStop()
        player?.playWhenReady = false
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
        if (presentation != null) {
            presentation!!.cancel()
            presentation = null
        }
    }

    companion object {
        private const val PLAYER_POSITION_KEY = "player_position"
        private const val SLIDESHOW_INTERVAL = 10000L // 3 seconds
    }
}
