package io.esper.android.files.compat

import android.app.Activity
import android.app.ActivityManager.TaskDescription
import android.graphics.Color
import android.os.Build
import androidx.annotation.StyleRes
import androidx.core.app.ActivityCompat
import io.esper.android.files.util.getColorByAttr

fun Activity.recreateCompat() {
    ActivityCompat.recreate(this)
}

fun Activity.setThemeCompat(@StyleRes resid: Int) {
    setTheme(resid)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        val surfaceColor = getColorByAttr(com.google.android.material.R.attr.colorSurface)
        if (surfaceColor != 0 && Color.alpha(surfaceColor) == 0xFF) {
            @Suppress("DEPRECATION")
            setTaskDescription(TaskDescription(null, null, surfaceColor))
        }
    }
}
