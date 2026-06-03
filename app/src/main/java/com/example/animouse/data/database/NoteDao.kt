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

    // Получить ВООБЩЕ ВСЕ заметки (пригодится для хаба на экране "Списки")
    @Query("SELECT * FROM notes ORDER BY updatedAt DESC")
    suspend fun getAllNotes(): List<NoteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: NoteEntity)

    @Update
    suspend fun update(note: NoteEntity)

    @Delete
    suspend fun delete(note: NoteEntity)
}