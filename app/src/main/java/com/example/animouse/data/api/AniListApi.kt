package com.example.animouse.data.api

import com.example.animouse.data.model.AniListExtraResponse
import com.example.animouse.data.model.AnimeResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import com.example.animouse.data.model.DiscoveryResponse

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

    // Внутри интерфейса AniListApi добавь:
    @POST("/")
    suspend fun getDiscoveryAnime(
        @Body request: GraphQLRequest
    ): Response<DiscoveryResponse>

}