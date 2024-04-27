package io.esper.android.files.filelist

import android.os.Bundle
import java8.nio.file.Path
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.WriteWith
import io.esper.android.files.app.AppActivity
import io.esper.android.files.file.MimeType
import io.esper.android.files.file.fileProviderUri
import io.esper.android.files.util.ParcelableArgs
import io.esper.android.files.util.ParcelableParceler
import io.esper.android.files.util.args
import io.esper.android.files.util.createEditIntent
import io.esper.android.files.util.startActivitySafe

// Use a trampoline activity so that we can have a proper icon and title.
class EditFileActivity : AppActivity() {
    private val args by args<Args>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        startActivitySafe(args.path.fileProviderUri.createEditIntent(args.mimeType))
        finish()
    }

    @Parcelize
    class Args(
        val path: @WriteWith<ParcelableParceler> Path,
        val mimeType: MimeType
    ) : ParcelableArgs
}
