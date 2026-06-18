package com.example.animouse.data.api

import com.example.animouse.data.model.KodikResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface KodikApi {
    @GET("search")
    suspend fun searchAnime(
        @Query("token") token: String,
        @Query("shikimori_id") shikiId: Int,
        @Query("with_episodes") withEpisodes: Boolean = true
    ): Response<KodikResponse>
}