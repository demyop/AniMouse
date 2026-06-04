package com.example.animouse.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_anime_list") // Строго это имя, чтобы DAO не ругался
data class UserAnimeEntity(
    @PrimaryKey val animeId: Int, // Оставляем только один правильный ID
    val idMal: Int,
    val status: String?,
    val title: String,
    val posterUrl: String?,
    val score: Int,
    val episodesTotal: Int,
    val episodesAired: Int,
    val animeStatus: String?
)