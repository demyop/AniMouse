package com.example.animouse.ui.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.animouse.R
import com.example.animouse.databinding.ItemAnimeOngoingBinding
import com.example.animouse.data.model.Anime
import com.example.animouse.ui.activity.DetailsActivity

class AnimeAdapter(
    private val animeList: List<Anime>,
    private val statuses: Map<Int, String>,
    private val customBadges: Map<Int, List<com.example.animouse.ui.activity.MainViewModel.CustomFolderPreview>> = emptyMap(),
    private val onLongClick: (Anime, View) -> Unit
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

            // === 1. ПОЛЬЗОВАТЕЛЬСКИЕ СТАТУСЫ (Твоя логика) ===
            val currentStatus = statuses[anime.id]
            if (currentStatus != null && currentStatus != "NONE") {
                binding.badgeStatus.visibility = View.VISIBLE
                binding.badgeStatus.text = statusLabels[currentStatus] ?: currentStatus

                when(currentStatus) {
                    "WATCHING" -> binding.badgeStatus.setBackgroundResource(R.drawable.bg_badge_turquoise)
                    "PLANNED" -> binding.badgeStatus.setBackgroundResource(R.drawable.bg_badge_orange)
                    "COMPLETED" -> binding.badgeStatus.setBackgroundResource(R.drawable.bg_badge_green)
                    else -> binding.badgeStatus.setBackgroundResource(R.drawable.bg_badge_neutral)
                }
            } else {
                binding.badgeStatus.visibility = View.GONE
            }

            // ЛОГИКА ДЛЯ КАСТОМНОЙ ПЛАШКИ
// ДИНАМИЧЕСКАЯ ОТРИСОВКА КАСТОМНЫХ ПЛАШЕК
            val customBadgesList = customBadges[anime.id] ?: emptyList()
            binding.layoutCustomBadges.removeAllViews() // Очищаем от старых (особенность RecyclerView)

            if (customBadgesList.isNotEmpty()) {
                binding.layoutCustomBadges.visibility = android.view.View.VISIBLE
                val context = binding.root.context
                val density = context.resources.displayMetrics.density

                // Берем максимум 3 плашки, чтобы не закрыть всю обложку
                // Убрали take(3), теперь генерируем все плашки!
                for (badge in customBadgesList) {
                    val badgeView = android.widget.TextView(context).apply {
                        text = badge.name
                        textSize = 10f
                        setTextColor(androidx.core.content.ContextCompat.getColor(context, com.example.animouse.R.color.bg_dark_deep))
                        typeface = android.graphics.Typeface.DEFAULT_BOLD
                        setPadding((6 * density).toInt(), (2 * density).toInt(), (6 * density).toInt(), (2 * density).toInt())

                        // Берем наш красивый шейп и просто перекрашиваем его
                        background = androidx.core.content.ContextCompat.getDrawable(context, com.example.animouse.R.drawable.bg_badge_turquoise)
                        backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor(badge.colorHex))

                        // Добавляем отступ справа, чтобы они не слипались
                        layoutParams = android.widget.LinearLayout.LayoutParams(
                            android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
                        ).apply {
                            marginEnd = (4 * density).toInt()
                        }
                    }
                    binding.layoutCustomBadges.addView(badgeView)
                }
            } else {
                binding.layoutCustomBadges.visibility = android.view.View.GONE
            }

// === 2. НОВАЯ ЛОГИКА: СТАТУС ВЫПУСКА С СЕЗОНОМ ===
            val translatedSeason = when (anime.season?.uppercase()) {
                "WINTER" -> "Зима"
                "SPRING" -> "Весна"
                "SUMMER" -> "Лето"
                "FALL" -> "Осень"
                else -> ""
            }

            val seasonSuffix = if (anime.seasonYear != null && translatedSeason.isNotEmpty()) {
                " • ${anime.seasonYear} $translatedSeason"
            } else ""

            when (anime.status?.lowercase()) {
                "ongoing", "releasing" -> {
                    binding.textReleaseStatus.visibility = View.GONE
                }
                "anons", "upcoming" -> {
                    binding.textReleaseStatus.text = "Анонс$seasonSuffix"
                    // Подкрасим сам текст в оранжевый, чтобы выделить анонсы, но без тяжелого фона
                    binding.textReleaseStatus.setTextColor(androidx.core.content.ContextCompat.getColor(binding.root.context, R.color.orange_accent))
                    binding.textReleaseStatus.visibility = View.VISIBLE
                }
                "released", "finished" -> {
                    binding.textReleaseStatus.text = "Вышло$seasonSuffix"
                    // Оставляем нейтральный цвет вторичного текста
                    binding.textReleaseStatus.setTextColor(androidx.core.content.ContextCompat.getColor(binding.root.context, R.color.text_secondary))
                    binding.textReleaseStatus.visibility = View.VISIBLE
                }
                else -> binding.textReleaseStatus.visibility = View.GONE
            }
            // ========================================================

            binding.root.setOnLongClickListener { view ->
                onLongClick(anime, view)
                true
            }

            // Обычный клик для перехода на экран деталей
            binding.root.setOnClickListener {
                val context = binding.root.context
                val intent = Intent(context, DetailsActivity::class.java).apply {
                    putExtra("EXTRA_ID", anime.id)
                    putExtra("EXTRA_ID_MAL", anime.idMal ?: -1)
                    putExtra("EXTRA_TITLE", anime.title.romaji)
                    putExtra("EXTRA_POSTER", anime.coverImage.large)
                    putExtra("EXTRA_SCORE", anime.averageScore ?: 0)
                    putExtra("EXTRA_DESC_ENG", anime.description)
                    putExtra("EXTRA_EPISODES_TOTAL", anime.episodes ?: 0)
                    putStringArrayListExtra("EXTRA_GENRES", ArrayList(anime.genres ?: emptyList()))
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