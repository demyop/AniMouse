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
                _animeStatuses.value = savedAnime.associate { it.animeId to it.status }

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
    fun setAnimeStatus(animeId: Int, status: String) {
        viewModelScope.launch {
            database.userAnimeDao().insert(UserAnimeEntity(animeId, status))
            updateLocalStatuses()
        }
    }

    // Метод для полного удаления тайтла из всех списков
    fun removeAnimeFromLists(animeId: Int) {
        viewModelScope.launch {
            database.userAnimeDao().deleteById(animeId)
            updateLocalStatuses()
        }
    }

    // Вспомогательный метод для обновления LiveData из базы
    private suspend fun updateLocalStatuses() {
        val savedAnime = database.userAnimeDao().getAll()
        _animeStatuses.value = savedAnime.associate { it.animeId to it.status }
    }

    // Добавь это куда-нибудь в MainViewModel
    fun refreshFavorites() {
        viewModelScope.launch {
            updateLocalStatuses() // Этот скрытый метод у нас уже есть в самом низу
        }
    }

}


