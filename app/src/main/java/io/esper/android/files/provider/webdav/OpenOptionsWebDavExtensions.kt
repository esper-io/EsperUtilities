package io.esper.android.files.provider.webdav

import java8.nio.file.StandardOpenOption
import io.esper.android.files.provider.common.OpenOptions

internal fun OpenOptions.checkForWebDav() {
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
