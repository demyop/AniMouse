package com.example.animouse.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_anime_list")
data class UserAnimeEntity(
    @PrimaryKey
    val animeId: Int,
    // Здесь будем хранить статус: "WATCHING", "PLANNED", "COMPLETED", "DROPPED", "FAVORITE"
    val status: String
)