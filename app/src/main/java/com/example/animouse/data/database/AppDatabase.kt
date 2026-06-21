package com.example.animouse.data.database

import com.example.animouse.data.database.AnimeDetailsDao
import androidx.room.Database
import androidx.room.RoomDatabase

// версия базы данных до 5
@Database(
    entities = [
        UserAnimeEntity::class,
        NoteEntity::class,
        CustomListEntity::class,
        AnimeCustomListCrossRef::class,
        AnimeDetailsEntity::class, // 👈 ДОБАВИЛИ СЮДУЮ
        AnimeListItemEntity::class
    ],
    version = 8, // 👈 ОБЯЗАТЕЛЬНО ИНКРЕМЕНТИРУЕМ ВЕРСИЮ
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userAnimeDao(): UserAnimeDao
    abstract fun noteDao(): NoteDao
    abstract fun customListDao(): CustomListDao
    abstract fun animeDetailsDao(): AnimeDetailsDao // 👈 ДОБАВИЛИ МЕТОД
    abstract fun animeListDao(): AnimeListDao
}
