package io.esper.android.files.provider.document

import android.provider.DocumentsContract
import java8.nio.file.ProviderMismatchException
import java8.nio.file.attribute.BasicFileAttributes
import io.esper.android.files.util.hasBits

val BasicFileAttributes.documentSupportsThumbnail: Boolean
    get() {
        this as? DocumentFileAttributes ?: throw ProviderMismatchException(toString())
        return flags().hasBits(DocumentsContract.Document.FLAG_SUPPORTS_THUMBNAIL)
    }
