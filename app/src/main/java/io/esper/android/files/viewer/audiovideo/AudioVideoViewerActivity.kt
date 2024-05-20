package io.esper.android.files.viewer.audiovideo

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.commit
import io.esper.android.files.app.AppActivity
import io.esper.android.files.util.extraPathList
import io.esper.android.files.util.putArgs
import java8.nio.file.Path

class AudioVideoViewerActivity : AppActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Calls ensureSubDecor().

        findViewById<View>(android.R.id.content)
        if (savedInstanceState == null) {
            val intent = intent
            val position = intent.getIntExtra(EXTRA_POSITION, 0)
            val fragment = AudioVideoViewerFragment().putArgs(AudioVideoViewerFragment.Args(intent, position))
            supportFragmentManager.commit { add(android.R.id.content, fragment) }
        }
    }

    companion object {
        private val EXTRA_POSITION = "${AudioVideoViewerActivity::class.java.name}.extra.POSITION"

        fun putExtras(intent: Intent, paths: List<Path>, position: Int) {
            // All extra put here must be framework classes, or we may crash the resolver activity.
            intent.extraPathList = paths
            intent.putExtra(EXTRA_POSITION, position)
        }
    }
}