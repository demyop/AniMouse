package com.example.animouse.data.api

import com.example.animouse.data.model.ShikimoriSearchResult
import retrofit2.http.Query
import com.example.animouse.data.model.ShikimoriAnimeDetails
import retrofit2.http.GET
import retrofit2.http.Path

interface ShikimoriApi {
    @GET("animes/{id}")
    suspend fun getAnimeDetails(
        @Path("id") idMal: Int
    ): ShikimoriAnimeDetails

    // Поиск по русскому или английскому названию
    @GET("animes")
    suspend fun searchAnime(
        @Query("search") searchQuery: String,
        @Query("limit") limit: Int = 20 // Ограничим выдачу 20 результатами для скорости
    ): List<ShikimoriSearchResult>
}