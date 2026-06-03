package com.example.animouse.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_anime_list")
data class UserAnimeEntity(
    @PrimaryKey val animeId: Int, // <-- Твое оригинальное название!
    val idMal: Int,
    val status: String?, // WATCHING, PLANNED, COMPLETED, DROPPED (Стандартные списки)
    val title: String,
    val posterUrl: String?,
    val score: Int,
    val episodesTotal: Int,
    val episodesAired: Int,
    val animeStatus: String? // ongoing, anons, released (Тег для карточек)
)