package com.example.animouse.ui.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.animouse.R
import com.example.animouse.ui.activity.DetailsActivity
import com.example.animouse.databinding.ItemAnimeScheduleBinding
import com.example.animouse.data.model.Anime
import com.example.animouse.data.NotificationHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ScheduleAnimeAdapter(
    private val animeList: List<Anime>,
    private val customBadges: Map<Int, List<com.example.animouse.ui.activity.MainViewModel.CustomFolderPreview>> = emptyMap()
) : RecyclerView.Adapter<ScheduleAnimeAdapter.ScheduleViewHolder>() {

    inner class ScheduleViewHolder(private val binding: ItemAnimeScheduleBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(anime: Anime) {
            binding.textTitle.text = anime.title.romaji ?: "Без названия"
            binding.textScore.text = (anime.averageScore ?: 0).toString()

            Glide.with(binding.root.context)
                .load(anime.coverImage.large)
                .into(binding.imagePoster)

// === 1. ЛОГИКА ЭПИЗОДОВ (? / ? eps) ===
            val nextEp = anime.nextAiringEpisode?.episode
            val totalEp = if (anime.episodes != null && anime.episodes > 0) anime.episodes.toString() else "?"

            if (nextEp != null) {
                binding.textEpisodes.text = "  $nextEp / $totalEp eps" // Добавили точку
            } else {
                binding.textEpisodes.text = "  ? / $totalEp eps" // Добавили точку
            }

            // === 2. ЛОГИКА КАСТОМНЫХ ТЕГОВ ПАПОК ===
            val customBadgesList = customBadges[anime.id] ?: emptyList()
            binding.layoutCustomBadges.removeAllViews()

            if (customBadgesList.isNotEmpty()) {
                binding.scrollBadges.visibility = android.view.View.VISIBLE
                val context = binding.root.context
                val density = context.resources.displayMetrics.density

                for (badge in customBadgesList) {
                    val badgeView = android.widget.TextView(context).apply {
                        text = badge.name
                        textSize = 10f
                        setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.bg_dark_deep))
                        typeface = android.graphics.Typeface.DEFAULT_BOLD
                        setPadding((6 * density).toInt(), (2 * density).toInt(), (6 * density).toInt(), (2 * density).toInt())

                        background = androidx.core.content.ContextCompat.getDrawable(context, R.drawable.bg_badge_turquoise)
                        backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor(badge.colorHex))

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
                binding.scrollBadges.visibility = android.view.View.GONE
            }

            // Время выхода
            val airingAt = anime.nextAiringEpisode?.airingAt
            if (airingAt != null) {
                val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                val date = Date(airingAt * 1000L)
                binding.textTime.text = sdf.format(date)
            } else {
                binding.textTime.text = "--:--"
            }

            // Логика колокольчика
            val context = binding.root.context
            fun updateBellUI(isActive: Boolean) {
                if (isActive) {
                    binding.iconNotification.setImageResource(R.drawable.ic_notification_bell_alarm_sol)
                    binding.iconNotification.setColorFilter(context.getColor(R.color.orange_accent))
                } else {
                    binding.iconNotification.setImageResource(R.drawable.ic_notification_bell_alarm_reg)
                    binding.iconNotification.setColorFilter(context.getColor(R.color.text_secondary))
                }
            }

            val isNotifying = NotificationHelper.isNotificationEnabled(context, anime.id)
            updateBellUI(isNotifying)

            binding.iconNotification.setOnClickListener {
                val airingAtSafe = anime.nextAiringEpisode?.airingAt?.toLong() ?: 0L
                val episode = anime.nextAiringEpisode?.episode ?: 0
                val title = anime.title.romaji ?: "Аниме"

                val newState = NotificationHelper.toggleNotification(
                    context, anime.id, title, airingAtSafe, episode
                )
                updateBellUI(newState)

                val msg = if (newState) "Мыш напомнит!₍ᐢ·͈༝·͈ᐢ₎" else "Промолчу!⚞ • ⚟"
                android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
            }

            // Переход на детали
            binding.root.setOnClickListener {
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScheduleViewHolder {
        val binding = ItemAnimeScheduleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ScheduleViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ScheduleViewHolder, position: Int) = holder.bind(animeList[position])
    override fun getItemCount() = animeList.size
}