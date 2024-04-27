package io.esper.android.files.util

import android.app.StatusBarManager
import android.content.ComponentName
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import io.esper.android.files.app.application
import io.esper.android.files.app.packageManager
import java.util.concurrent.Executor

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
fun StatusBarManager.requestAddTileService(
    serviceClass: Class<out TileService>,
    resultExecutor: Executor,
    resultCallback: (Int) -> Unit
) {
    val application = application
    val componentName = ComponentName(application, serviceClass)
    val packageManager = packageManager
    val serviceInfo = packageManager.getServiceInfo(
        componentName,
        PackageManager.MATCH_DIRECT_BOOT_AWARE or PackageManager.MATCH_DIRECT_BOOT_UNAWARE
    )
    val label = serviceInfo.loadLabel(packageManager)
    val icon = Icon.createWithResource(application, serviceInfo.iconResource)
    requestAddTileService(componentName, label, icon, resultExecutor, resultCallback)
}
