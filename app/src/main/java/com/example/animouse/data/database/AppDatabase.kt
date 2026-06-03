package com.example.animouse.data.database

import androidx.room.Database
import androidx.room.RoomDatabase

// Увеличили версию базы данных до 2 и добавили NoteEntity
@Database(entities = [UserAnimeEntity::class, NoteEntity::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userAnimeDao(): UserAnimeDao
    abstract fun noteDao(): NoteDao // Добавили доступ к заметкам
}