package io.esper.android.files.viewmodel

import android.app.Application
import androidx.lifecycle.*
import io.esper.android.files.data.ContentDb
import io.esper.android.files.model.AllContent
import io.esper.android.files.repository.ContentRepository
import kotlinx.coroutines.launch

class DlcViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ContentRepository
    val allContent: LiveData<List<AllContent>>

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    init {
        val contentDao = ContentDb.getDatabase(application).contentDao()
        repository = ContentRepository(application, contentDao)
        allContent = repository.allContent

        // Fetch data from the database and display it immediately
        fetchContentFromDatabase()
        // Update the database in the background
        fetchContentFromNetwork()
    }

    private fun fetchContentFromDatabase() {
        // allContent is already observing the database via LiveData
        // No additional code needed here
    }

    private fun fetchContentFromNetwork() {
        _isLoading.value = true
        viewModelScope.launch {
            repository.fetchContentFromNetwork()
            _isLoading.value = false
        }
    }

    // Optionally, provide a method to manually refresh the data
    fun refreshContent() {
        fetchContentFromNetwork()
    }
}
