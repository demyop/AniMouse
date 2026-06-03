package com.example.animouse.data.database

import androidx.room.Entity

// Проверь, что tableName написан буква в букву!
@Entity(tableName = "anime_custom_list_cross_ref", primaryKeys = ["animeId", "listId"])
data class AnimeCustomListCrossRef(
    val animeId: Int,
    val listId: Int
)