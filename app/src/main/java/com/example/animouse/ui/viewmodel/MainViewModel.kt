package com.example.animouse.ui.activity

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.animouse.data.database.AppDatabase
import com.example.animouse.data.database.UserAnimeEntity
import com.example.animouse.data.model.Anime
import com.example.animouse.data.repository.AnimeRepository
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    // Инициализируем обновленную БД с поддержкой деструктивной миграции
    private val database = Room.databaseBuilder(
        application,
        AppDatabase::class.java,
        "animouse_db"
    ).fallbackToDestructiveMigration().build()

    private val repository = AnimeRepository()

    private val _allAnime = MutableLiveData<List<Anime>>(emptyList())
    val allAnime: LiveData<List<Anime>> = _allAnime

    private val _localAnime = MutableLiveData<List<com.example.animouse.data.database.UserAnimeEntity>>()
    val localAnime: LiveData<List<com.example.animouse.data.database.UserAnimeEntity>> = _localAnime

    // Теперь храним карту: ID аниме -> Его статус ("WATCHING", "PLANNED" и т.д.)
    private val _animeStatuses = MutableLiveData<Map<Int, String>>(emptyMap())
    val animeStatuses: LiveData<Map<Int, String>> = _animeStatuses

    init {
        loadData()
        updateLocalStatuses() // <-- Загрузит кастомные списки и плашки прямо на старте!
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
                    val entity = com.example.animouse.data.database.UserAnimeEntity(
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
        // Просто вызываем наш умный метод с null, он сам всё удалит и обновит UI!
        setAnimeStatus(animeId, null)
    }

    // Шпаргалка 1: ID кастомной папки -> Список аниме в ней (чтобы открывать папки)
    private val _customFolderAnime = MutableLiveData<Map<Int, List<com.example.animouse.data.database.UserAnimeEntity>>>()
    val customFolderAnime: LiveData<Map<Int, List<com.example.animouse.data.database.UserAnimeEntity>>> = _customFolderAnime

    // Шпаргалка 2: ID аниме -> Превью кастомного списка (чтобы рисовать цветные плашки на карточках)
// Меняем Map<Int, CustomFolderPreview> на Map<Int, List<CustomFolderPreview>>
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

// 2. НОВОЕ: Собираем кастомные списки и генерируем превью для папок
            val allCustomLists = database.customListDao().getAllLists()

            val previews = mutableListOf<CustomFolderPreview>()
            val folderAnimeMap = mutableMapOf<Int, List<com.example.animouse.data.database.UserAnimeEntity>>()
            val badgeMap = mutableMapOf<Int, MutableList<CustomFolderPreview>>() // Теперь это список

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

    // Добавь это куда-нибудь в MainViewModel
    fun refreshFavorites() {
        viewModelScope.launch {
            updateLocalStatuses() // Этот скрытый метод у нас уже есть в самом низу
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
            updateLocalStatuses() // Мгновенно перерисовываем папки на главном экране
        }
    }

    fun updateCustomList(listId: Int, newName: String, newColorHex: String) {
        viewModelScope.launch {
            val updatedList = com.example.animouse.data.database.CustomListEntity(
                id = listId, // Привязываемся к старому ID, чтобы обновить, а не создать новый
                name = newName,
                colorHex = newColorHex
            )
            database.customListDao().insertList(updatedList)
            updateLocalStatuses()
        }
    }

}


