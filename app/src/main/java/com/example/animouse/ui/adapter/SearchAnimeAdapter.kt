package com.example.animouse.ui.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.animouse.R
import com.example.animouse.data.model.ShikimoriSearchResult
import com.example.animouse.databinding.ItemAnimeSearchBinding
import com.example.animouse.ui.activity.DetailsActivity

class SearchAnimeAdapter : RecyclerView.Adapter<SearchAnimeAdapter.SearchViewHolder>() {

    private var results: List<ShikimoriSearchResult> = emptyList()

    fun submitList(newResults: List<ShikimoriSearchResult>) {
        results = newResults
        notifyDataSetChanged()
    }

    inner class SearchViewHolder(private val binding: ItemAnimeSearchBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(anime: ShikimoriSearchResult) {
            binding.textSearchTitle.text = anime.russian ?: anime.name

            // 1. Вычисляем ГОД и СЕЗОН из даты aired_on (формат "YYYY-MM-DD")
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

            // 2. Формируем финальную строку подзаголовка
            val scoreText = if (anime.score != "0.0" && anime.score != null) "⭐ ${anime.score}" else "⭐ —"
            binding.textSearchSubtitle.text = "$seasonYearText Эп: ${anime.episodes_aired}/${if (anime.episodes > 0) anime.episodes else "?"} • $scoreText"

            // --- ОБРАБОТКА СТАТУСА ---
            when (anime.status) {
                "ongoing" -> {
                    binding.textSearchStatus.text = "Онгоинг"
                    binding.textSearchStatus.setBackgroundResource(R.drawable.bg_badge_turquoise)
                    binding.textSearchStatus.visibility = android.view.View.VISIBLE
                }
                "anons" -> {
                    binding.textSearchStatus.text = "Анонс"
                    binding.textSearchStatus.setBackgroundResource(R.drawable.bg_badge_orange)
                    binding.textSearchStatus.visibility = android.view.View.VISIBLE
                }
                "released" -> {
                    binding.textSearchStatus.text = "Вышло"
                    binding.textSearchStatus.setBackgroundResource(R.drawable.bg_badge_green)
                    binding.textSearchStatus.visibility = android.view.View.VISIBLE
                }
                else -> binding.textSearchStatus.visibility = android.view.View.GONE
            }

            val posterUrl = "https://shikimori.one" + anime.image?.original
            Glide.with(binding.root.context).load(posterUrl).into(binding.imageSearchPoster)

            binding.root.setOnClickListener {
                val context = binding.root.context
                val intent = android.content.Intent(context, DetailsActivity::class.java).apply {
                    putExtra("EXTRA_ID", -1)
                    putExtra("EXTRA_ID_MAL", anime.id)
                    putExtra("EXTRA_TITLE", anime.russian ?: anime.name)
                    putExtra("EXTRA_POSTER", posterUrl)
                    val floatScore = anime.score?.toFloatOrNull() ?: 0f
                    putExtra("EXTRA_SCORE", (floatScore * 10).toInt())
                }
                context.startActivity(intent)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchViewHolder {
        val binding = ItemAnimeSearchBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SearchViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SearchViewHolder, position: Int) = holder.bind(results[position])

    override fun getItemCount() = results.size
}