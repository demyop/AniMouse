package com.example.animouse.di

import com.example.animouse.data.api.AniListApi
import com.example.animouse.data.api.ShikimoriApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://graphql.anilist.co/") // Базовый URL для AniList
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideAniListApi(retrofit: Retrofit): AniListApi {
        return retrofit.create(AniListApi::class.java)
    }

    // Если ShikimoriApi тоже нужен, делаем так же:
    @Provides
    @Singleton
    fun provideShikimoriApi(): ShikimoriApi {
        return Retrofit.Builder()
            .baseUrl("https://shikimori.one/api/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ShikimoriApi::class.java)
    }
}