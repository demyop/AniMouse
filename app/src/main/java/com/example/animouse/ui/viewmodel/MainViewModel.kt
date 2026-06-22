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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.example.animouse.data.database.AnimeListItemEntity

@HiltViewModel // 1. Говорим, что эта ViewModel использует Hilt
class MainViewModel @Inject constructor(
    private val database: AppDatabase,
    private val repository: AnimeRepository,// 2. Hilt сам принесет нам готовую базу!

) : ViewModel() { // 3. Наследуемся от обычного ViewModel, Application больше не нужен

    // 👇 ДОБАВЛЯЙ СЮДА: Хранилище для детального экрана новости
    var selectedNewsForDetails: com.example.animouse.data.model.AnimeNews? = null

    // Наш репозиторий (по-хорошему, его потом тоже надо будет инжектить через Hilt,
    // но пока оставим так, чтобы не сломать логику)

    private val _allAnime = MutableStateFlow<List<AnimeListItemEntity>>(emptyList())
    val allAnime: StateFlow<List<AnimeListItemEntity>> = _allAnime

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
                _animeStatuses.value = savedAnime.filter { it.status != null }
                    .associate { it.animeId to it.status!! }

                // 2. 👇 ФИКС: Используем тот же мощный поток, что и для Карусели "В этом сезоне"!
                repository.getDiscoveryListFlow("DISCOVERY_TRENDING").collect { entities ->
                    _allAnime.value = entities
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
                val animeFromList = _allAnime.value.find { it.idMal == animeId }

                if (animeFromList != null) {
                    // Если нашли в нашем умном кэше — сохраняем полную карточку
                    val entity = UserAnimeEntity(
                        animeId = animeId,
                        idMal = animeFromList.idMal,
                        status = status,
                        // Берем русское название, а если его пока нет (еще грузится) — берем английское
                        title = animeFromList.titleRussian ?: animeFromList.titleRomaji,
                        posterUrl = animeFromList.posterUrl,
                        score = animeFromList.score,
                        episodesTotal = animeFromList.episodes,
                        episodesAired = 0, // Это поле можно добавить в AnimeListItemEntity позже
                        animeStatus = "RELEASING" // Так как мы тянем онгоинги
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

    // ===================================================================
    // ДАННЫЕ ДЛЯ НОВОГО СУПЕР-ЭКРАНА (DISCOVERY)
    // ===================================================================

    // Наша "коробка", в которой лежит всё для главного экрана
    data class DiscoveryUiState(
        val isLoading: Boolean = true,
        val news: List<com.example.animouse.data.model.AnimeNews> = emptyList(),
        val trending: List<AnimeListItemEntity> = emptyList(),
        val upcoming: List<AnimeListItemEntity> = emptyList(),
        val top100: List<AnimeListItemEntity> = emptyList()
    )

    private val _discoveryState = MutableStateFlow(DiscoveryUiState())
    val discoveryState: StateFlow<DiscoveryUiState> = _discoveryState

    // Этот метод мы вызовем, когда перейдем на вкладку Супер-экрана
    fun loadDiscoveryScreenData() {
        // 1. Отдельно грузим новости (так как мы решили их не кэшировать)
        viewModelScope.launch {
            try {
                val freshNews = repository.getNews()
                _discoveryState.value = _discoveryState.value.copy(news = freshNews)
            } catch (e: Exception) {
                Log.e("MainViewModel", "Ошибка загрузки новостей", e)
            }
        }

        // 2. Подписываемся на Тренды
        viewModelScope.launch {
            repository.getDiscoveryListFlow("DISCOVERY_TRENDING").collect { entities ->
                _discoveryState.value = _discoveryState.value.copy(trending = entities, isLoading = false)
            }
        }

        // 3. Подписываемся на Анонсы
        viewModelScope.launch {
            repository.getDiscoveryListFlow("UPCOMING").collect { entities ->
                _discoveryState.value = _discoveryState.value.copy(upcoming = entities)
            }
        }

        // 4. Подписываемся на Топ-100
        viewModelScope.launch {
            repository.getDiscoveryListFlow("TOP_100").collect { entities ->
                _discoveryState.value = _discoveryState.value.copy(top100 = entities)
            }
        }
    }

    // 👇 Состояние для крутилки на кубике
    val isRandomLoading = androidx.compose.runtime.mutableStateOf(false)

    // Функция броска
// Функция броска
    fun rollRandomAnime(onSuccess: (com.example.animouse.data.model.ShikimoriSearchResult) -> Unit) {
        if (isRandomLoading.value) return // Защита от двойного клика

        // 👇 Теперь код выглядит чисто и аккуратно
        viewModelScope.launch {
            isRandomLoading.value = true
            val randomResult = repository.rollTheDice()
            isRandomLoading.value = false

            if (randomResult != null) {
                onSuccess(randomResult)
            }
        }
    }
    // --- УНИВЕРСАЛЬНЫЕ МЕТОДЫ ДЛЯ ЛЮБОГО ЭКРАНА ---
    // --- УНИВЕРСАЛЬНЫЕ МЕТОДЫ ДЛЯ ЛЮБОГО ЭКРАНА ---
    fun updateAnimeStatusUniversal(anime: com.example.animouse.data.model.Anime, status: String?) {
        viewModelScope.launch {
            if (status == null || status == "NONE") {
                val current = database.userAnimeDao().getAnimeById(anime.id)
                if (current != null) {
                    database.userAnimeDao().insert(current.copy(status = null))
                    database.userAnimeDao().deleteIfUnused(anime.id)
                }
            } else {
                val existing = database.userAnimeDao().getAnimeById(anime.id)

                // БЕЗОПАСНЫЙ МАППИНГ: используем ?. для защиты от null
                val entity = existing?.copy(status = status) ?: UserAnimeEntity(
                    animeId = anime.id,
                    idMal = anime.idMal ?: anime.id,
                    status = status, // (Во втором методе тут будет status = null)
                    title = anime.title.romaji ?: "Без названия",
                    posterUrl = anime.coverImage.large,

                    // 👇 БЕЗОПАСНЫЙ INT (Если оценки нет, ставим 0)
                    score = anime.averageScore ?: 0,

                    episodesTotal = anime.episodes ?: 0,
                    episodesAired = 0,
                    animeStatus = anime.status ?: "RELEASING"
                )
                database.userAnimeDao().insert(entity)
            }
            updateLocalStatuses()
        }
    }

    fun toggleAnimeInCustomListUniversal(anime: com.example.animouse.data.model.Anime, listId: Int, isAdding: Boolean) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            if (isAdding) {
                val existing = database.userAnimeDao().getAnimeById(anime.id)
                if (existing == null) {
                    // 👈 ТУТ БЫЛА ОШИБКА: Добавили обязательный status = null
                    if (existing == null) {
                        val entity = UserAnimeEntity(
                            animeId = anime.id,
                            idMal = anime.idMal ?: anime.id,
                            status = null, // 👈 Тут обязательно null для "болванки"
                            title = anime.title.romaji ?: "Без названия",
                            posterUrl = anime.coverImage.large,

                            // 👇 То же самое лекарство:
                            score = anime.averageScore ?: 0,

                            episodesTotal = anime.episodes ?: 0,
                            episodesAired = 0,
                            animeStatus = anime.status ?: "RELEASING"
                        )
                        database.userAnimeDao().insert(entity)
                        }
                }
                database.customListDao().addAnimeToList(com.example.animouse.data.database.AnimeCustomListCrossRef(anime.id, listId))
            } else {
                database.customListDao().removeAnimeFromList(com.example.animouse.data.database.AnimeCustomListCrossRef(anime.id, listId))
            }
            updateLocalStatuses()
        }
    }
}