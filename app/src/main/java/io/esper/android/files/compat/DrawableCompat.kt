package io.esper.android.files.compat

import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt
import androidx.core.graphics.drawable.DrawableCompat

var Drawable.layoutDirectionCompat: Int
    get() = DrawableCompat.getLayoutDirection(this)
    set(value) {
        DrawableCompat.setLayoutDirection(this, value)
    }

fun Drawable.setTintCompat(@ColorInt tint: Int) {
    DrawableCompat.setTint(this, tint)
}
