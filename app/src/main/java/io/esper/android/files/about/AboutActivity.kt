package io.esper.android.files.about

import android.os.Bundle
import android.view.View
import androidx.fragment.app.add
import androidx.fragment.app.commit
import io.esper.android.files.app.AppActivity

class AboutActivity : AppActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Calls ensureSubDecor().
        findViewById<View>(android.R.id.content)
        if (savedInstanceState == null) {
            supportFragmentManager.commit { add<AboutFragment>(android.R.id.content) }
        }
    }
}
