package com.example.animouse.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.animouse.data.api.RetrofitClient
import com.example.animouse.data.database.AppDatabase
import com.example.animouse.data.database.UserAnimeEntity
import com.example.animouse.data.model.ShikimoriAnimeDetails
import kotlinx.coroutines.launch

class DetailsViewModel(application: Application) : AndroidViewModel(application) {

    // --- 1. ЛОКАЛЬНАЯ БАЗА (Списки пользователя) ---
    private val database = Room.databaseBuilder(
        application, AppDatabase::class.java, "animouse_db"
    ).fallbackToDestructiveMigration().build()

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
        newStatus: String?,
        title: String,
        posterUrl: String?,
        score: Int,
        epTotal: Int,
        epAired: Int,
        animeStatus: String?
    ) {
        viewModelScope.launch {
            if (newStatus == null || newStatus == "NONE") {
                // Если юзер нажал "Удалить из списков"
                val current = database.userAnimeDao().getAnimeById(animeId)
                if (current != null) {
                    // Сначала обнуляем статус (чтобы пропало из стандартных списков)
                    database.userAnimeDao().insert(current.copy(status = null))
                    // А затем пытаемся удалить полностью (удалится, только если нет в кастомных списках)
                    database.userAnimeDao().deleteIfUnused(animeId)
                }
            } else {
                // Добавляем или обновляем карточку
                val entity = com.example.animouse.data.database.UserAnimeEntity(
                    animeId = animeId,
                    idMal = idMal,
                    status = newStatus,
                    title = title,
                    posterUrl = posterUrl,
                    score = score,
                    episodesTotal = epTotal,
                    episodesAired = epAired,
                    animeStatus = animeStatus
                )
                database.userAnimeDao().insert(entity)
            }
            _currentStatus.value = newStatus
        }
    }

    // --- 3. СЕТЬ (Доп. данные из AniList: Трейлер и Связанное) ---
    private val _aniListExtra = MutableLiveData<com.example.animouse.data.model.AniListExtraMedia?>()
    val aniListExtra: LiveData<com.example.animouse.data.model.AniListExtraMedia?> = _aniListExtra

    fun loadAniListExtra(animeId: Int, idMal: Int) {
        viewModelScope.launch {
            try {
                // Определяем, откуда мы пришли: из поиска (только idMal) или с главного экрана (оба ID)
                val isSearchByMal = animeId == -1 && idMal != -1

                // Подменяем текст запроса в зависимости от ситуации
                val query = if (isSearchByMal) {
                    """
                    query(${'$'}idMal: Int) {
                      Media(idMal: ${'$'}idMal, type: ANIME) {
                        id
                        trailer { id site }
                        relations { edges { relationType node { id idMal title { romaji } coverImage { large } averageScore } } }
                      }
                    }
                    """.trimIndent()
                } else {
                    """
                    query(${'$'}id: Int) {
                      Media(id: ${'$'}id, type: ANIME) {
                        id
                        trailer { id site }
                        relations { edges { relationType node { id idMal title { romaji } coverImage { large } averageScore } } }
                      }
                    }
                    """.trimIndent()
                }

                // Передаем нужную переменную
                val variables = if (isSearchByMal) mapOf("idMal" to idMal) else mapOf("id" to animeId)

                val request = com.example.animouse.data.api.GraphQLRequest(query, variables)
                val response = com.example.animouse.data.api.RetrofitClient.api.getAnimeDetails(request)

                _aniListExtra.value = response.data.Media
            } catch (e: Exception) {
                _aniListExtra.value = null
            }
        }
    }

    // --- 2. СЕТЬ (Данные из Шикимори) ---
    private val _animeDetails = MutableLiveData<ShikimoriAnimeDetails?>()
    val animeDetails: LiveData<ShikimoriAnimeDetails?> = _animeDetails

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun loadAnimeDetails(idMal: Int) {
        if (idMal == -1) {
            _error.value = "ID MyAnimeList не найден"
            return
        }
        viewModelScope.launch {
            try {
                val response = RetrofitClient.shikimoriApi.getAnimeDetails(idMal)
                _animeDetails.value = response
                _error.value = null
            } catch (e: Exception) {
                _error.value = e.localizedMessage ?: "Ошибка сети"
                _animeDetails.value = null
            }
        }
    }

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
    private val _allCustomLists = MutableLiveData<List<com.example.animouse.data.database.CustomListEntity>>()
    val allCustomLists: LiveData<List<com.example.animouse.data.database.CustomListEntity>> = _allCustomLists

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
            val newList = com.example.animouse.data.database.CustomListEntity(name = name, colorHex = colorHex)
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

}