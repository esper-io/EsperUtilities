package io.esper.android.files.provider.sftp

import java8.nio.file.StandardOpenOption
import io.esper.android.files.provider.common.OpenOptions
import io.esper.android.files.util.enumSetOf
import net.schmizz.sshj.sftp.OpenMode

internal fun OpenOptions.toSftpFlags(): Set<OpenMode> =
    enumSetOf<OpenMode>().apply {
        if (read && write) {
            this += OpenMode.READ
            this += OpenMode.WRITE
        } else if (write) {
            this += OpenMode.WRITE
        } else {
            this += OpenMode.READ
        }
        if (append) {
            this += OpenMode.APPEND
        }
        if (truncateExisting) {
            this += OpenMode.TRUNC
        }
        if (createNew) {
            this += OpenMode.CREAT
            this += OpenMode.EXCL
        } else if (create) {
            this += OpenMode.CREAT
        }
        if (deleteOnClose) {
            throw UnsupportedOperationException(StandardOpenOption.DELETE_ON_CLOSE.toString())
        }
        if (sync) {
            throw UnsupportedOperationException(StandardOpenOption.SYNC.toString())
        }
        if (dsync) {
            throw UnsupportedOperationException(StandardOpenOption.DSYNC.toString())
        }
    }
