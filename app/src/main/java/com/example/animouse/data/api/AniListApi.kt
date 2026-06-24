package com.example.animouse.data.api

import com.example.animouse.data.model.AnimeResponse
import com.example.animouse.data.model.AniListExtraResponse // Не забудь, что для этого нужна новая модель из Шага 1
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AniListApi {

    // Старый метод (ВЕРНУЛИ Response<...>, теперь MainViewModel починится!)
    @POST("/")
    suspend fun getAnime(
        @Body request: GraphQLRequest
    ): Response<AnimeResponse>

    // Наш новый точечный запрос для деталей:
    @POST("/")
    suspend fun getAnimeDetails(
        @Body request: GraphQLRequest
    ): AniListExtraResponse
}