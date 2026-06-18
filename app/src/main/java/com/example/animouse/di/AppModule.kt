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


// ... твой текущий код в AppModule (например, provideDatabase) ...

    @Provides
    @Singleton
    fun provideAniListApi(): AniListApi {
        return Retrofit.Builder()
            .baseUrl("https://graphql.anilist.co")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AniListApi::class.java)
    }

    @Provides
    @Singleton
    fun provideShikimoriApi(): ShikimoriApi {
        return Retrofit.Builder()
            .baseUrl("https://shikimori.one/api/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ShikimoriApi::class.java)
    }

    @Provides
    @Singleton
    fun provideKodikApi(): KodikApi {
        return Retrofit.Builder()
            .baseUrl("https://kodikapi.info/") // Токен можно будет передавать прямо в запросы
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(KodikApi::class.java)
    }
}