package com.example.animouse.data.database

import androidx.room.*

@Dao
interface UserAnimeDao {

    // Получить вообще все сохраненные тайтлы
    @Query("SELECT * FROM user_anime_list")
    suspend fun getAll(): List<UserAnimeEntity>

    // МАГИЯ ДЛЯ СПИСКОВ: Получить тайтлы только с определенным статусом
    @Query("SELECT * FROM user_anime_list WHERE status = :status")
    suspend fun getAnimeByStatus(status: String): List<UserAnimeEntity>

    // REPLACE означает: если такой ID уже есть, просто перезапиши его новым статусом
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(anime: UserAnimeEntity)

    // Удалить тайтл из БД по ID (если пользователь вообще убрал его из списков)
    @Query("DELETE FROM user_anime_list WHERE animeId = :animeId")
    suspend fun deleteById(animeId: Int)

    // ДОБАВИТЬ ЭТОТ МЕТОД:
    @Query("SELECT * FROM user_anime_list WHERE animeId = :id LIMIT 1")
    suspend fun getAnimeById(id: Int): UserAnimeEntity?
}