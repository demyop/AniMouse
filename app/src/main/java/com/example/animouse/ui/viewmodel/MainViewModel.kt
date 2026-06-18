package com.example.animouse.ui.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.animouse.data.database.AppDatabase
import com.example.animouse.data.database.UserAnimeEntity
import com.example.animouse.data.model.Anime
import com.example.animouse.data.repository.AnimeRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel // 1. Говорим, что эта ViewModel использует Hilt
class MainViewModel @Inject constructor(
    private val database: AppDatabase // 2. Hilt сам принесет нам готовую базу!
) : ViewModel() { // 3. Наследуемся от обычного ViewModel, Application больше не нужен

    // Наш репозиторий (по-хорошему, его потом тоже надо будет инжектить через Hilt,
    // но пока оставим так, чтобы не сломать логику)
    private val repository = AnimeRepository()

    private val _allAnime = MutableLiveData<List<Anime>>(emptyList())
    val allAnime: LiveData<List<Anime>> = _allAnime

    private val _localAnime = MutableLiveData<List<UserAnimeEntity>>()
    val localAnime: LiveData<List<UserAnimeEntity>> = _localAnime

    // Теперь храним карту: ID аниме -> Его статус ("WATCHING", "PLANNED" и т.д.)
    private val _animeStatuses = MutableLiveData<Map<Int, String>>(emptyMap())
    val animeStatuses: LiveData<Map<Int, String>> = _animeStatuses

    init {
        loadData()
        updateLocalStatuses() // Загрузит кастомные списки и плашки прямо на старте
    }

    private fun loadData() {
        viewModelScope.launch {
            try {
                // 1. Загружаем все сохраненные пользователем тайтлы
                val savedAnime = database.userAnimeDao().getAll()
                // Превращаем список в Map для мгновенного поиска по ID
                _animeStatuses.value = savedAnime.filter { it.status != null }
                    .associate { it.animeId to it.status!! }

                // 2. Загружаем онгоинги из сети
                val response = repository.getAnimeList()
                if (response.isSuccessful) {
                    _allAnime.value = response.body()?.data?.Page?.media ?: emptyList()
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", e.message ?: "Unknown error")
            }
        }
    }

    // Метод для изменения статуса или добавления в список
    fun setAnimeStatus(animeId: Int, status: String?) {
        viewModelScope.launch {
            if (status == null || status == "NONE") {
                // Логика удаления
                val current = database.userAnimeDao().getAnimeById(animeId)
                if (current != null) {
                    database.userAnimeDao().insert(current.copy(status = null))
                    database.userAnimeDao().deleteIfUnused(animeId)
                }
            } else {
                // Пытаемся найти аниме в текущем загруженном списке главного экрана
                val animeFromList = _allAnime.value?.find { it.id == animeId }

                if (animeFromList != null) {
                    // Если нашли в сети — сохраняем полную карточку
                    val entity = UserAnimeEntity(
                        animeId = animeId,
                        idMal = animeFromList.idMal ?: -1,
                        status = status,
                        title = animeFromList.title.romaji ?: "Без названия",
                        posterUrl = animeFromList.coverImage.large,
                        score = animeFromList.averageScore ?: 0,
                        episodesTotal = animeFromList.episodes ?: 0,
                        episodesAired = animeFromList.nextAiringEpisode?.episode?.minus(1) ?: 0,
                        animeStatus = animeFromList.status
                    )
                    database.userAnimeDao().insert(entity)
                } else {
                    // Если вдруг аниме нет в списке, но оно есть в БД (просто обновляем статус)
                    val existing = database.userAnimeDao().getAnimeById(animeId)
                    if (existing != null) {
                        database.userAnimeDao().insert(existing.copy(status = status))
                    }
                }
            }
            updateLocalStatuses() // Обновляем UI
        }
    }

    // Метод для полного удаления тайтла из всех списков
    fun removeAnimeFromLists(animeId: Int) {
        setAnimeStatus(animeId, null)
    }

    // Шпаргалка 1: ID кастомной папки -> Список аниме в ней
    private val _customFolderAnime = MutableLiveData<Map<Int, List<UserAnimeEntity>>>()
    val customFolderAnime: LiveData<Map<Int, List<UserAnimeEntity>>> = _customFolderAnime

    // Шпаргалка 2: ID аниме -> Список превью кастомных списков
    private val _animeCustomBadges = MutableLiveData<Map<Int, List<CustomFolderPreview>>>()
    val animeCustomBadges: LiveData<Map<Int, List<CustomFolderPreview>>> = _animeCustomBadges

    // Вспомогательный метод для обновления LiveData из базы
    fun updateLocalStatuses() {
        viewModelScope.launch {
            // 1. Получаем вообще все сохраненные тайтлы из базы
            val savedAnime = database.userAnimeDao().getAll()

            _localAnime.value = savedAnime

            _animeStatuses.value = savedAnime.filter { it.status != null }
                .associate { it.animeId to it.status!! }

            // 2. Собираем кастомные списки и генерируем превью для папок
            val allCustomLists = database.customListDao().getAllLists()

            val previews = mutableListOf<CustomFolderPreview>()
            val folderAnimeMap = mutableMapOf<Int, List<UserAnimeEntity>>()
            val badgeMap = mutableMapOf<Int, MutableList<CustomFolderPreview>>()

            for (customList in allCustomLists) {
                val animeInList = database.customListDao().getAnimeInList(customList.id)
                val preview = CustomFolderPreview(customList.id, customList.name, customList.colorHex, animeInList.size, animeInList.randomOrNull()?.posterUrl)

                previews.add(preview)
                folderAnimeMap[customList.id] = animeInList

                // Добавляем плашку в список для конкретного аниме
                animeInList.forEach { entity ->
                    if (badgeMap[entity.animeId] == null) {
                        badgeMap[entity.animeId] = mutableListOf()
                    }
                    badgeMap[entity.animeId]?.add(preview)
                }
            }

            _customFolderPreviews.value = previews
            _customFolderAnime.value = folderAnimeMap
            _animeCustomBadges.value = badgeMap
        }
    }

    fun refreshFavorites() {
        viewModelScope.launch {
            updateLocalStatuses()
        }
    }

    // --- ДАННЫЕ ДЛЯ КАСТОМНЫХ ПАПОК ---
    data class CustomFolderPreview(
        val id: Int,
        val name: String,
        val colorHex: String,
        val count: Int,
        val randomPosterUrl: String?
    )

    private val _customFolderPreviews = MutableLiveData<List<CustomFolderPreview>>()
    val customFolderPreviews: LiveData<List<CustomFolderPreview>> = _customFolderPreviews

    fun deleteCustomList(listId: Int) {
        viewModelScope.launch {
            database.customListDao().deleteList(listId)
            updateLocalStatuses()
        }
    }

    fun updateCustomList(listId: Int, newName: String, newColorHex: String) {
        viewModelScope.launch {
            val updatedList = com.example.animouse.data.database.CustomListEntity(
                id = listId,
                name = newName,
                colorHex = newColorHex
            )
            database.customListDao().insertList(updatedList)
            updateLocalStatuses()
        }
    }

    fun toggleAnimeInCustomList(animeId: Int, listId: Int, isAdding: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val crossRef = com.example.animouse.data.database.AnimeCustomListCrossRef(animeId, listId)
            if (isAdding) {
                database.customListDao().addAnimeToList(crossRef)
            } else {
                database.customListDao().removeAnimeFromList(crossRef)
            }
            updateLocalStatuses()
        }
    }

    fun createCustomList(name: String, colorHex: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val newList = com.example.animouse.data.database.CustomListEntity(name = name, colorHex = colorHex)
            database.customListDao().insertList(newList)
            updateLocalStatuses()
        }
    }
}