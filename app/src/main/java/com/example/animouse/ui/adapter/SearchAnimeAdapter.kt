package com.example.animouse.ui.adapter

import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.recyclerview.widget.RecyclerView
import com.example.animouse.data.model.ShikimoriSearchResult
import com.example.animouse.ui.activity.DetailsActivity
import com.example.animouse.ui.compose.SearchAnimeCard

class SearchAnimeAdapter : RecyclerView.Adapter<SearchAnimeAdapter.ComposeViewHolder>() {

    private var results: List<ShikimoriSearchResult> = emptyList()

    fun submitList(newResults: List<ShikimoriSearchResult>) {
        results = newResults
        notifyDataSetChanged()
    }

    inner class ComposeViewHolder(val composeView: ComposeView) : RecyclerView.ViewHolder(composeView) {
        fun bind(anime: ShikimoriSearchResult) {

            // 1. Вычисляем год и сезон (оставляем логику здесь, чтобы не грузить Compose)
            var seasonYearText = ""
            val airedOn = anime.aired_on
            if (!airedOn.isNullOrEmpty() && airedOn.length >= 7) {
                val year = airedOn.substring(0, 4)
                val month = airedOn.substring(5, 7).toIntOrNull() ?: 0

                val season = when (month) {
                    in 1..3 -> "Зима"
                    in 4..6 -> "Весна"
                    in 7..9 -> "Лето"
                    in 10..12 -> "Осень"
                    else -> ""
                }
                seasonYearText = if (season.isNotEmpty()) "$year • $season • " else "$year • "
            }

            // 2. Рендерим Compose-карточку
            composeView.setContent {
                SearchAnimeCard(
                    anime = anime,
                    seasonYearText = seasonYearText,
                    onClick = {
                        val context = composeView.context
                        val intent = android.content.Intent(context, DetailsActivity::class.java).apply {
                            putExtra("EXTRA_ID", -1)
                            putExtra("EXTRA_ID_MAL", anime.id)
                            putExtra("EXTRA_TITLE", anime.russian ?: anime.name)
                            putExtra("EXTRA_POSTER", "https://shikimori.one${anime.image?.original}")
                            val floatScore = anime.score?.toFloatOrNull() ?: 0f
                            putExtra("EXTRA_SCORE", (floatScore * 10).toInt())
                        }
                        context.startActivity(intent)
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

    override fun onBindViewHolder(holder: ComposeViewHolder, position: Int) = holder.bind(results[position])

    override fun getItemCount() = results.size
}