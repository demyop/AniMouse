package com.example.animouse.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "anime_details_cache")
data class AnimeDetailsEntity(
    @PrimaryKey val animeIdMal: Int, // Используем idMal как ключ, т.к. Шикимори работает по нему
    val russianTitle: String?,
    val description: String?,
    val episodesTotal: Int,
    val status: String?,
    val lastAccessedAt: Long = System.currentTimeMillis(), // Для умной очистки!
    val episodesAired: Int? // 👈 ДОБАВЛЯЕМ
)