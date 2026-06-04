package com.example.animouse.data.repository

import com.example.animouse.data.api.GraphQLRequest
import com.example.animouse.data.api.RetrofitClient

class AnimeRepository {

    suspend fun getAnimeList() =

        RetrofitClient.api.getAnime(
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
                      idMal  # <-- ДОБАВИЛИ ЗАПРОС ID ДЛЯ ШИКИМОРИ
                      status       # <-- ДОБАВИЛИ! Теперь он не null
                      season       # <-- ДОБАВИТЬ ЭТО
                      seasonYear   # <-- ДОБАВИТЬ ЭТО
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