package io.esper.android.files.viewer.text

import android.os.Bundle
import android.view.View
import androidx.fragment.app.commit
import io.esper.android.files.app.AppActivity
import io.esper.android.files.util.putArgs

class TextEditorActivity : AppActivity() {
    private lateinit var fragment: TextEditorFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Calls ensureSubDecor().
        findViewById<View>(android.R.id.content)
        if (savedInstanceState == null) {
            fragment = TextEditorFragment().putArgs(TextEditorFragment.Args(intent))
            supportFragmentManager.commit { add(android.R.id.content, fragment) }
        } else {
            fragment = supportFragmentManager.findFragmentById(android.R.id.content)
                as TextEditorFragment
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        if (fragment.onSupportNavigateUp()) {
            return true
        }
        return super.onSupportNavigateUp()
    }
}
