package io.esper.android.files.util

import android.net.Uri
import io.esper.android.files.app.contentResolver

fun Uri.takePersistablePermission(modeFlags: Int): Boolean =
    try {
        contentResolver.takePersistableUriPermission(this, modeFlags)
        true
    } catch (e: SecurityException) {
        e.printStackTrace()
        false
    }

fun Uri.releasePersistablePermission(modeFlags: Int): Boolean =
    try {
        contentResolver.releasePersistableUriPermission(this, modeFlags)
        true
    } catch (e: SecurityException) {
        e.printStackTrace()
        false
    }
