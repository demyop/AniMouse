package com.example.animouse.data.api

import com.example.animouse.data.model.ShikimoriSearchResult
import com.example.animouse.data.model.ShikimoriAnimeDetails
import com.example.animouse.data.model.ShikiScreenshot // 👈 Не забудь импорт!
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

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

}