package com.example.animouse.data.repository

import com.example.animouse.data.api.AniListApi
import com.example.animouse.data.api.GraphQLRequest
import com.example.animouse.data.api.ShikimoriApi
import com.example.animouse.data.database.AnimeDetailsDao
import com.example.animouse.data.database.AnimeDetailsEntity
import com.example.animouse.data.database.AnimeListDao
import com.example.animouse.data.database.AnimeListItemEntity
import com.example.animouse.data.model.AniListExtraMedia
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

@Singleton
class AnimeRepository @Inject constructor(
    private val aniListApi: AniListApi, // Имя переменной должно совпадать с тем, что ты используешь ниже
    private val shikiApi: ShikimoriApi,
    private val detailsDao: AnimeDetailsDao,
    private val listDao: AnimeListDao
) {

    // 1. Метод, на который будет подписываться ViewModel
    fun getTrendingAnimeFlow(): Flow<List<AnimeListItemEntity>> {
        return listDao.getListByType("TRENDING")
            .onStart {
                // Запускаем сеть в ОТДЕЛЬНОМ независимом потоке!
                // Теперь БД мгновенно отдаст кэш, а сеть просто обновит его, когда скачает.
                CoroutineScope(Dispatchers.IO).launch {
                    refreshTrendingAnime()
                }
            }
            .flowOn(Dispatchers.IO)
    }

    // 2. Вся грязная работа спрятана здесь
    private suspend fun refreshTrendingAnime() {
        try {
// Шаг 1: Быстро тянем тренды с AniList
            val response = aniListApi.getAnime(
                GraphQLRequest(
                    """
                query {
                  Page(page: 1, perPage: 30) {
                    media(type: ANIME, status: RELEASING, sort: POPULARITY_DESC) {
                      id
                      idMal
                      status
                      season
                      seasonYear
                      episodes
                      averageScore
                      title { romaji }
                      coverImage { large }
                      nextAiringEpisode {
                        episode
                        airingAt
                      }
                    }
                  }
                }
                """.trimIndent()
                )
            )

            val aniListItems = response.body()?.data?.Page?.media ?: return

            // Шаг 2: Мапим ответ в наши Entity для базы
            val entities = aniListItems.mapNotNull { item ->
                if (item.idMal == null) return@mapNotNull null

                AnimeListItemEntity(
                    idMal = item.idMal,
                    titleRomaji = item.title?.romaji ?: "Unknown",
                    titleRussian = null,
                    posterUrl = item.coverImage?.large,
                    score = item.averageScore ?: 0,
                    episodes = item.episodes ?: 0,
                    listType = "TRENDING",
                    // 👇 ДОБАВЛЯЕМ ДАННЫЕ ИЗ ANILIST
                    nextAiringAt = item.nextAiringEpisode?.airingAt?.toLong(),
                    nextEpisode = item.nextAiringEpisode?.episode
                )
            }

            // Шаг 3: Сохраняем в базу
            // В ЭТОТ МОМЕНТ UI УЖЕ САМ ОБНОВИТСЯ И ПОКАЖЕТ АНГЛИЙСКИЕ КАРТОЧКИ!
            listDao.insertAll(entities)

            // Шаг 4: Идем в Шикимори за русскими названиями
            // Берем все ID, соединяем их через запятую (получится "123,456,789")
            val idsString = entities.map { it.idMal }.joinToString(",")

            if (idsString.isNotEmpty()) {
                val shikiData = shikiApi.getAnimeListByIds(idsString)

                // Шаг 5: Точечно обновляем русские названия в базе
                shikiData.forEach { shikiAnime ->
                    val ruTitle = shikiAnime.russian
                    if (!ruTitle.isNullOrBlank()) {
                        listDao.updateRussianTitle(shikiAnime.id, ruTitle)
                    }
                }
                // В ЭТОТ МОМЕНТ У ПОЛЬЗОВАТЕЛЯ НА ЭКРАНЕ "ПРОРАСТУТ" РУССКИЕ БУКВЫ!
            }

        } catch (e: Exception) {
            // Если нет интернета — игнорируем ошибку.
            // Пользователь просто увидит кэш из БД.
        }
    }
    // Добавляем хелпер для проверки свежести
    private fun isDataStale(timestamp: Long): Boolean {
        val twentyFourHours = 24 * 60 * 60 * 1000L
        return (System.currentTimeMillis() - timestamp) > twentyFourHours
    }

    // Новый метод получения деталей
    fun getAnimeDetails(idMal: Int): Flow<AnimeDetailsEntity?> = flow {
        // 1. Пытаемся достать из БД
        val cached = detailsDao.getDetails(idMal)

        // 2. Если данные есть, сразу отдаем их
        if (cached != null) {
            emit(cached)
        }

        // 3. Если данных нет или они протухли — тянем свежие
        if (cached == null || isDataStale(cached.lastAccessedAt)) {
            try {
                val fresh = shikiApi.getAnimeDetails(idMal)

                val entity = AnimeDetailsEntity(
                    animeIdMal = fresh.id,
                    russianTitle = fresh.russian,
                    description = fresh.description,
                    episodesTotal = fresh.episodes,
                    status = fresh.status,
                    episodesAired = fresh.episodes_aired
                )

                // Сохраняем и отдаем
                detailsDao.insertDetails(entity)
                emit(entity)
            } catch (e: Exception) {
                // Если сеть упала, а кэша нет — ничего не делаем
            }
        }
    }.flowOn(Dispatchers.IO)
    suspend fun getAniListExtra(animeId: Int, idMal: Int): AniListExtraMedia? {
        // 👇 Новая железная логика: если MAL ID существует, используем только его!
        val searchByMal = idMal > 0

        val query = if (searchByMal) {
            "query(\$idMal: Int) { Media(idMal: \$idMal, type: ANIME) { id season seasonYear genres trailer { id site } relations { edges { relationType node { id idMal title { romaji } coverImage { large } averageScore } } } nextAiringEpisode { airingAt episode } } }"
        } else {
            "query(\$id: Int) { Media(id: \$id, type: ANIME) { id season seasonYear genres trailer { id site } relations { edges { relationType node { id idMal title { romaji } coverImage { large } averageScore } } } nextAiringEpisode { airingAt episode } } }"
        }

        val variables = if (searchByMal) mapOf("idMal" to idMal) else mapOf("id" to animeId)

        return try {
            aniListApi.getAnimeDetails(GraphQLRequest(query, variables)).data.Media
        } catch (e: Exception) {
            null
        }
    }

    // В AnimeRepository.kt
    suspend fun getScreenshots(idMal: Int): List<String> {
        return try {
            // Вызываем твой API
            val response = shikiApi.getAnimeScreenshots(idMal)
            // Мапим результат в список строк
            response.map { "https://shikimori.one${it.original}" }
        } catch (e: Exception) {
            emptyList()
        }
    }

}