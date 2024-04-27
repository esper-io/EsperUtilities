package io.esper.android.files.util

fun Any.hash(vararg values: Any?): Int = values.contentDeepHashCode()
