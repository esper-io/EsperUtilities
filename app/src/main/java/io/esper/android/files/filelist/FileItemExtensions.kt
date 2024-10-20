package io.esper.android.files.filelist

import android.content.Context
import android.os.Build
import java8.nio.file.Path
import java8.nio.file.attribute.BasicFileAttributes
import java8.nio.file.attribute.FileTime
import io.esper.android.files.file.FileItem
import io.esper.android.files.file.MimeType
import io.esper.android.files.file.getBrokenSymbolicLinkName
import io.esper.android.files.file.getName
import io.esper.android.files.file.isApk
import io.esper.android.files.file.isImage
import io.esper.android.files.file.isMedia
import io.esper.android.files.file.isPdf
import io.esper.android.files.provider.archive.createArchiveRootPath
import io.esper.android.files.provider.document.documentSupportsThumbnail
import io.esper.android.files.provider.document.isDocumentPath
import io.esper.android.files.provider.ftp.isFtpPath
import io.esper.android.files.provider.linux.isLinuxPath
import io.esper.android.files.settings.Settings
import io.esper.android.files.util.asFileName
import io.esper.android.files.util.isGetPackageArchiveInfoCompatible
import io.esper.android.files.util.isMediaMetadataRetrieverCompatible
import io.esper.android.files.util.valueCompat
import java.text.CollationKey

val FileItem.name: String
    get() = path.name

val FileItem.baseName: String
    get() = if (attributes.isDirectory) name else name.asFileName().baseName

val FileItem.extension: String
    get() = if (attributes.isDirectory) "" else name.asFileName().extensions

fun FileItem.getMimeTypeName(context: Context): String {
        if (attributesNoFollowLinks.isSymbolicLink && isSymbolicLinkBroken) {
            return MimeType.getBrokenSymbolicLinkName(context)
        }
        return mimeType.getName(extension, context)
    }

val FileItem.isArchiveFile: Boolean
    get() = path.isArchiveFile(mimeType)

val FileItem.isListable: Boolean
    get() = attributes.isDirectory || isArchiveFile

val FileItem.listablePath: Path
    get() = if (isArchiveFile) path.createArchiveRootPath() else path

// @see PathAttributesFetcher.fetch
val FileItem.supportsThumbnail: Boolean
    get() {
        if (path.isDocumentPath && attributes.documentSupportsThumbnail) {
            return true
        }
        if (path.isRemotePath) {
            val shouldReadRemotePath = !path.isFtpPath
                && Settings.READ_REMOTE_FILES_FOR_THUMBNAIL.valueCompat
            if (!shouldReadRemotePath) {
                return false
            }
        }
        return when {
            mimeType.isApk && path.isGetPackageArchiveInfoCompatible -> true
            mimeType.isImage -> true
            mimeType.isMedia && path.isMediaMetadataRetrieverCompatible -> true
            mimeType.isPdf && (path.isLinuxPath || path.isDocumentPath) ->
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
                    || Settings.SHOW_PDF_THUMBNAIL_PRE_28.valueCompat
            else -> false
        }
    }

// @see android.content.pm.parsing.ApkLiteParseUtils.parsePackageSplitNames
// @see android.content.pm.parsing.ParsingPackageUtils.validateName
// @see com.android.server.pm.PackageManagerService.getNextCodePath
private const val PACKAGE_NAME_COMPONENT_PATTERN = "[A-Za-z][0-9A-Z_a-z]*"
private const val PACKAGE_NAME_PATTERN =
    "$PACKAGE_NAME_COMPONENT_PATTERN(?:\\.$PACKAGE_NAME_COMPONENT_PATTERN)+"
private const val BASE64_URL_SAFE_CHARACTER_CLASS = "[0-9A-Za-z\\-_]"
private const val BASE64_URL_SAFE_PATTERN = ("(?:$BASE64_URL_SAFE_CHARACTER_CLASS{4})*"
    + "(?:$BASE64_URL_SAFE_CHARACTER_CLASS{3}=|$BASE64_URL_SAFE_CHARACTER_CLASS{2}==)?")
private val APP_DIRECTORY_REGEX =
    Regex("($PACKAGE_NAME_PATTERN)(?:-$BASE64_URL_SAFE_PATTERN)?")

val FileItem.appDirectoryPackageName: String?
    get() {
        if (!attributes.isDirectory) {
            return null
        }
        return APP_DIRECTORY_REGEX.matchEntire(name)?.groupValues?.get(1)
    }

fun FileItem.createDummyArchiveRoot(): FileItem =
    FileItem(
        path.createArchiveRootPath(), DummyCollationKey(), DummyArchiveRootBasicFileAttributes(),
        null, null, false, MimeType.DIRECTORY
    )

// Dummy collation key only to be added to the selection set, which may be used to determine file
// type when confirming deletion.
private class DummyCollationKey : CollationKey("") {
    override fun compareTo(other: CollationKey?): Int {
        throw UnsupportedOperationException()
    }

    override fun toByteArray(): ByteArray {
        throw UnsupportedOperationException()
    }
}

// Dummy attributes only to be added to the selection set, which may be used to determine file
// type when confirming deletion.
private class DummyArchiveRootBasicFileAttributes : BasicFileAttributes {
    override fun lastModifiedTime(): FileTime {
        throw UnsupportedOperationException()
    }

    override fun lastAccessTime(): FileTime {
        throw UnsupportedOperationException()
    }

    override fun creationTime(): FileTime {
        throw UnsupportedOperationException()
    }

    override fun isRegularFile(): Boolean = false

    override fun isDirectory(): Boolean = true

    override fun isSymbolicLink(): Boolean = false

    override fun isOther(): Boolean = false

    override fun size(): Long {
        throw UnsupportedOperationException()
    }

    override fun fileKey(): Any {
        throw UnsupportedOperationException()
    }
}
