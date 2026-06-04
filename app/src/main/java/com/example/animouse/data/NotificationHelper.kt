package com.example.animouse.data

import android.content.Context

object NotificationHelper {
    private const val PREFS_NAME = "animouse_notifications"
    private const val KEY_NOTIFIED_IDS = "notified_anime_ids"

    // Проверяем, включены ли уведомления для конкретного аниме
    fun isNotificationEnabled(context: Context, animeId: Int): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedIds = prefs.getStringSet(KEY_NOTIFIED_IDS, emptySet()) ?: emptySet()
        return savedIds.contains(animeId.toString())
    }

    // Переключаем статус колокольчика (возвращает true, если включили, false — если выключили)
    fun toggleNotification(context: Context, animeId: Int): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedIds = prefs.getStringSet(KEY_NOTIFIED_IDS, emptySet())?.toMutableSet() ?: mutableSetOf()

        val idString = animeId.toString()
        val isEnabledNow = if (savedIds.contains(idString)) {
            savedIds.remove(idString)
            false
        } else {
            savedIds.add(idString)
            true
        }

        prefs.edit().putStringSet(KEY_NOTIFIED_IDS, savedIds).apply()
        return isEnabledNow
    }
}