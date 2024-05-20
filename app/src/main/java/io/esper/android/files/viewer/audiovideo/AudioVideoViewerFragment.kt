package io.esper.android.files.viewer.audiovideo

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.audio.AudioAttributes
import dev.chrisbanes.insetter.applySystemWindowInsetsToPadding
import io.esper.android.files.databinding.VideoFragmentBinding
import io.esper.android.files.util.ParcelableArgs
import io.esper.android.files.util.args
import io.esper.android.files.util.extraPathList
import io.esper.android.files.util.finish
import io.esper.android.files.util.mediumAnimTime
import kotlinx.parcelize.Parcelize
import me.zhanghai.android.systemuihelper.SystemUiHelper
import java.io.File

class AudioVideoViewerFragment : Fragment() {
    private lateinit var binding: VideoFragmentBinding
    private val args by args<Args>()
    private lateinit var systemUiHelper: SystemUiHelper
    private val argsPaths by lazy { args.intent.extraPathList }
    private val activity: AppCompatActivity by lazy { requireActivity() as AppCompatActivity }
    private var player: SimpleExoPlayer? = null
    private var toolbarHidden = false
    private var playerPosition: Long = 0

    private val hideHandler = Runnable {
        if (isAdded) {
            systemUiHelper.hide()
            binding.appBarLayout.animate().alpha(0f)
                .translationY(-binding.appBarLayout.bottom.toFloat()).setDuration(400)
                .setInterpolator(FastOutSlowInInterpolator()).start()
            toolbarHidden = true
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = VideoFragmentBinding.inflate(inflater, container, false).also { binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        setupPlayer(savedInstanceState)
        setupToolbarHiding()
    }

    private fun setupViews() {
        if (argsPaths.isEmpty()) {
            // TODO: Show a toast or handle the case where argsPaths is empty
            finish()
            return
        }

        activity.setSupportActionBar(binding.toolbar)
        activity.supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        activity.window.statusBarColor = Color.TRANSPARENT
        binding.appBarLayout.applySystemWindowInsetsToPadding(left = true, top = true, right = true)
        systemUiHelper = SystemUiHelper(
            activity, SystemUiHelper.LEVEL_IMMERSIVE, SystemUiHelper.FLAG_IMMERSIVE_STICKY
        ) { visible: Boolean ->
            if (!toolbarHidden && isAdded) {
                binding.appBarLayout.animate().alpha(if (visible) 1f else 0f)
                    .translationY(if (visible) 0f else -binding.appBarLayout.bottom.toFloat())
                    .setDuration(mediumAnimTime.toLong())
                    .setInterpolator(FastOutSlowInInterpolator()).start()
            }
        }
        systemUiHelper.show()
        updateTitle(File(argsPaths[0].toString()).name)
    }

    private fun setupPlayer(savedInstanceState: Bundle? = null) {
        player = SimpleExoPlayer.Builder(activity).build()
        player!!.setAudioAttributes(AudioAttributes.DEFAULT, true)
        player!!.playWhenReady = true
        binding.playerView.player = player

        if (savedInstanceState != null) {
            playerPosition = savedInstanceState.getLong(PLAYER_POSITION_KEY, 0)
        }

        player!!.addListener(playerListener)
        player!!.setMediaItem(MediaItem.fromUri(Uri.parse(argsPaths[0].toString())))
        player!!.prepare()
        player!!.seekTo(playerPosition)
        player!!.play()
    }

    private fun setupToolbarHiding() {
        binding.playerView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    showToolbar()
                }

                MotionEvent.ACTION_UP -> {
                    binding.playerView.performClick()
                }
            }
            true
        }

        binding.playerView.postDelayed(hideHandler, HIDE_DELAY_MS)
    }

    private fun showToolbar() {
        binding.appBarLayout.animate().cancel()
        binding.appBarLayout.animate().alpha(1f).translationY(0f).setDuration(400)
            .setInterpolator(FastOutSlowInInterpolator()).start()
        toolbarHidden = false

        binding.playerView.removeCallbacks(hideHandler)
        binding.playerView.postDelayed(hideHandler, HIDE_DELAY_MS)
    }

    private fun updateTitle(title: String? = null) {
        if (!TextUtils.isEmpty(title)) {
            activity.title = title
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong(PLAYER_POSITION_KEY, player?.currentPosition ?: 0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        player?.removeListener(playerListener)
        player?.release()
        player = null
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
    }

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(state: Int) {
            if (state == Player.STATE_ENDED) {
                // show the toolbar
                showToolbar()
            }
        }
    }

    @Parcelize
    class Args(val intent: Intent, val position: Int) : ParcelableArgs

    companion object {
        private const val PLAYER_POSITION_KEY = "player_position"
        private const val HIDE_DELAY_MS = 4000L
    }
}
