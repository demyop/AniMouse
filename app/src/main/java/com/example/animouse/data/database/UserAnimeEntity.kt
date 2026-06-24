package com.example.animouse.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_anime_list") // Строго твое имя!
data class UserAnimeEntity(
    @PrimaryKey val animeId: Int,
    val idMal: Int,
    val status: String?,      // Твое поле (скорее всего, это WATCHING / PLANNED)
    val title: String,
    val posterUrl: String?,
    val score: Int,
    val episodesTotal: Int,
    val episodesAired: Int,
    val animeStatus: String?, // Твое поле (скорее всего, это RELEASING / FINISHED)

    // --- НАШИ НОВЫЕ ПОЛЯ ---
    val season: String? = null,
    val seasonYear: Int? = null
)