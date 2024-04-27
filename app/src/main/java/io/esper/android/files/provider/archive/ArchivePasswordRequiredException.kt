package io.esper.android.files.provider.archive

import android.content.Context
import java8.nio.file.Path
import io.esper.android.files.fileaction.ArchivePasswordDialogActivity
import io.esper.android.files.fileaction.ArchivePasswordDialogFragment
import io.esper.android.files.provider.common.UserAction
import io.esper.android.files.provider.common.UserActionRequiredException
import io.esper.android.files.util.createIntent
import io.esper.android.files.util.putArgs
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

class ArchivePasswordRequiredException(
    private val file: Path,
    reason: String?
) :
    UserActionRequiredException(file.toString(), null, reason) {

    override fun getUserAction(continuation: Continuation<Boolean>, context: Context): UserAction {
        return UserAction(
            ArchivePasswordDialogActivity::class.createIntent().putArgs(
                ArchivePasswordDialogFragment.Args(file) { continuation.resume(it) }
            ), ArchivePasswordDialogFragment.getTitle(context),
            ArchivePasswordDialogFragment.getMessage(file, context)
        )
    }
}
