package io.esper.android.files.filelist

import android.content.Intent
import android.os.Bundle
import java8.nio.file.Path
import io.esper.android.files.app.AppActivity
import io.esper.android.files.app.application
import io.esper.android.files.file.MimeType
import io.esper.android.files.file.asMimeTypeOrNull
import io.esper.android.files.file.fileProviderUri
import io.esper.android.files.filejob.FileJobService
import io.esper.android.files.provider.archive.isArchivePath
import io.esper.android.files.util.createViewIntent
import io.esper.android.files.util.extraPath
import io.esper.android.files.util.startActivitySafe

class OpenFileActivity : AppActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val intent = intent
        val path = intent.extraPath
        val mimeType = intent.type?.asMimeTypeOrNull()
        if (path != null && mimeType != null) {
            openFile(path, mimeType)
        }
        finish()
    }

    private fun openFile(path: Path, mimeType: MimeType) {
        if (path.isArchivePath) {
            FileJobService.open(path, mimeType, false, this)
        } else {
            val intent = path.fileProviderUri.createViewIntent(mimeType)
                .addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                .apply { extraPath = path }
            startActivitySafe(intent)
        }
    }

    companion object {
        private const val ACTION_OPEN_FILE = "io.esper.android.files.intent.action.OPEN_FILE"

        fun createIntent(path: Path, mimeType: MimeType): Intent =
            Intent(ACTION_OPEN_FILE)
                .setPackage(application.packageName)
                .setType(mimeType.value)
                .apply { extraPath = path }
    }
}
