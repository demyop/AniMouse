package com.example.animouse.data.model

data class Anime(
    val id: Int,
    val idMal: Int?,
    val title: Title,
    val coverImage: CoverImage,
    val nextAiringEpisode: NextAiringEpisode?,
    val episodes: Int?,
    val description: String?,   // Описание
    val genres: List<String>?,  // Список жанров
    val averageScore: Int?,     // Рейтинг (от 0 до 100)
    val status: String?,
    val season: String? = null,
    val seasonYear: Int? = null
)