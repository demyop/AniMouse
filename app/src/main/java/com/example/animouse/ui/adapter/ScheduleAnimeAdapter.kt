package com.example.animouse.ui.adapter

import android.content.Intent
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.recyclerview.widget.RecyclerView
import com.example.animouse.data.NotificationHelper
import com.example.animouse.data.model.Anime
import com.example.animouse.ui.activity.DetailsActivity
import com.example.animouse.ui.compose.AnimeListCard
import com.example.animouse.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ScheduleAnimeAdapter(
    private val animeList: List<Anime>,
    private val customBadges: Map<Int, List<MainViewModel.CustomFolderPreview>> = emptyMap(),
) : RecyclerView.Adapter<ScheduleAnimeAdapter.ComposeViewHolder>() {

    inner class ComposeViewHolder(val composeView: ComposeView) : RecyclerView.ViewHolder(composeView) {
        fun bind(anime: Anime) {
            val context = composeView.context

            // 1. Форматируем время выхода серии
            val airingAt = anime.nextAiringEpisode?.airingAt
            val formattedTime = if (airingAt != null) {
                val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                sdf.format(Date(airingAt * 1000L))
            } else {
                "--:--"
            }

            // 2. Получаем текущий статус колокольчика
            val isNotifying = NotificationHelper.isNotificationEnabled(context, anime.id)

            // 3. Рендерим Compose-карточку
            composeView.setContent {
                AnimeListCard(
                    anime = anime,
                    customBadges = customBadges[anime.id] ?: emptyList(),
                    airingTime = formattedTime,
                    isNotificationEnabled = isNotifying,
                    onNotificationClick = {
                        val airingAtSafe = anime.nextAiringEpisode?.airingAt?.toLong() ?: 0L
                        val episode = anime.nextAiringEpisode?.episode ?: 0
                        val title = anime.title.romaji ?: "Аниме"

                        val newState = NotificationHelper.toggleNotification(
                            context, anime.id, title, airingAtSafe, episode
                        )

                        // Заставляем адаптер перерисовать ЭТУ конкретную карточку, чтобы колокольчик поменял цвет
                        notifyItemChanged(bindingAdapterPosition)

                        val msg = if (newState) "Мыш напомнит! ₍ᐢ·͈༝·͈ᐢ₎" else "Промолчу! ⚞ • ⚟"
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    },
                    onClick = {
                        val intent = Intent(context, DetailsActivity::class.java).apply {
                            putExtra("EXTRA_ID", anime.id)
                            putExtra("EXTRA_ID_MAL", anime.idMal ?: -1)
                            putExtra("EXTRA_TITLE", anime.title.romaji)
                            putExtra("EXTRA_POSTER", anime.coverImage.large)
                            putExtra("EXTRA_SCORE", anime.averageScore ?: 0)
                            putExtra("EXTRA_DESC_ENG", anime.description)
                            putExtra("EXTRA_EPISODES_TOTAL", anime.episodes ?: 0)
                            putStringArrayListExtra("EXTRA_GENRES", java.util.ArrayList(anime.genres ?: emptyList()))
                        }
                        context.startActivity(intent)
                    },
                    onLongClick = {
                        // В расписании у нас пока нет обработки долгого клика, поэтому оставляем пустым
                    }
                )
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ComposeViewHolder {
        val composeView = ComposeView(parent.context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        }
        return ComposeViewHolder(composeView)
    }

    override fun onBindViewHolder(holder: ComposeViewHolder, position: Int) = holder.bind(animeList[position])
    override fun getItemCount() = animeList.size
}