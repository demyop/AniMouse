package com.example.animouse.data.database

import androidx.room.Database
import androidx.room.RoomDatabase

// версия базы данных до 5
@Database(entities = [UserAnimeEntity::class, NoteEntity::class, CustomListEntity::class, AnimeCustomListCrossRef::class], version = 5, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userAnimeDao(): UserAnimeDao
    abstract fun noteDao(): NoteDao
    abstract fun customListDao(): CustomListDao
}