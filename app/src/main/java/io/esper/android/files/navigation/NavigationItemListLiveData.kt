package io.esper.android.files.navigation

import androidx.lifecycle.MediatorLiveData
import io.esper.android.files.settings.Settings
import io.esper.android.files.storage.StorageVolumeListLiveData

object NavigationItemListLiveData : MediatorLiveData<List<NavigationItem?>>() {
    init {
        // Initialize value before we have any active observer.
        loadValue()
        addSource(Settings.STORAGES) { loadValue() }
        addSource(StorageVolumeListLiveData) { loadValue() }
    }

    private fun loadValue() {
        value = navigationItems
    }
}
