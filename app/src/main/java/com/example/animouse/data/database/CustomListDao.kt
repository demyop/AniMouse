package com.example.animouse.data.database

import androidx.room.*

@Dao
interface CustomListDao {
    @Query("SELECT * FROM custom_lists")
    suspend fun getAllLists(): List<CustomListEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertList(list: CustomListEntity)

    @Query("DELETE FROM custom_lists WHERE id = :listId")
    suspend fun deleteList(listId: Int)

    // Связывание тайтла со списком
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addAnimeToList(crossRef: AnimeCustomListCrossRef)

    // Удаление тайтла из кастомного списка
    @Delete
    suspend fun removeAnimeFromList(crossRef: AnimeCustomListCrossRef)

    // Получить все кастомные списки, в которых состоит этот тайтл (для тегов в деталях)
    @Query("""
        SELECT cl.* FROM custom_lists cl 
        INNER JOIN anime_custom_list_cross_ref ref ON cl.id = ref.listId 
        WHERE ref.animeId = :animeId
    """)
    suspend fun getListsForAnime(animeId: Int): List<CustomListEntity>

    // Получить все тайтлы конкретного кастомного списка
    @Query("""
        SELECT ua.* FROM user_anime_list ua 
        INNER JOIN anime_custom_list_cross_ref ref ON ua.animeId = ref.animeId 
        WHERE ref.listId = :listId
    """)
    suspend fun getAnimeInList(listId: Int): List<UserAnimeEntity>
}