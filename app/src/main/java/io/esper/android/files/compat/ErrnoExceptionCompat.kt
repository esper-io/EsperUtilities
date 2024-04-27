package io.esper.android.files.compat

import android.system.ErrnoException
import io.esper.android.files.hiddenapi.RestrictedHiddenApi
import io.esper.android.files.util.lazyReflectedField

@RestrictedHiddenApi
private val functionNameField by lazyReflectedField(ErrnoException::class.java, "functionName")

val ErrnoException.functionNameCompat: String
    get() = functionNameField.get(this) as String
