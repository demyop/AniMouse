package com.example.animouse.data.model

// Шикимори возвращает массив таких объектов при поиске
data class ShikimoriSearchResult(
    val id: Int, // В Шикимори их внутренний ID совпадает с MAL ID!
    val name: String,
    val russian: String?,
    val score: String?,
    val episodes: Int,
    val episodes_aired: Int,
    val status: String?, // <-- ДОБАВИЛИ СТАТУС В ПОИСКЕ
    val image: ShikimoriImage?
)

data class ShikimoriImage(
    val original: String?
)