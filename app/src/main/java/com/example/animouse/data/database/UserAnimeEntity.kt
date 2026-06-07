package com.example.animouse.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_anime_list")
data class UserAnimeEntity(
    @PrimaryKey val animeId: Int,
    val idMal: Int,
    val status: String?,
    val title: String,
    val posterUrl: String?,
    val score: Int,
    val episodesTotal: Int,
    val episodesAired: Int,
    val animeStatus: String?,

    val season: String? = null,
    val seasonYear: Int? = null
)