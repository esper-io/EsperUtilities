package io.esper.android.files.navigation

import androidx.lifecycle.MediatorLiveData
import io.esper.android.files.settings.Settings
import io.esper.android.files.storage.StorageVolumeListLiveData

object NavigationItemBottomListLiveData : MediatorLiveData<List<NavigationItem?>>() {
    init {
        loadValue()
    }

    private fun loadValue() {
        value = navigationBottomListItems
    }
}
