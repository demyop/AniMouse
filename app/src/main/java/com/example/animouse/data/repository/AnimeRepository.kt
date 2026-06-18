package com.example.animouse.data.repository

import com.example.animouse.data.api.AniListApi
import com.example.animouse.data.api.GraphQLRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnimeRepository @Inject constructor(
    private val api: AniListApi // 👈 Завхоз Hilt сам передаст сюда клиент!
) {

    suspend fun getAnimeList() =
        api.getAnime( // 👈 Больше никакого RetrofitClient.api
            GraphQLRequest(
                """
                query {
                  Page(page: 1, perPage: 30) {
                    media(
                      type: ANIME,
                      status: RELEASING,
                      sort: POPULARITY_DESC
                    ) {
                      id
                      idMal
                      status
                      season
                      seasonYear
                      episodes
                      description
                      genres
                      averageScore
                      
                      title {
                        romaji
                      }

                      coverImage {
                        large
                      }

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
}