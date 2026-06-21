package com.example.animouse.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "anime_list_cache")
data class AnimeListItemEntity(
    @PrimaryKey val idMal: Int,
    val titleRomaji: String,
    val titleRussian: String?,
    val posterUrl: String?,
    val score: Int,
    val episodes: Int,
    val listType: String,
    // 👇 ДОБАВЛЯЕМ ДЛЯ КАЛЕНДАРЯ
    val nextAiringAt: Long?,
    val nextEpisode: Int?
)