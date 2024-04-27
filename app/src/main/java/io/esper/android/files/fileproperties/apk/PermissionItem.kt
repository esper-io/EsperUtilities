package io.esper.android.files.fileproperties.apk

import android.content.pm.PermissionInfo

class PermissionItem(
    val name: String,
    val permissionInfo: PermissionInfo?,
    val label: String?,
    val description: String?
)
