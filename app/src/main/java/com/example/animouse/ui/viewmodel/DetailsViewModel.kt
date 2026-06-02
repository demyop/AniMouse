package com.example.animouse.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.animouse.data.database.AppDatabase
import com.example.animouse.data.database.UserAnimeEntity
import kotlinx.coroutines.launch

class DetailsViewModel(application: Application) : AndroidViewModel(application) {

    private val database = Room.databaseBuilder(
        application, AppDatabase::class.java, "animouse_db"
    ).fallbackToDestructiveMigration().build()

    private val _currentStatus = MutableLiveData<String?>()
    val currentStatus: LiveData<String?> = _currentStatus

    fun loadStatus(animeId: Int) {
        viewModelScope.launch {
            val entity = database.userAnimeDao().getAnimeById(animeId)
            _currentStatus.value = entity?.status
        }
    }

    fun updateStatus(animeId: Int, newStatus: String?) {
        viewModelScope.launch {
            if (newStatus == null || newStatus == "NONE") {
                database.userAnimeDao().deleteById(animeId)
            } else {
                database.userAnimeDao().insert(UserAnimeEntity(animeId, newStatus))
            }
            _currentStatus.value = newStatus
        }
    }
}