package com.example.animouse.data.api

import com.example.animouse.data.model.ShikimoriSearchResult
import com.example.animouse.data.model.ShikimoriAnimeDetails
import com.example.animouse.data.model.ShikiScreenshot // 👈 Не забудь импорт!
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import com.example.animouse.data.model.ShikimoriNewsTopic

interface ShikimoriApi {
    @GET("animes/{id}")
    suspend fun getAnimeDetails(
        @Path("id") idMal: Int
    ): ShikimoriAnimeDetails

    // Поиск по русскому или английскому названию
    @GET("animes")
    suspend fun searchAnime(
        @Query("search") searchQuery: String,
        @Query("limit") limit: Int = 20
    ): List<ShikimoriSearchResult>

    // 👇 НАШ НОВЫЙ ЭНДПОИНТ ДЛЯ СКРИНШОТОВ 👇
    @GET("animes/{id}/screenshots")
    suspend fun getAnimeScreenshots(
        @Path("id") idMal: Int // У Шикимори их ID совпадает с MAL ID
    ): List<ShikiScreenshot>

    @GET("animes")
    suspend fun getAnimeListByIds(
        @Query("ids") ids: String, // Сюда передадим строку "1,2,3,4"
        @Query("limit") limit: Int = 50 // Максимум у Шики 50 за раз
    ): List<ShikimoriSearchResult>

    // 👇 НОВЫЙ ЭНДПОИНТ ДЛЯ НОВОСТЕЙ 👇
    @GET("topics")
    suspend fun getNewsTopics(
        @Query("forum") forum: String = "news",
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 10 // Грузим по 10 штук за раз, чтобы не мучить парсер
    ): List<ShikimoriNewsTopic>

    // 👇 ДОБАВЛЯЕМ ЭТО: Запрос полной версии новости (где ЕСТЬ html_body)
    @GET("topics/{id}")
    suspend fun getTopicDetails(
        @Path("id") id: Int
    ): ShikimoriNewsTopic

    // Запрос новостей с поддержкой страниц (для бесконечного скролла)
    @GET("topics")
    suspend fun getPaginatedNewsTopics(
        @Query("forum") forum: String = "news",
        @Query("page") page: Int,
        @Query("limit") limit: Int = 10 // Берем по 10, чтобы парсер не захлебнулся
    ): List<ShikimoriNewsTopic>

    // Умный рандом: берем 1 тайтл, случайно, с оценкой выше 7, формат ТВ или Фильм
    @GET("animes")
    suspend fun getRandomAnime(
        @Query("limit") limit: Int = 1,
        @Query("order") order: String = "random",
        @Query("score") minScore: Int = 7,
        @Query("kind") kind: String = "tv,movie"
    ): List<com.example.animouse.data.model.ShikimoriSearchResult>
}