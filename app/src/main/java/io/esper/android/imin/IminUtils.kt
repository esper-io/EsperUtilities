package io.esper.android.imin

import android.hardware.display.DisplayManager
import android.util.Log
import android.view.Display

object IminUtils {
    private const val TAG = "IminUtils"

    fun getPresentationDisplay(displayManager: DisplayManager): Display? {
        val displays = displayManager.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION)
        return displays.firstOrNull { display ->
            val isSecure = display.flags and Display.FLAG_SECURE != 0
            val supportsProtectedBuffers =
                display.flags and Display.FLAG_SUPPORTS_PROTECTED_BUFFERS != 0
            val isPresentation = display.flags and Display.FLAG_PRESENTATION != 0
            isSecure && supportsProtectedBuffers && isPresentation
        }?.also {
            Log.i(TAG, "getPresentationDisplay: found display $it")
        }
    }
}
