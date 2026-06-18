package com.example.animouse.di

import android.content.Context
import androidx.room.Room
import com.example.animouse.data.database.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class) // Означает, что склад живет пока живо всё приложение
object AppModule {

    // Объясняем Hilt, как собирать базу данных
    @Provides
    @Singleton // База должна быть ОДНА на весь проект
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "animouse_db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    // Заодно объясняем, как доставать DAO из базы
    @Provides
    fun provideCustomListDao(database: AppDatabase) = database.customListDao()

    @Provides
    fun provideNoteDao(database: AppDatabase) = database.noteDao()

    @Provides
    fun provideUserAnimeDao(database: AppDatabase) = database.userAnimeDao()
}