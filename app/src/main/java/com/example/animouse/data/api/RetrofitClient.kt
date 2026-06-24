package com.example.animouse.data.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    // --- ТВОЙ ТЕКУЩИЙ КЛИЕНТ ДЛЯ ANILIST ---
    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://graphql.anilist.co")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val api: AniListApi by lazy {
        retrofit.create(AniListApi::class.java)
    }

    // --- НОВЫЙ КЛИЕНТ ДЛЯ SHIKIMORI ---
    private val shikimoriRetrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://shikimori.one/api/") // Базовый URL Шикимори
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val shikimoriApi: ShikimoriApi by lazy {
        shikimoriRetrofit.create(ShikimoriApi::class.java)
    }
}