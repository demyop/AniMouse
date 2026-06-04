package com.example.animouse.ui.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.animouse.ui.activity.DetailsActivity
import com.example.animouse.R
import com.example.animouse.databinding.ItemAnimeScheduleBinding
import com.example.animouse.data.model.Anime
import com.example.animouse.data.NotificationHelper // Подключили наш менеджер
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ScheduleAnimeAdapter(
    private val animeList: List<Anime> // <-- Убрали старые параметры, они больше не нужны!
) : RecyclerView.Adapter<ScheduleAnimeAdapter.ScheduleViewHolder>() {

    inner class ScheduleViewHolder(private val binding: ItemAnimeScheduleBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(anime: Anime) {
            binding.textTitle.text = anime.title.romaji ?: "Без названия"
            binding.textScore.text = (anime.averageScore ?: 0).toString()

            Glide.with(binding.root.context)
                .load(anime.coverImage.large)
                .into(binding.imagePoster)

            // Конвертируем время выхода
            val airingAt = anime.nextAiringEpisode?.airingAt
            if (airingAt != null) {
                val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                val date = Date(airingAt * 1000L)
                binding.textTime.text = sdf.format(date)
            } else {
                binding.textTime.text = "--:--"
            }

            // === НОВАЯ ЛОГИКА УВЕДОМЛЕНИЙ ===
            val context = binding.root.context

            // Функция для покраски колокольчика
            fun updateBellUI(isActive: Boolean) {
                if (isActive) {
                    binding.iconNotification.setImageResource(R.drawable.ic_notification_bell_alarm_sol)
                    binding.iconNotification.setColorFilter(context.getColor(R.color.orange_accent))
                } else {
                    binding.iconNotification.setImageResource(R.drawable.ic_notification_bell_alarm_reg)
                    binding.iconNotification.setColorFilter(context.getColor(R.color.text_secondary))
                }
            }

            // Читаем текущий статус при отрисовке карточки
            val isNotifying = NotificationHelper.isNotificationEnabled(context, anime.id)
            updateBellUI(isNotifying)

            // Обработка клика по колокольчику
            binding.iconNotification.setOnClickListener {
                val newState = NotificationHelper.toggleNotification(context, anime.id)
                updateBellUI(newState) // Мгновенно перерисовываем

                val msg = if (newState) "Уведомления включены" else "Уведомления выключены"
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }

            // Переход на экран деталей
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

    override fun onBindViewHolder(holder: ScheduleViewHolder, position: Int) {
        holder.bind(animeList[position])
    }

    override fun getItemCount() = animeList.size
}