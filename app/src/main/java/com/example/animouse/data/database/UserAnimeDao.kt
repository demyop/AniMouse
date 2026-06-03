package com.example.animouse.data.database

import androidx.room.*

@Dao
interface UserAnimeDao {

    // --- ТВОИ СТАРЫЕ МЕТОДЫ (оставляем как было) ---

    @Query("SELECT * FROM user_anime_list")
    suspend fun getAll(): List<UserAnimeEntity>

    @Query("SELECT * FROM user_anime_list WHERE status = :status")
    suspend fun getAnimeByStatus(status: String): List<UserAnimeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(anime: UserAnimeEntity)

    @Query("DELETE FROM user_anime_list WHERE animeId = :animeId")
    suspend fun deleteById(animeId: Int)

    @Query("SELECT * FROM user_anime_list WHERE animeId = :id LIMIT 1")
    suspend fun getAnimeById(id: Int): UserAnimeEntity?


    // --- НОВЫЕ МЕТОДЫ (Для новой Offline-First логики и кастомных списков) ---

    // Получить вообще все сохраненные тайтлы (у которых есть хоть какой-то стандартный статус)
    @Query("SELECT * FROM user_anime_list WHERE status IS NOT NULL")
    suspend fun getAllSavedAnime(): List<UserAnimeEntity>

    // Получить статус конкретного тайтла
    @Query("SELECT status FROM user_anime_list WHERE animeId = :id")
    suspend fun getStatusById(id: Int): String?

    // Умное удаление: удаляем из кэша, ТОЛЬКО если аниме не состоит в кастомных списках
    @Query("DELETE FROM user_anime_list WHERE animeId = :id AND animeId NOT IN (SELECT animeId FROM anime_custom_list_cross_ref)")
    suspend fun deleteIfUnused(id: Int)
}