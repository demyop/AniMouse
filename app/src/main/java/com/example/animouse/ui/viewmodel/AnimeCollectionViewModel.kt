package com.example.animouse.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.animouse.data.database.AnimeListItemEntity
import com.example.animouse.data.repository.AnimeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AnimeCollectionViewModel @Inject constructor(
    private val repository: AnimeRepository
) : ViewModel() {

    private val _animeList = MutableStateFlow<List<AnimeListItemEntity>>(emptyList())
    val animeList: StateFlow<List<AnimeListItemEntity>> = _animeList

    val isLoading = MutableStateFlow(true)

    fun loadCollection(listType: String) {
        viewModelScope.launch {
            isLoading.value = true
            // Подписываемся на нужный список (Анонсы или Топ-100)
            repository.getDiscoveryListFlow(listType).collect { entities ->
                _animeList.value = entities
                isLoading.value = false
            }
        }
    }
}