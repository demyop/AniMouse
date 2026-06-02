package com.example.animouse.ui.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.animouse.DetailsActivity
import com.example.animouse.R
import com.example.animouse.databinding.ItemAnimeOngoingBinding
import com.example.animouse.data.model.Anime

class AnimeAdapter(
    private val animeList: List<Anime>,
    private val statuses: Map<Int, String>,
    private val onLongClick: (Anime, View) -> Unit // МЕНЯЕМ ЛЯМБДУ на долгое нажатие
) : RecyclerView.Adapter<AnimeAdapter.AnimeViewHolder>() {

    private val statusLabels = mapOf(
        "WATCHING" to "Смотрю",
        "PLANNED" to "В планах",
        "COMPLETED" to "Просмотрено",
        "DROPPED" to "Брошено"
    )

    inner class AnimeViewHolder(private val binding: ItemAnimeOngoingBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(anime: Anime) {
            binding.textTitle.text = anime.title.romaji ?: "Без названия"
            binding.textScore.text = (anime.averageScore ?: 0).toString()

            Glide.with(binding.root.context)
                .load(anime.coverImage.large)
                .into(binding.imagePoster)

            val nextEp = anime.nextAiringEpisode?.episode
            if (nextEp != null) {
                binding.textEpisodes.text = "${nextEp - 1} eps"
            } else {
                binding.textEpisodes.text = "0 eps"
            }

            // --- НОВАЯ ЛОГИКА ЦВЕТНЫХ ТЕГОВ СПИСКОВ ---
            val currentStatus = statuses[anime.id]
            if (currentStatus != null && currentStatus != "NONE") {
                binding.badgeStatus.visibility = View.VISIBLE
                binding.badgeStatus.text = statusLabels[currentStatus] ?: currentStatus

                // Красим фон плашки в зависимости от статуса
                when(currentStatus) {
                    "WATCHING" -> binding.badgeStatus.setBackgroundResource(R.drawable.bg_badge_turquoise)
                    "PLANNED" -> binding.badgeStatus.setBackgroundResource(R.drawable.bg_badge_orange)
                    "COMPLETED" -> binding.badgeStatus.setBackgroundResource(R.drawable.bg_badge_green) // ДОБАВИЛИ ЗЕЛЕНЫЙ
                    else -> binding.badgeStatus.setBackgroundResource(R.drawable.bg_badge_neutral)
                }

            } else {
                binding.badgeStatus.visibility = View.GONE
            }

            // --- ОБРАБОТКА ДОЛГОГО НАЖАТИЯ на всю карточку ---
            binding.root.setOnLongClickListener { view ->
                onLongClick(anime, view)
                true // true означает, что событие обработано и обычный клик не сработает
            }

            // Обычный клик для перехода на экран деталей
            binding.root.setOnClickListener {
                val context = binding.root.context
                val intent = Intent(context, DetailsActivity::class.java).apply {
                    putExtra("EXTRA_ID", anime.id)
                    putExtra("EXTRA_TITLE", anime.title.romaji)
                    putExtra("EXTRA_POSTER", anime.coverImage.large)
                    putExtra("EXTRA_SCORE", anime.averageScore ?: 0)
                }
                context.startActivity(intent)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AnimeViewHolder {
        val binding = ItemAnimeOngoingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AnimeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AnimeViewHolder, position: Int) {
        holder.bind(animeList[position])
    }

    override fun getItemCount() = animeList.size
}