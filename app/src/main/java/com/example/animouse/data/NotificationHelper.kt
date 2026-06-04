package com.example.animouse.data

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.animouse.data.worker.AnimeNotificationWorker
import java.util.concurrent.TimeUnit

object NotificationHelper {
    private const val PREFS_NAME = "animouse_notifications"
    private const val KEY_NOTIFIED_IDS = "notified_anime_ids"

    fun isNotificationEnabled(context: Context, animeId: Int): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(KEY_NOTIFIED_IDS, emptySet())?.contains(animeId.toString()) == true
    }

    // Теперь метод принимает больше данных, чтобы завести таймер!
    fun toggleNotification(
        context: Context,
        animeId: Int,
        title: String,
        airingAtSec: Long,
        episode: Int
    ): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedIds = prefs.getStringSet(KEY_NOTIFIED_IDS, emptySet())?.toMutableSet() ?: mutableSetOf()

        val idString = animeId.toString()
        val isEnabledNow = if (savedIds.contains(idString)) {
            savedIds.remove(idString)
            cancelNotificationWork(context, animeId) // Отменяем таймер
            false
        } else {
            savedIds.add(idString)
            scheduleNotificationWork(context, animeId, title, airingAtSec, episode) // Заводим таймер
            true
        }

        prefs.edit().putStringSet(KEY_NOTIFIED_IDS, savedIds).apply()
        return isEnabledNow
    }

    private fun scheduleNotificationWork(context: Context, animeId: Int, title: String, airingAtSec: Long, episode: Int) {
        if (airingAtSec <= 0) return

        val currentTimeMillis = System.currentTimeMillis()

        // Магия времени: берем дату выхода из AniList и накидываем сверху 2 часа (в миллисекундах)
        val targetTimeMillis = (airingAtSec * 1000L) + (2 * 60 * 60 * 1000L)
        val delayMillis = targetTimeMillis - currentTimeMillis

        // Если с учетом двух часов серия уже давно в прошлом — не планируем
        if (delayMillis <= 0) return

        val inputData = Data.Builder()
            .putInt("ANIME_ID", animeId)
            .putString("ANIME_TITLE", title)
            .putInt("EPISODE", episode)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<AnimeNotificationWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setInputData(inputData)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "anime_release_$animeId",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    private fun cancelNotificationWork(context: Context, animeId: Int) {
        WorkManager.getInstance(context).cancelUniqueWork("anime_release_$animeId")
    }
}