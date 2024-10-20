package io.esper.android.files.storage

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Environment
import androidx.annotation.DrawableRes
import java8.nio.file.Path
import kotlinx.parcelize.Parcelize
import io.esper.android.files.R
import io.esper.android.files.compat.getDescriptionCompat
import io.esper.android.files.compat.isPrimaryCompat
import io.esper.android.files.compat.pathCompat
import io.esper.android.files.file.DocumentTreeUri
import io.esper.android.files.file.displayName
import io.esper.android.files.file.storageVolume
import io.esper.android.files.provider.document.createDocumentTreeRootPath
import io.esper.android.files.util.createIntent
import io.esper.android.files.util.putArgs
import io.esper.android.files.util.supportsExternalStorageManager
import kotlin.random.Random

@Parcelize
data class DocumentTree(
    override val id: Long,
    override val customName: String?,
    val uri: DocumentTreeUri
) : Storage() {
    constructor(
        id: Long?,
        customName: String?,
        uri: DocumentTreeUri
    ) : this(id ?: Random.nextLong(), customName, uri)

    override val iconRes: Int
        @DrawableRes
        // Error: Call requires API level 24 (current min is 21):
        // android.os.storage.StorageVolume#equals [NewApi]
        @SuppressLint("NewApi")
        get() =
            // We are using MANAGE_EXTERNAL_STORAGE to access all storage volumes when supported.
            if (!Environment::class.supportsExternalStorageManager()
                && uri.storageVolume.let { it != null && !it.isPrimaryCompat }) {
                R.drawable.sd_card_icon_white_24dp
            } else {
                super.iconRes
            }

    override fun getDefaultName(context: Context): String =
        uri.storageVolume?.getDescriptionCompat(context) ?: uri.displayName
            ?: uri.value.lastPathSegment ?: uri.value.toString()

    override val description: String
        get() = uri.value.toString()

    override val path: Path
        get() = uri.value.createDocumentTreeRootPath()

    override val linuxPath: String?
        get() = uri.storageVolume?.pathCompat

    override fun createEditIntent(): Intent =
        EditDocumentTreeDialogActivity::class.createIntent()
            .putArgs(EditDocumentTreeDialogFragment.Args(this))
}
