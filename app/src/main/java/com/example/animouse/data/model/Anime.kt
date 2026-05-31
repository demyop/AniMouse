package com.example.animouse.data.model

data class Anime(
    val id: Int,
    val title: Title,
    val coverImage: CoverImage,
    val nextAiringEpisode: NextAiringEpisode?
)