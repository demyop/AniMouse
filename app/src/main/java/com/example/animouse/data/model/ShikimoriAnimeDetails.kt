package com.example.animouse.data.model

data class ShikimoriAnimeDetails(
    val id: Int,
    val name: String,
    val russian: String?,
    val description: String?,
    val episodes: Int,
    val episodes_aired: Int,
    val status: String
)

data class ShikiScreenshot(
    val original: String,
    val preview: String
)