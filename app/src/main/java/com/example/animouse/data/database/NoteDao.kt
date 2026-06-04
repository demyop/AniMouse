package com.example.animouse.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface NoteDao {
    // Получить все заметки для конкретного тайтла (сортировка от новых к старым)
    @Query("SELECT * FROM notes WHERE animeId = :animeId ORDER BY updatedAt DESC")
    suspend fun getNotesForAnime(animeId: Int): List<NoteEntity>

    @Query("DELETE FROM notes WHERE id = :noteId")
    suspend fun deleteById(noteId: Int)

    // Получить ВООБЩЕ ВСЕ заметки (пригодится для хаба на экране "Списки")
    @Query("SELECT * FROM notes ORDER BY updatedAt DESC")
    suspend fun getAllNotes(): List<NoteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: NoteEntity)

    @Update
    suspend fun update(note: NoteEntity)

    @Delete
    suspend fun delete(note: NoteEntity)

    // Вытаскиваем все заметки и приклеиваем к ним название и постер из таблицы user_anime_list
    @Query("""
        SELECT n.id AS noteId, n.animeId, ua.idMal, n.content, n.createdAt, n.updatedAt, 
               ua.title AS animeTitle, ua.posterUrl AS animePosterUrl
        FROM notes n
        INNER JOIN user_anime_list ua ON n.animeId = ua.animeId
        ORDER BY n.updatedAt DESC
    """)

    suspend fun getAllNotesWithAnime(): List<NoteWithAnime>
}