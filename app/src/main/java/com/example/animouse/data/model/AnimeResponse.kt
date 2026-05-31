package com.example.animouse.data.model

data class AnimeResponse(
    val data: PageData
)

data class PageData(
    val Page: AnimePage
)

data class AnimePage(
    val media: List<Anime>
)