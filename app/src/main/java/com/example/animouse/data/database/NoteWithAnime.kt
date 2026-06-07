package com.example.animouse.data.database

data class NoteWithAnime(
    val noteId: Int,
    val animeId: Int,
    val idMal: Int,
    val content: String,
    val createdAt: Long,
    val updatedAt: Long,
    val animeTitle: String,
    val animePosterUrl: String?
)