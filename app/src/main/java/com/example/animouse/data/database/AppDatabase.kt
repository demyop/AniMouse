package com.example.animouse.data.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [UserAnimeEntity::class],
    version = 2, // Повышаем версию!
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userAnimeDao(): UserAnimeDao

}