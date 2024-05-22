package io.esper.android.files.filelist

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import androidx.activity.result.contract.ActivityResultContract
import androidx.fragment.app.commit
import io.esper.android.files.app.AppActivity
import io.esper.android.files.file.MimeType
import io.esper.android.files.util.Constants
import io.esper.android.files.util.FileUtils
import io.esper.android.files.util.GeneralUtils
import io.esper.android.files.util.ManagedConfigUtils
import io.esper.android.files.util.createIntent
import io.esper.android.files.util.extraPath
import io.esper.android.files.util.putArgs
import io.esper.android.network.NetworkTesterActivity
import java8.nio.file.Path

class FileListActivity : AppActivity() {
    private lateinit var fragment: FileListFragment

    //SharedPref
    private var sharedPrefManaged: SharedPreferences? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Calls ensureSubDecor().
        findViewById<View>(android.R.id.content)

        // TODO check if its needed
//        GeneralUtils.initNetworkConfigs()

        FileUtils.createEsperFolder()
        initSharedPrefs()
        sharedPrefManaged?.let { GeneralUtils.initSDK(it, this) }

        // Check if the files app need to be converted to app store
        if (sharedPrefManaged!!.getBoolean(Constants.SHARED_MANAGED_CONFIG_CONVERT_FILES_TO_APP_STORE, false)) {
            startActivity(AppStoreActivity::class.createIntent())
            finish()
            return
        }

        // Check if the files app need to be converted to network tester
        if (sharedPrefManaged!!.getBoolean(Constants.SHARED_MANAGED_CONFIG_CONVERT_FILES_TO_NETWORK_TESTER, false)) {
            startActivity(NetworkTesterActivity::class.createIntent())
            finish()
            return
        }

        ManagedConfigUtils.getManagedConfigValues(this)
        if (savedInstanceState == null) {
            fragment = FileListFragment().putArgs(FileListFragment.Args(intent))
            supportFragmentManager.commit { add(android.R.id.content, fragment) }
        } else {
            fragment =
                supportFragmentManager.findFragmentById(android.R.id.content) as FileListFragment
        }
    }

    private fun initSharedPrefs() {
        sharedPrefManaged =
            getSharedPreferences(Constants.SHARED_MANAGED_CONFIG_VALUES, Context.MODE_PRIVATE)
    }

    override fun onKeyShortcut(keyCode: Int, event: KeyEvent): Boolean {
        if (fragment.onKeyShortcut(keyCode, event)) {
            return true
        }
        return super.onKeyUp(keyCode, event)
    }

    companion object {
        fun createViewIntent(path: Path): Intent =
            FileListActivity::class.createIntent().setAction(Intent.ACTION_VIEW)
                .apply { extraPath = path }
    }

    class OpenFileContract : ActivityResultContract<List<MimeType>, Path?>() {
        override fun createIntent(context: Context, input: List<MimeType>): Intent =
            FileListActivity::class.createIntent().setAction(Intent.ACTION_OPEN_DOCUMENT)
                .setType(MimeType.ANY.value).addCategory(Intent.CATEGORY_OPENABLE)
                .putExtra(Intent.EXTRA_MIME_TYPES, input.map { it.value }.toTypedArray())

        override fun parseResult(resultCode: Int, intent: Intent?): Path? =
            if (resultCode == RESULT_OK) intent?.extraPath else null
    }

    class CreateFileContract : ActivityResultContract<Triple<MimeType, String?, Path?>, Path?>() {
        override fun createIntent(
            context: Context, input: Triple<MimeType, String?, Path?>
        ): Intent = FileListActivity::class.createIntent().setAction(Intent.ACTION_CREATE_DOCUMENT)
            .setType(input.first.value).addCategory(Intent.CATEGORY_OPENABLE).apply {
                input.second?.let { putExtra(Intent.EXTRA_TITLE, it) }
                input.third?.let { extraPath = it }
            }

        override fun parseResult(resultCode: Int, intent: Intent?): Path? =
            if (resultCode == RESULT_OK) intent?.extraPath else null
    }

    class OpenDirectoryContract : ActivityResultContract<Path?, Path?>() {
        override fun createIntent(context: Context, input: Path?): Intent =
            FileListActivity::class.createIntent().setAction(Intent.ACTION_OPEN_DOCUMENT_TREE)
                .apply { input?.let { extraPath = it } }

        override fun parseResult(resultCode: Int, intent: Intent?): Path? =
            if (resultCode == RESULT_OK) intent?.extraPath else null
    }
}
