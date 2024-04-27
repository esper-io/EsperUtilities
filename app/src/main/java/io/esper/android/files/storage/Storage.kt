package io.esper.android.files.storage

import android.content.Context
import android.content.Intent
import android.os.Parcelable
import androidx.annotation.DrawableRes
import java8.nio.file.Path
import io.esper.android.files.R
import io.esper.android.files.util.takeIfNotEmpty

abstract class Storage : Parcelable {
    abstract val id: Long

    open val iconRes: Int
        @DrawableRes
        get() = R.drawable.directory_icon_white_24dp

    abstract val customName: String?

    abstract fun getDefaultName(context: Context): String

    fun getName(context: Context): String = customName?.takeIfNotEmpty() ?: getDefaultName(context)

    abstract val description: String

    abstract val path: Path?

    open val linuxPath: String? = null

    open val isVisible: Boolean = true

    open fun createIntent(): Intent? = null

    abstract fun createEditIntent(): Intent
}
