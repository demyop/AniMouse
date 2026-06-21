package com.example.animouse.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.animouse.data.database.AnimeDetailsEntity

@Dao
interface AnimeDetailsDao {
    @Query("SELECT * FROM anime_details_cache WHERE animeIdMal = :idMal")
    suspend fun getDetails(idMal: Int): AnimeDetailsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDetails(details: AnimeDetailsEntity)

    // Запрос для очистки старого кэша
    @Query("DELETE FROM anime_details_cache WHERE lastAccessedAt < :threshold")
    suspend fun clearOldCache(threshold: Long)
}