package io.esper.android.files.fileproperties.apk

import androidx.lifecycle.ViewModel

class PermissionListViewModel(permissionNames: Array<String>) : ViewModel() {
    val permissionListLiveData = PermissionListLiveData(permissionNames)
}
