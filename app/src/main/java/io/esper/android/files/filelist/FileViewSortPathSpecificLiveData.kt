package io.esper.android.files.filelist

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import java8.nio.file.Path
import io.esper.android.files.settings.PathSettings
import io.esper.android.files.settings.SettingLiveData
import io.esper.android.files.settings.Settings
import io.esper.android.files.util.valueCompat

class FileViewSortPathSpecificLiveData(pathLiveData: LiveData<Path>) : MediatorLiveData<Boolean>() {
    private lateinit var pathViewTypeLiveData: SettingLiveData<FileViewType?>
    private lateinit var pathSortOptionsLiveData: SettingLiveData<FileSortOptions?>

    private fun loadValue() {
        val value = pathViewTypeLiveData.value != null || pathSortOptionsLiveData.value != null
        if (this.value != value) {
            this.value = value
        }
    }

    fun putValue(value: Boolean) {
        if (value) {
            if (pathViewTypeLiveData.value == null) {
                pathViewTypeLiveData.putValue(Settings.FILE_LIST_VIEW_TYPE.valueCompat)
            }
            if (pathSortOptionsLiveData.value == null) {
                pathSortOptionsLiveData.putValue(Settings.FILE_LIST_SORT_OPTIONS.valueCompat)
            }
        } else {
            if (pathViewTypeLiveData.value != null) {
                pathViewTypeLiveData.putValue(null)
            }
            if (pathSortOptionsLiveData.value != null) {
                pathSortOptionsLiveData.putValue(null)
            }
        }
    }

    init {
        addSource(pathLiveData) { path: Path ->
            if (this::pathViewTypeLiveData.isInitialized) {
                removeSource(pathViewTypeLiveData)
            }
            if (this::pathSortOptionsLiveData.isInitialized) {
                removeSource(pathSortOptionsLiveData)
            }
            pathViewTypeLiveData = PathSettings.getFileListViewType(path)
            pathSortOptionsLiveData = PathSettings.getFileListSortOptions(path)
            addSource(pathViewTypeLiveData) { loadValue() }
            addSource(pathSortOptionsLiveData) { loadValue() }
        }
    }
}
