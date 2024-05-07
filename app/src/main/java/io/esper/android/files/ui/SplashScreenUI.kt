package io.esper.android.files.ui

import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.Toast
import io.esper.android.files.R
import io.esper.android.files.app.AppActivity

class SplashScreenUI : AppActivity() {

    private var clickCount = 0
    private lateinit var imageView: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        setContentView(R.layout.activity_full_screen_image)
        hideSystemUI()
        imageView = findViewById(R.id.fullScreenImageView)

        startBreathingAnimation()

        imageView.setOnClickListener {
            clickCount++
            if (clickCount >= 15) {
                Toast.makeText(this, "App developed by: Karthik Mohan", Toast.LENGTH_SHORT).show()
                clickCount = 0
            }
        }

        imageView.setOnLongClickListener {
            finish()
            true
        }
    }

    private fun hideSystemUI() {
        window.decorView.systemUiVisibility =
            (View.SYSTEM_UI_FLAG_IMMERSIVE or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LOW_PROFILE)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemUI()
        }
    }

    private fun startBreathingAnimation() {
        val alpha = ObjectAnimator.ofFloat(imageView, View.ALPHA, 0.5f, 1.0f)
        alpha.repeatCount = ObjectAnimator.INFINITE
        alpha.repeatMode = ObjectAnimator.REVERSE
        alpha.interpolator = AccelerateDecelerateInterpolator()
        alpha.duration = 1000
        alpha.start()
    }
}
