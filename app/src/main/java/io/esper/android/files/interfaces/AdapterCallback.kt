package io.esper.android.files.interfaces

import android.content.Context
import io.esper.appstore.model.AppData

interface AdapterCallback {
    fun onMethodCallback(newArray: MutableList<AppData>)
}