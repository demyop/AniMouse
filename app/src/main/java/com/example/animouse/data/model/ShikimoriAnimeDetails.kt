package com.example.animouse.data.model

data class ShikimoriAnimeDetails(
    val id: Int,
    val name: String,
    val russian: String?,
    val description: String?,
    val episodes: Int,
    val episodes_aired: Int,
    val status: String,

    val score: String?,
    val image: ShikimoriImage?, // Используем класс из твоего ShikimoriSearchResponse
    val aired_on: String?
)

data class ShikiScreenshot(
    val original: String,
    val preview: String
)