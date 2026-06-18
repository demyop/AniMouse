package com.example.animouse.ui.adapter

import android.content.Intent
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.graphics.Color
import androidx.recyclerview.widget.RecyclerView
import com.example.animouse.data.model.Anime
import com.example.animouse.ui.activity.DetailsActivity
import com.example.animouse.ui.viewmodel.MainViewModel

// ✅ ДОБАВИЛИ ИМПОРТ НАШЕЙ КАРТОЧКИ (Иначе она будет красной)
import com.example.animouse.ui.compose.AnimeCard

class AnimeAdapter(
    private val animeList: List<Anime>,
    private val statuses: Map<Int, String>,
    private val customBadges: Map<Int, List<MainViewModel.CustomFolderPreview>> = emptyMap(),
    // ✅ 1. ПЕРЕИМЕНОВАЛИ ПАРАМЕТР, чтобы компилятор не путался
    private val onLongClick: (Anime, View?) -> Unit
) : RecyclerView.Adapter<AnimeAdapter.ComposeViewHolder>() {

    private val statusLabels = mapOf(
        "WATCHING" to "Смотрю",
        "PLANNED" to "В планах",
        "COMPLETED" to "Просмотрено",
        "DROPPED" to "Брошено"
    )

    inner class ComposeViewHolder(val composeView: ComposeView) : RecyclerView.ViewHolder(composeView) {
        fun bind(anime: Anime) {

            val currentStatus = statuses[anime.id]
            var displayStatus: String? = null
            var statusColor = Color.Transparent

            if (currentStatus != null && currentStatus != "NONE") {
                displayStatus = statusLabels[currentStatus] ?: currentStatus
                statusColor = when(currentStatus) {
                    "WATCHING" -> Color(0xFF00BFA5)
                    "PLANNED" -> Color(0xFFFF9800)
                    "COMPLETED" -> Color(0xFF4CAF50)
                    else -> Color(0xFF757575)
                }
            }

            // 2. Рендерим Compose-карточку
            composeView.setContent {
                AnimeCard(
                    anime = anime,
                    systemStatusText = displayStatus,
                    systemStatusColor = statusColor,
                    customBadges = customBadges[anime.id] ?: emptyList(),
                    onClick = {
                        val context = composeView.context
                        val intent = Intent(context, DetailsActivity::class.java).apply {
                            putExtra("EXTRA_ID", anime.id)
                            putExtra("EXTRA_ID_MAL", anime.idMal ?: -1)
                            putExtra("EXTRA_TITLE", anime.title.romaji)
                            putExtra("EXTRA_POSTER", anime.coverImage.large)
                            putExtra("EXTRA_SCORE", anime.averageScore ?: 0)
                            putExtra("EXTRA_DESC_ENG", anime.description)
                            putExtra("EXTRA_EPISODES_TOTAL", anime.episodes ?: 0)
                            // ✅ 2. Явно указали java.util.ArrayList, чтобы студия не ругалась
                            putStringArrayListExtra("EXTRA_GENRES", java.util.ArrayList(anime.genres ?: emptyList()))
                        }
                        context.startActivity(intent)
                    },
                    onLongClick = {
                        // ✅ 3. ТЕПЕРЬ ИСПОЛЬЗУЕМ НОВОЕ ИМЯ! Конфликт исчерпан.
                        onLongClick(anime, composeView)
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

    override fun onBindViewHolder(holder: ComposeViewHolder, position: Int) {
        holder.bind(animeList[position])
    }

    override fun getItemCount() = animeList.size
}