package com.example.animouse.data.api

import com.example.animouse.data.model.AnimeResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AniListApi {

    @POST("/")
    suspend fun getAnime(
        @Body request: GraphQLRequest
    ): Response<AnimeResponse>
}