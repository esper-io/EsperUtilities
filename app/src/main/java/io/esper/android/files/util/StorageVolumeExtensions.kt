package io.esper.android.files.util

import android.os.storage.StorageVolume
import io.esper.android.files.compat.directoryCompat

val StorageVolume.isMounted: Boolean
    get() = directoryCompat != null
