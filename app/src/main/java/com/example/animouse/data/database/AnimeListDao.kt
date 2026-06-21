package com.example.animouse.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AnimeListDao {
    // 👈 Обрати внимание, возвращаем Flow! UI будет сам обновляться при изменениях в таблице.
    @Query("SELECT * FROM anime_list_cache WHERE listType = :type")
    fun getListByType(type: String): Flow<List<AnimeListItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(animeList: List<AnimeListItemEntity>)

    // Точечное обновление только русского названия, чтобы не затереть остальные данные
    @Query("UPDATE anime_list_cache SET titleRussian = :ruTitle WHERE idMal = :idMal")
    suspend fun updateRussianTitle(idMal: Int, ruTitle: String)

    // Очистка старого кэша (например, при обновлении списка свайпом)
    @Query("DELETE FROM anime_list_cache WHERE listType = :type")
    suspend fun clearListByType(type: String)
}