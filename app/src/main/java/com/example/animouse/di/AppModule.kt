package com.example.animouse.di

import android.content.Context
import androidx.room.Room
import com.example.animouse.data.api.AniListApi
import com.example.animouse.data.api.KodikApi
import com.example.animouse.data.api.ShikimoriApi
import com.example.animouse.data.database.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton
import com.example.animouse.data.database.AnimeDetailsDao
import com.example.animouse.data.database.AnimeListDao

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(context, AppDatabase::class.java, "animouse_db")
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideCustomListDao(database: AppDatabase) = database.customListDao()

    @Provides
    fun provideNoteDao(database: AppDatabase) = database.noteDao()

    @Provides
    fun provideUserAnimeDao(database: AppDatabase) = database.userAnimeDao()

    // 👈 ЭТОГО МЕТОДА НЕ ХВАТАЕТ:
    @Provides
    fun provideAnimeDetailsDao(database: AppDatabase) = database.animeDetailsDao()
    // ВСЁ, БОЛЬШЕ НИЧЕГО ТУТ НЕ ДОЛЖНО БЫТЬ!
    @Provides
    fun provideAnimeListDao(database: AppDatabase): AnimeListDao {
        return database.animeListDao()
    }
}