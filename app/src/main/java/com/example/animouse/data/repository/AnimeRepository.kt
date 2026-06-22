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
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlin.collections.map
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

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

    // Метод для получения чистых новостей
    // Метод для получения чистых новостей
    // Метод для получения чистых новостей
    // Метод для получения чистых новостей
    // Метод для получения чистых новостей
    // Метод для получения чистых новостей
    // Метод для получения чистых новостей
    // Метод для получения чистых новостей
    // Метод для получения чистых новостей
// Метод для получения чистых новостей
    // Метод для получения чистых новостей
    // Метод для получения чистых новостей
// Метод для получения чистых новостей
// Метод для получения чистых новостей
// Метод для получения чистых новостей
    // Метод для получения чистых и детальных новостей
    suspend fun getNews(page: Int = 1): List<com.example.animouse.data.model.AnimeNews> {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                // 👇 Передаем page сюда! И сразу фильтруем системные закрепленные посты
                val rawTopics = shikiApi.getNewsTopics(page = page).filter { it.linked != null }
                val resultList = mutableListOf<com.example.animouse.data.model.AnimeNews>()

                for (topic in rawTopics) {
                    var imageUrl: String? = null
                    var parsedTags: List<String> = emptyList()
                    var bodyHtml = ""
                    var sourceUrl: String? = null
                    val mediaUrls = mutableListOf<String>()
                    val newsUrl = "https://shikimori.one/forum/news/${topic.id}"

                    try {
                        kotlinx.coroutines.delay(600)

                        val url = java.net.URL(newsUrl)
                        val connection = url.openConnection() as java.net.HttpURLConnection
                        connection.setRequestProperty("User-Agent", "Mozilla/5.0 ...")
                        // ⏱ Было 5000 (5 сек), даем 10000 (10 сек), чтобы холодный старт не отваливался
                        connection.connectTimeout = 10000
                        connection.readTimeout = 10000

                        val webpageHtml = connection.inputStream.bufferedReader().readText()

                        // 🎯 1. КАРТИНКА ДЛЯ ОБЛОЖКИ (og:image)
                        val ogImageRegex = Regex("<meta property=\"og:image\" content=\"([^\"]+)\"")
                        val ogImage = ogImageRegex.find(webpageHtml)?.groupValues?.get(1)
                        if (ogImage != null && !ogImage.contains("missing_original")) {
                            imageUrl = ogImage
                        }

                        // 🎯 2. СБОР ВСЕХ МЕДИА ИЗ b-shiki_wall
                        val wallBlockRegex = Regex("<div[^>]*class=\"[^\"]*b-shiki_wall[^\"]*\"[^>]*>([\\s\\S]*?)</div>")
                        val wallBlock = wallBlockRegex.find(webpageHtml)?.groupValues?.get(1)
                        if (wallBlock != null) {
                            val imgInWallRegex = Regex("""src="([^"]+)"""")
                            imgInWallRegex.findAll(wallBlock).forEach { match ->
                                var mUrl = match.groupValues[1]
                                if (mUrl.startsWith("//")) mUrl = "https:$mUrl"
                                else if (mUrl.startsWith("/")) mUrl = "https://shikimori.one$mUrl"
                                if (!mediaUrls.contains(mUrl)) mediaUrls.add(mUrl)
                            }
                        }

                        // Если og:image не нашелся, берем первую картинку из медиа-блока
                        if (imageUrl == null && mediaUrls.isNotEmpty()) {
                            imageUrl = mediaUrls.first()
                        }

                        // 🎯 3. ТЕГИ
                        val tagsStartIndex = webpageHtml.indexOf("<div class=\"tags\">")
                        if (tagsStartIndex != -1) {
                            val searchArea = webpageHtml.substring(tagsStartIndex, minOf(tagsStartIndex + 1000, webpageHtml.length))
                            val tagItemRegex = Regex("""data-text="([^"]+)"""")
                            parsedTags = tagItemRegex.findAll(searchArea).map { it.groupValues[1] }.toList()
                        }

                        // 🎯 4. СОДЕРЖИМОЕ НОВОСТИ (body-inner)
                        val bodyInnerRegex = Regex("<div[^>]*class=\"[^\"]*body-inner[^\"]*\"[^>]*>([\\s\\S]*?)</div>")
                        bodyHtml = bodyInnerRegex.find(webpageHtml)?.groupValues?.get(1) ?: ""

                        // 🎯 5. ИСТОЧНИК (source)
                        val sourceRegex = Regex("<div[^>]*class=\"[^\"]*source[^\"]*\"[^>]*>[\\s\\S]*?href=\"([^\"]+)\"")
                        sourceUrl = sourceRegex.find(webpageHtml)?.groupValues?.get(1)
                        if (sourceUrl != null && sourceUrl.startsWith("/")) {
                            sourceUrl = "https://shikimori.one$sourceUrl"
                        }

                    } catch (e: Exception) {
                        android.util.Log.e("ShikiNews", "❌ Ошибка парсинга страницы ${topic.id}: ${e.message}")
                    }

                    // Спасательный круг для картинки обложки
                    if (imageUrl == null) {
                        imageUrl = topic.linked?.image?.original
                        if (imageUrl != null && imageUrl.startsWith("/")) imageUrl = "https://shikimori.one$imageUrl"
                    }
                    if (imageUrl != null && imageUrl.contains("missing_original")) imageUrl = null

                    val finalTags = if (parsedTags.isNotEmpty()) parsedTags else topic.tags?.map { it.name } ?: emptyList()

                    // Форматируем строгую дату для карточки деталей
                    val strictDate = try {
                        val cleanIso = topic.created_at.substringBefore("+").substringBefore("Z").substringBefore(".")
                        val inFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                        val dateObj = inFormat.parse(cleanIso)
                        if (dateObj != null) {
                            SimpleDateFormat("HH:mm | dd.MM.yyyy", Locale.getDefault()).format(dateObj)
                        } else "00:00 | 00.00.0000"
                    } catch (e: Exception) {
                        "00:00 | 00.00.0000"
                    }

                    resultList.add(
                        com.example.animouse.data.model.AnimeNews(
                            id = topic.id,
                            title = topic.topic_title,
                            imageUrl = imageUrl,
                            date = formatRelativeDate(topic.created_at), // "Вчера" для главной
                            fullDate = strictDate, // "00:00 | ЧЧ.ММ.ГГГГ" для деталей
                            tags = finalTags,
                            newsUrl = newsUrl,
                            bodyHtml = bodyHtml,
                            sourceUrl = sourceUrl,
                            linkedAnimeIdMal = topic.linked?.id, // ID связанного тайтла (у Шики совпадает с MAL)
                            mediaUrls = mediaUrls
                        )
                    )
                }
                resultList
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    // Сама по себе функция ничего не ломает, просто вычисляет квартал
    private fun getUpcomingSeason(): Pair<String, Int> {
        val calendar = java.util.Calendar.getInstance()
        val month = calendar.get(java.util.Calendar.MONTH)
        val year = calendar.get(java.util.Calendar.YEAR)

        return when (month) {
            in 0..2 -> "SPRING" to year
            in 3..5 -> "SUMMER" to year
            in 6..8 -> "FALL" to year
            else -> "WINTER" to (year + 1)
        }
    }

    // Этот метод вообще не пересекается со старым!
    suspend fun refreshDiscoveryData() {
        try {
            val (upcomingSeason, upcomingYear) = getUpcomingSeason()

            val response = aniListApi.getDiscoveryAnime(
                GraphQLRequest(
                    query = """
                    query(${'$'}season: MediaSeason, ${'$'}seasonYear: Int) {
                      trending: Page(page: 1, perPage: 20) {
                        media(type: ANIME, status: RELEASING, episodes_lesser: 150, sort: POPULARITY_DESC) {
                          id idMal status episodes averageScore title { romaji } coverImage { large } nextAiringEpisode { episode airingAt }
                        }
                      }
                      upcoming: Page(page: 1, perPage: 20) {
                        media(type: ANIME, status: NOT_YET_RELEASED, season: ${'$'}season, seasonYear: ${'$'}seasonYear, sort: POPULARITY_DESC) {
                          id idMal status episodes averageScore title { romaji } coverImage { large } nextAiringEpisode { episode airingAt }
                        }
                      }
                      top: Page(page: 1, perPage: 50) {
                        media(type: ANIME, sort: SCORE_DESC) {
                          id idMal status episodes averageScore title { romaji } coverImage { large } nextAiringEpisode { episode airingAt }
                        }
                      }
                    }
                    """.trimIndent(),
                    variables = mapOf("season" to upcomingSeason, "seasonYear" to upcomingYear)
                )
            )

            val data = response.body()?.data ?: return

            fun mapToEntities(mediaList: List<com.example.animouse.data.model.Anime>?, listType: String): List<AnimeListItemEntity> {
                return mediaList?.mapNotNull { item ->
                    if (item.idMal == null) return@mapNotNull null
                    AnimeListItemEntity(
                        idMal = item.idMal,
                        titleRomaji = item.title?.romaji ?: "Unknown",
                        titleRussian = null,
                        posterUrl = item.coverImage?.large,
                        score = item.averageScore ?: 0,
                        episodes = item.episodes ?: 0,
                        listType = listType,
                        nextAiringAt = item.nextAiringEpisode?.airingAt?.toLong(),
                        nextEpisode = item.nextAiringEpisode?.episode
                    )
                } ?: emptyList()
            }

            val allEntities = mutableListOf<AnimeListItemEntity>()
            // ⚠️ ВАЖНО: записываем тренды под новым тегом "DISCOVERY_TRENDING",
            // чтобы они не перемешались со старым списком "TRENDING"!
            allEntities.addAll(mapToEntities(data.trending?.media, "DISCOVERY_TRENDING"))
            allEntities.addAll(mapToEntities(data.upcoming?.media, "UPCOMING"))
            allEntities.addAll(mapToEntities(data.top?.media, "TOP_100"))

            listDao.insertAll(allEntities)

            val idsString = allEntities.map { it.idMal }.distinct().joinToString(",")
            if (idsString.isNotEmpty()) {
                val shikiData = shikiApi.getAnimeListByIds(idsString)
                shikiData.forEach { shikiAnime ->
                    val ruTitle = shikiAnime.russian
                    if (!ruTitle.isNullOrBlank()) {
                        listDao.updateRussianTitle(shikiAnime.id, ruTitle)
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("AniMouseDiscovery", "Ошибка мега-запроса: ${e.message}", e)
        }
    }
    // Функция превращает скучный ISO-формат в "2 ч. назад" или "Вчера"
    private fun formatRelativeDate(isoDate: String): String {
        try {
            // Очищаем строку от часовых поясов и миллисекунд (2024-03-10T12:00:00)
            val cleanIso = isoDate.substringBefore("+").substringBefore("Z").substringBefore(".")
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val date = format.parse(cleanIso) ?: return isoDate.substringBefore("T")

            val diff = System.currentTimeMillis() - date.time

            val oneMinute = 60 * 1000L
            val oneHour = 60 * oneMinute
            val oneDay = 24 * oneHour

            val calDate = Calendar.getInstance().apply { time = date }
            val calNow = Calendar.getInstance()

            // Сбрасываем часы, чтобы честно считать "Вчера" и "Позавчера"
            calDate.set(Calendar.HOUR_OF_DAY, 0); calDate.set(Calendar.MINUTE, 0); calDate.set(Calendar.SECOND, 0); calDate.set(Calendar.MILLISECOND, 0)
            calNow.set(Calendar.HOUR_OF_DAY, 0); calNow.set(Calendar.MINUTE, 0); calNow.set(Calendar.SECOND, 0); calNow.set(Calendar.MILLISECOND, 0)

            val dayDiff = (calNow.timeInMillis - calDate.timeInMillis) / oneDay

            return when {
                diff < oneHour && diff >= 0 -> {
                    val mins = (diff / oneMinute).toInt()
                    if (mins <= 0) "Только что" else "$mins мин. назад"
                }
                diff < oneDay && diff >= 0 && dayDiff == 0L -> "${(diff / oneHour).toInt()} ч. назад"
                dayDiff == 1L -> "Вчера"
                dayDiff == 2L -> "Позавчера"
                else -> {
                    val outFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                    outFormat.format(date)
                }
            }
        } catch (e: Exception) {
            return isoDate.substringBefore("T") // Запасной план
        }
    }
    // Этот метод будет кормить наши новые карусели, когда мы их создадим
    fun getDiscoveryListFlow(type: String): Flow<List<AnimeListItemEntity>> {
        return listDao.getListByType(type)
            .onStart {
                CoroutineScope(Dispatchers.IO).launch {
                    refreshDiscoveryData()
                }
            }
            .flowOn(Dispatchers.IO)
    }

    // Бросаем кубик!
    suspend fun rollTheDice(): com.example.animouse.data.model.ShikimoriSearchResult? {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                shikiApi.getRandomAnime().firstOrNull()
            } catch (e: Exception) {
                null
            }
        }
    }
}