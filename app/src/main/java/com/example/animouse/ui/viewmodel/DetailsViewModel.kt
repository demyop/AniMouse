package com.example.animouse.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.animouse.data.api.AniListApi
import com.example.animouse.data.api.GraphQLRequest
import com.example.animouse.data.api.ShikimoriApi
import com.example.animouse.data.database.AppDatabase
import com.example.animouse.data.model.ShikimoriAnimeDetails
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import android.util.Log
import com.example.animouse.data.database.AnimeDetailsEntity
import com.example.animouse.data.repository.AnimeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import com.example.animouse.di.AppModule

@HiltViewModel
class DetailsViewModel @Inject constructor(
    private val repository: AnimeRepository,
    private val database: AppDatabase
) : ViewModel() {

    // 1. Используем StateFlow (Реактивный подход)
    private val _animeDetails = MutableStateFlow<AnimeDetailsEntity?>(null)
    val animeDetails: StateFlow<AnimeDetailsEntity?> = _animeDetails

    // Метод подписки (вызывай его из onCreate вместо старого loadAnimeDetails)
    fun observeAnimeDetails(idMal: Int) {
        viewModelScope.launch {
            repository.getAnimeDetails(idMal).collectLatest { entity ->
                _animeDetails.value = entity
            }
        }
    }

    // --- 1. ЛОКАЛЬНАЯ БАЗА (Списки пользователя) ---
    private val _currentStatus = MutableLiveData<String?>()
    val currentStatus: LiveData<String?> = _currentStatus

    fun loadStatus(animeId: Int) {
        viewModelScope.launch {
            val entity = database.userAnimeDao().getAnimeById(animeId)
            _currentStatus.value = entity?.status
        }
    }

    fun updateStatus(
        animeId: Int,
        idMal: Int,
        userFolderStatus: String?, // Это папка (Смотрю/В планах)
        title: String,
        posterUrl: String?,
        score: Int,
        epTotal: Int,
        epAired: Int,
        releaseStatus: String?,    // Это статус (Вышло/Онгоинг)
        season: String?,
        seasonYear: Int?
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            if (userFolderStatus == null || userFolderStatus == "NONE") {
                val current = database.userAnimeDao().getAnimeById(animeId)
                if (current != null) {
                    // Обнуляем именно ПАПКУ (в твоей БД это поле status)
                    database.userAnimeDao().insert(current.copy(status = null))
                    database.userAnimeDao().deleteIfUnused(animeId)
                }
            } else {
                val entity = com.example.animouse.data.database.UserAnimeEntity(
                    animeId = animeId,
                    idMal = idMal,
                    status = userFolderStatus,      // <-- ПАПКА! ("WATCHING")
                    title = title,
                    posterUrl = posterUrl,
                    score = score,
                    episodesTotal = epTotal,
                    episodesAired = epAired,
                    animeStatus = releaseStatus,    // <-- РЕЛИЗ! ("FINISHED")
                    season = season,
                    seasonYear = seasonYear
                )
                database.userAnimeDao().insert(entity)
            }

            loadStatus(animeId)
        }
    }

    // --- 3. СЕТЬ (Доп. данные из AniList: Трейлер и Связанное) ---
    private val _aniListExtra =
        MutableLiveData<com.example.animouse.data.model.AniListExtraMedia?>()
    val aniListExtra: LiveData<com.example.animouse.data.model.AniListExtraMedia?> = _aniListExtra

    // В DetailsViewModel
    fun loadAniListExtra(animeId: Int, idMal: Int) {
        viewModelScope.launch {
            // 👇 Обращаемся через repository
            _aniListExtra.value = repository.getAniListExtra(animeId, idMal)
        }
    }

    // --- 2. СЕТЬ (Данные из Шикимори) ---


    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error


    // --- 4. ЗАМЕТКИ (Локальная БД) ---
    private val _notes = MutableLiveData<List<com.example.animouse.data.database.NoteEntity>>()
    val notes: LiveData<List<com.example.animouse.data.database.NoteEntity>> = _notes

    fun loadNotes(animeId: Int) {
        viewModelScope.launch {
            _notes.value = database.noteDao().getNotesForAnime(animeId)
        }
    }

    fun addOrUpdateNote(note: com.example.animouse.data.database.NoteEntity) {
        viewModelScope.launch {
            if (note.id == 0) {
                database.noteDao().insert(note)
            } else {
                database.noteDao().update(note.copy(updatedAt = System.currentTimeMillis()))
            }
            loadNotes(note.animeId) // Перезагружаем список
        }
    }

    fun deleteNote(note: com.example.animouse.data.database.NoteEntity) {
        viewModelScope.launch {
            database.noteDao().delete(note)
            loadNotes(note.animeId)
        }
    }

    // --- 5. КАСТОМНЫЕ СПИСКИ (Локальная БД) ---
    private val _allCustomLists =
        MutableLiveData<List<com.example.animouse.data.database.CustomListEntity>>()
    val allCustomLists: LiveData<List<com.example.animouse.data.database.CustomListEntity>> =
        _allCustomLists

    private val _activeCustomListIds = MutableLiveData<List<Int>>()
    val activeCustomListIds: LiveData<List<Int>> = _activeCustomListIds

    // Загружаем все списки и проверяем, в каких из них лежит наше аниме
    fun loadCustomListsData(animeId: Int) {
        viewModelScope.launch {
            _allCustomLists.value = database.customListDao().getAllLists()
            if (animeId != -1) {
                val activeLists = database.customListDao().getListsForAnime(animeId)
                _activeCustomListIds.value = activeLists.map { it.id }
            }
        }
    }

    fun createNewCustomList(name: String, colorHex: String, currentAnimeId: Int) {
        viewModelScope.launch {
            val newList = com.example.animouse.data.database.CustomListEntity(
                name = name,
                colorHex = colorHex
            )
            database.customListDao().insertList(newList)
            // Обновляем данные, чтобы список сразу появился в меню
            loadCustomListsData(currentAnimeId)
        }
    }

    // Умное добавление/удаление из кастомного списка с подстраховкой оффлайн-кэша
    fun toggleAnimeInCustomList(
        listId: Int, animeId: Int, idMal: Int, title: String, posterUrl: String?,
        score: Int, epTotal: Int, epAired: Int, animeStatus: String?, isAdding: Boolean
    ) {
        viewModelScope.launch {
            if (isAdding) {
                // Если тайтла вообще нет в БД, сохраняем его со статусом null
                val existing = database.userAnimeDao().getAnimeById(animeId)
                if (existing == null) {
                    val entity = com.example.animouse.data.database.UserAnimeEntity(
                        animeId = animeId, idMal = idMal, status = null, title = title,
                        posterUrl = posterUrl, score = score, episodesTotal = epTotal,
                        episodesAired = epAired,
                        animeStatus = animeStatus
                    )
                    database.userAnimeDao().insert(entity)
                }
                database.customListDao().addAnimeToList(
                    com.example.animouse.data.database.AnimeCustomListCrossRef(
                        animeId = animeId,
                        listId = listId
                    )
                )
            } else {
                database.customListDao().removeAnimeFromList(
                    com.example.animouse.data.database.AnimeCustomListCrossRef(
                        animeId = animeId,
                        listId = listId
                    )
                )
                // Пытаемся удалить из кэша. Он удалится только если статус null и нет других кастомных списков
                val current = database.userAnimeDao().getAnimeById(animeId)
                if (current?.status == null) {
                    database.userAnimeDao().deleteIfUnused(animeId)
                }
            }
            // Перезагружаем галочки
            loadCustomListsData(animeId)
        }
    }

    // --- СКРИНШОТЫ ---
    private val _screenshots = MutableLiveData<List<String>>(emptyList())
    val screenshots: LiveData<List<String>> = _screenshots

    fun loadScreenshots(idMal: Int) {
        if (idMal <= 0) return
        viewModelScope.launch(Dispatchers.IO) {
            // Теперь репозиторий берет на себя всю грязную работу
            val urls = repository.getScreenshots(idMal)
            withContext(Dispatchers.Main) {
                _screenshots.value = urls
            }
        }
    }
}