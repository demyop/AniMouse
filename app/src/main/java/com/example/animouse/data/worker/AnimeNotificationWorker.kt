package com.example.animouse.data.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.animouse.R
import com.example.animouse.data.NotificationHelper
import com.example.animouse.ui.activity.MainActivity

class AnimeNotificationWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val animeId = inputData.getInt("ANIME_ID", -1)
        val title = inputData.getString("ANIME_TITLE") ?: "Аниме"
        val episode = inputData.getInt("EPISODE", -1)

        // 1. Проверяем, не выключил ли юзер колокольчик, пока ждал серию
        if (!NotificationHelper.isNotificationEnabled(context, animeId)) {
            return Result.success()
        }

        // 2. Показываем уведомление
        showNotification(animeId, title, episode)

        // 3. Запускаем перепланирование на следующую неделю
        // Так как серия только что вышла, через пару часов AniList обновит nextAiringEpisode.
        // Вызываем метод перепланирования с задержкой (например, через NotificationHelper)
        // Для простоты, пока оставим так, а обновление можно будет повесить на открытие приложения.

        return Result.success()
    }

    private fun showNotification(animeId: Int, title: String, episode: Int) {
        val channelId = "animouse_releases"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Создаем канал для Android 8.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Выход новых серий",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        // Интент для открытия приложения по клику на уведомление
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, animeId, intent, PendingIntent.FLAG_IMMUTABLE
        )

        val largeIconBitmap = android.graphics.BitmapFactory.decodeResource(context.resources, R.drawable.ic_launcher_notification)

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification_bell_alarm_sol)
            .setLargeIcon(largeIconBitmap)
            .setContentTitle("Вышла $episode серия!")
            .setContentText("Тайтл «$title» уже доступен к просмотру.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(animeId, notification)
    }
}