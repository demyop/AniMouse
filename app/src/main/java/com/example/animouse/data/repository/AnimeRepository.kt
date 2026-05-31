package com.example.animouse.data.repository

import com.example.animouse.data.api.GraphQLRequest
import com.example.animouse.data.api.RetrofitClient

class AnimeRepository {

    suspend fun getAnimeList() =

        RetrofitClient.api.getAnime(
            GraphQLRequest(
                """
                query {
                  Page(page: 1, perPage: 10) {
                    media(
                      type: ANIME,
                      status: RELEASING
                    ) {
                      id

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