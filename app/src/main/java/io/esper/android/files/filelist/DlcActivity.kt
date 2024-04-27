package io.esper.android.files.filelist

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.add
import androidx.fragment.app.commit
import io.esper.android.files.app.AppActivity
import io.esper.android.files.util.Constants
import io.esper.android.files.util.showToast

class DlcActivity : AppActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedPrefManaged =
            getSharedPreferences(Constants.SHARED_MANAGED_CONFIG_VALUES, Context.MODE_PRIVATE)
        if (!sharedPrefManaged.getBoolean(
                Constants.SHARED_MANAGED_CONFIG_ON_DEMAND_DOWNLOAD, false
            ) || sharedPrefManaged.getString(Constants.SHARED_MANAGED_CONFIG_API_KEY, null) == null
        ) {
            showToast("Downloadable Content access has been disabled by your administrator.")
            finish()
        } else {
            // Calls ensureSubDecor().
            findViewById<View>(android.R.id.content)
            if (savedInstanceState == null) {
                supportFragmentManager.commit { add<DlcFragment>(android.R.id.content) }
            }
        }
    }
}
