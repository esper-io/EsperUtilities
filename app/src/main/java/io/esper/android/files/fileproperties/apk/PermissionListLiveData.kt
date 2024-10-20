package io.esper.android.files.fileproperties.apk

import android.os.AsyncTask
import androidx.lifecycle.MutableLiveData
import io.esper.android.files.app.packageManager
import io.esper.android.files.util.Failure
import io.esper.android.files.util.Loading
import io.esper.android.files.util.Stateful
import io.esper.android.files.util.Success
import io.esper.android.files.util.getPermissionInfoOrNull
import io.esper.android.files.util.valueCompat

class PermissionListLiveData(
    private val permissionNames: Array<String>
) : MutableLiveData<Stateful<List<PermissionItem>>>() {
    init {
        loadValue()
    }

    private fun loadValue() {
        value = Loading(value?.value)
        AsyncTask.THREAD_POOL_EXECUTOR.execute {
            val value = try {
                val permissions = permissionNames.map { name ->
                    val packageManager = packageManager
                    val permissionInfo = packageManager.getPermissionInfoOrNull(name, 0)
                    val label = permissionInfo?.loadLabel(packageManager)?.toString()
                        .takeIf { it != name }
                    val description = permissionInfo?.loadDescription(packageManager)?.toString()
                    PermissionItem(name, permissionInfo, label, description)
                }
                Success(permissions)
            } catch (e: Exception) {
                Failure(valueCompat.value, e)
            }
            postValue(value)
        }
    }
}
