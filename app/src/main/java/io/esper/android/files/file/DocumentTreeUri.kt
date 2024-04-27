package io.esper.android.files.file

import android.content.Intent
import android.net.Uri
import android.os.Parcelable
import android.os.storage.StorageVolume
import android.provider.DocumentsContract
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.WriteWith
import io.esper.android.files.app.contentResolver
import io.esper.android.files.compat.DocumentsContractCompat
import io.esper.android.files.compat.createOpenDocumentTreeIntentCompat
import io.esper.android.files.storage.StorageVolumeListLiveData
import io.esper.android.files.util.StableUriParceler
import io.esper.android.files.util.getParcelableExtraSafe
import io.esper.android.files.util.releasePersistablePermission
import io.esper.android.files.util.takePersistablePermission
import io.esper.android.files.util.valueCompat

@Parcelize
@JvmInline
value class DocumentTreeUri(val value: @WriteWith<StableUriParceler> Uri) : Parcelable {
    val documentId: String
        get() = DocumentsContract.getTreeDocumentId(value)

    companion object {
        val persistedUris: List<DocumentTreeUri>
            get() =
                contentResolver.persistedUriPermissions
                    .filter { it.uri.isDocumentTreeUri }
                    .sortedBy { it.persistedTime }
                    .map { it.uri.asDocumentTreeUri() }
    }
}

fun Uri.asDocumentTreeUriOrNull(): DocumentTreeUri? =
    if (isDocumentTreeUri) DocumentTreeUri(this) else null

fun Uri.asDocumentTreeUri(): DocumentTreeUri {
    check(isDocumentTreeUri)
    return DocumentTreeUri(this)
}

private val Uri.isDocumentTreeUri: Boolean
    get() = DocumentsContractCompat.isTreeUri(this)

fun DocumentTreeUri.buildDocumentUri(documentId: String): DocumentUri =
    DocumentsContract.buildDocumentUriUsingTree(value, documentId).asDocumentUri()

val DocumentTreeUri.displayName: String?
    get() = buildDocumentUri(documentId).displayName

fun DocumentTreeUri.takePersistablePermission(): Boolean =
    value.takePersistablePermission(
        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
    ) || value.takePersistablePermission(Intent.FLAG_GRANT_READ_URI_PERMISSION)

fun DocumentTreeUri.releasePersistablePermission(): Boolean =
    value.releasePersistablePermission(
        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
    )

val StorageVolume.documentTreeUri: DocumentTreeUri
    get() {
        val intent = createOpenDocumentTreeIntentCompat()
        val rootUri = intent.getParcelableExtraSafe<Uri>(
            DocumentsContractCompat.EXTRA_INITIAL_URI
        )!!
        // @see com.android.externalstorage.ExternalStorageProvider#getDocIdForFile(File)
        // @see com.android.documentsui.picker.ConfirmFragment#onCreateDialog(Bundle)
        return DocumentsContract.buildTreeDocumentUri(
            rootUri.authority, "${DocumentsContract.getRootId(rootUri)}:"
        ).asDocumentTreeUri()
    }

val DocumentTreeUri.storageVolume: StorageVolume?
    get() = StorageVolumeListLiveData.valueCompat.find { it.documentTreeUri == this }
