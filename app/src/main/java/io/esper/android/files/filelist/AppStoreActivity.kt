package io.esper.android.files.filelist

import android.os.Bundle
import android.view.View
import androidx.fragment.app.add
import androidx.fragment.app.commit
import io.esper.android.files.app.AppActivity
import io.esper.android.files.util.GeneralUtils
import io.esper.android.files.util.showToast

class AppStoreActivity : AppActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!GeneralUtils.isEsperAppStoreVisible(this) || GeneralUtils.getApiKey(this) == null) {
            showToast("Esper App Store access has been disabled by your administrator.")
            finish()
        } else {
            // Calls ensureSubDecor().
            findViewById<View>(android.R.id.content)
            if (savedInstanceState == null) {
                supportFragmentManager.commit { add<AppStoreFragment>(android.R.id.content) }
            }
        }
    }
}

