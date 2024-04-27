package io.esper.android.files.provider.archive

import java8.nio.file.Path
import java8.nio.file.attribute.FileAttributeView
import io.esper.android.files.file.MimeType
import io.esper.android.files.file.guessFromPath
import io.esper.android.files.provider.common.PosixFileStore
import io.esper.android.files.provider.common.size
import java.io.IOException

internal class ArchiveFileStore(private val archiveFile: Path) : PosixFileStore() {
    override fun refresh() {}

    override fun name(): String = archiveFile.toString()

    override fun type(): String = MimeType.guessFromPath(archiveFile.toString()).value

    override fun isReadOnly(): Boolean = true

    @Throws(IOException::class)
    override fun setReadOnly(readOnly: Boolean) {
        throw UnsupportedOperationException()
    }

    @Throws(IOException::class)
    override fun getTotalSpace(): Long = archiveFile.size()

    override fun getUsableSpace(): Long = 0

    override fun getUnallocatedSpace(): Long = 0

    override fun supportsFileAttributeView(type: Class<out FileAttributeView>): Boolean =
        ArchiveFileSystemProvider.supportsFileAttributeView(type)

    override fun supportsFileAttributeView(name: String): Boolean =
        name in ArchiveFileAttributeView.SUPPORTED_NAMES
}
