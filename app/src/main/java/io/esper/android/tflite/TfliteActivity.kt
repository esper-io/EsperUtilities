package io.esper.android.tflite

import android.R
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.view.View
import androidx.fragment.app.add
import androidx.fragment.app.commit
import io.esper.android.files.app.AppActivity
import io.esper.android.files.util.Constants
import io.esper.android.files.util.showToast


class TfliteActivity : AppActivity(), TfliteFragment.ScreenRecordingCallback {
    private val SCREEN_RECORD_REQUEST_CODE = 777

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedPrefManaged =
            getSharedPreferences(Constants.SHARED_MANAGED_CONFIG_VALUES, Context.MODE_PRIVATE)
        if (!sharedPrefManaged.getBoolean(
                Constants.SHARED_MANAGED_CONFIG_CONVERT_TO_TFLITE_APP, false
            )
        ) {
            showToast("Tflite Activity has been disabled by your administrator.")
            finish()
        } else {
            // Calls ensureSubDecor().
            findViewById<View>(R.id.content)
            if (savedInstanceState == null) {
                supportFragmentManager.commit { add<TfliteFragment>(R.id.content) }
            }
        }
    }

    override fun requestScreenRecordingPermission() {
        val mediaProjectionManager =
            getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val permissionIntent = mediaProjectionManager.createScreenCaptureIntent()
        startActivityForResult(permissionIntent, SCREEN_RECORD_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SCREEN_RECORD_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                (supportFragmentManager.findFragmentById(R.id.content) as TfliteFragment?)?.onScreenRecordingPermissionResult(
                    resultCode, data
                )
            }
        }
    }
}