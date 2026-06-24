package com.example.animouse.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "custom_lists")
data class CustomListEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val colorHex: String // Храним цвет в формате "#00FFFF" (бирюзовый и т.д.)
)