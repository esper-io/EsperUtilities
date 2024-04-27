package io.esper.android.files.terminal

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import io.esper.android.files.app.packageManager
import io.esper.android.files.util.startActivitySafe

object Terminal {
    fun open(path: String, context: Context) {
        val componentName =
            packageManager.queryIntentActivities(Intent(Intent.ACTION_SEND).setType("*/*"), 0)
                .firstOrNull { it.activityInfo.name.endsWith(".TermHere") }?.activityInfo
                ?.let { ComponentName(it.packageName, it.name) }
                ?: ComponentName("jackpal.androidterm", "jackpal.androidterm.TermHere")
        val intent = Intent()
            .setComponent(componentName)
            .setAction(Intent.ACTION_SEND)
            .putExtra(Intent.EXTRA_STREAM, Uri.parse(path))
        context.startActivitySafe(intent)
    }
}
