package com.example.animouse

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

    // Вспомогательный метод для обновления LiveData из базы
    fun updateLocalStatuses() {
        viewModelScope.launch {
            // Получаем вообще все сохраненные тайтлы из базы
            val savedAnime = database.userAnimeDao().getAll()

            // 1. Отдаем список локальных карточек в MainActivity
            _localAnime.value = savedAnime

            // 2. Отдаем карту статусов (уже с исправленным фильтром null, который мы делали ранее)
            _animeStatuses.value = savedAnime.filter { it.status != null }
                .associate { it.animeId to it.status!! }
        }
    }

    // Добавь это куда-нибудь в MainViewModel
    fun refreshFavorites() {
        viewModelScope.launch {
            updateLocalStatuses() // Этот скрытый метод у нас уже есть в самом низу
        }
    }

}


