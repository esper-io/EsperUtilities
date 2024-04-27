package io.esper.android.files.provider.common

import java8.nio.file.attribute.FileTime
import org.threeten.bp.Instant
import kotlin.reflect.KClass

val KClass<FileTime>.EPOCH: FileTime
    get() = FileTime.from(Instant.EPOCH)
