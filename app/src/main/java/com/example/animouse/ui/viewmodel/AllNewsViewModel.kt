package com.example.animouse.ui.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.animouse.data.model.AnimeNews
import com.example.animouse.data.repository.AnimeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AllNewsViewModel @Inject constructor(
    private val repository: AnimeRepository
) : ViewModel() {

    // Список новостей (используем Compose State для мгновенной перерисовки)
    val newsList = mutableStateListOf<AnimeNews>()

    val isLoading = mutableStateOf(false)
    private var currentPage = 1
    private var isLastPage = false

    init {
        loadNextPage() // Загружаем первую страницу при открытии экрана
    }

    fun loadNextPage() {
        // Если уже грузим или достигли конца — ничего не делаем
        if (isLoading.value || isLastPage) return

        viewModelScope.launch {
            isLoading.value = true

            val newItems = repository.getNews(page = currentPage)

            if (newItems.isEmpty()) {
                isLastPage = true // Шикимори вернул пустой список, значит новости кончились
            } else {
                newsList.addAll(newItems)
                currentPage++ // Готовимся к следующей странице
            }

            isLoading.value = false
        }
    }
}