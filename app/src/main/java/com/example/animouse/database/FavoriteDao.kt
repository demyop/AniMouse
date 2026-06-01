package com.example.animouse.data.database

import androidx.room.*

@Dao
interface FavoriteDao {

    @Query("SELECT * FROM favorites")
    suspend fun getAll(): List<FavoriteEntity>

    @Insert
    suspend fun insert(
        favorite: FavoriteEntity
    )

    @Delete
    suspend fun delete(
        favorite: FavoriteEntity
    )
}