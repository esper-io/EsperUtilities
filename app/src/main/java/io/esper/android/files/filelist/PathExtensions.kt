package io.esper.android.files.filelist

import java8.nio.file.Path
import io.esper.android.files.file.MimeType
import io.esper.android.files.file.isSupportedArchive
import io.esper.android.files.provider.archive.archiveFile
import io.esper.android.files.provider.archive.isArchivePath
import io.esper.android.files.provider.document.isDocumentPath
import io.esper.android.files.provider.document.resolver.DocumentResolver
import io.esper.android.files.provider.linux.isLinuxPath

val Path.name: String
    get() = fileName?.toString() ?: if (isArchivePath) archiveFile.fileName.toString() else "/"

fun Path.toUserFriendlyString(): String = if (isLinuxPath) toFile().path else toUri().toString()

fun Path.isArchiveFile(mimeType: MimeType): Boolean = !isArchivePath && mimeType.isSupportedArchive

val Path.isLocalPath: Boolean
    get() =
        isLinuxPath || (isDocumentPath && DocumentResolver.isLocal(this as DocumentResolver.Path))

val Path.isRemotePath: Boolean
    get() = !isLocalPath
