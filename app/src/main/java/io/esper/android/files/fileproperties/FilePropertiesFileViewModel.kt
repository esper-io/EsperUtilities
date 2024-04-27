package io.esper.android.files.fileproperties

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import io.esper.android.files.file.FileItem
import io.esper.android.files.util.Stateful

class FilePropertiesFileViewModel(file: FileItem) : ViewModel() {
    private val _fileLiveData = FileLiveData(file)
    val fileLiveData: LiveData<Stateful<FileItem>>
        get() = _fileLiveData

    fun reload() {
        _fileLiveData.loadValue()
    }

    override fun onCleared() {
        _fileLiveData.close()
    }
}
