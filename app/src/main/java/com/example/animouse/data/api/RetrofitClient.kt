package com.example.animouse.data.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    // ТЕКУЩИЙ КЛИЕНТ ДЛЯ ANILIST
    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://graphql.anilist.co")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val api: AniListApi by lazy {
        retrofit.create(AniListApi::class.java)
    }

    // НОВЫЙ КЛИЕНТ ДЛЯ SHIKIMORI
    private val shikimoriRetrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://shikimori.one/api/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val shikimoriApi: ShikimoriApi by lazy {
        shikimoriRetrofit.create(ShikimoriApi::class.java)
    }

    private const val KODIK_BASE_URL = "https://kodikapi.info/"

    // Это публичный токен, который часто используется в открытых парсерах
    const val KODIK_PUBLIC_TOKEN = "7e930ed6c496660e7ee90a4dfb7c25c3"

    val kodikApi: KodikApi by lazy {
        Retrofit.Builder()
            .baseUrl(KODIK_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(KodikApi::class.java)
    }

}