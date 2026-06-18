package com.example.animouse.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.animouse.data.api.ShikimoriApi
import com.example.animouse.data.model.ShikimoriSearchResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val shikimoriApi: ShikimoriApi // 👈 Hilt сам подставит сюда клиент из AppModule!
) : ViewModel() {

    private val _searchResults = MutableLiveData<List<ShikimoriSearchResult>>()
    val searchResults: LiveData<List<ShikimoriSearchResult>> = _searchResults

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private var searchJob: Job? = null

    fun searchAnime(query: String) {
        searchJob?.cancel() // Отменяем предыдущий поиск, если юзер продолжает печатать

        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }

        searchJob = viewModelScope.launch {
            delay(500) // Ждем полсекунды после последнего нажатия
            _isLoading.value = true
            try {
                val results = shikimoriApi.searchAnime(query)
                _searchResults.value = results
            } catch (e: Exception) {
                _searchResults.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
}