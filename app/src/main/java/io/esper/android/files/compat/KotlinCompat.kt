package io.esper.android.files.compat

import kotlin.comparisons.reversed as kotlinReversed

fun <T> Comparator<T>.reversedCompat(): Comparator<T> = kotlinReversed()
