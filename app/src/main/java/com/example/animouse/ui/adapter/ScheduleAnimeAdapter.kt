package com.example.animouse.ui.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.animouse.DetailsActivity
import com.example.animouse.R
import com.example.animouse.databinding.ItemAnimeScheduleBinding
import com.example.animouse.data.model.Anime
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ScheduleAnimeAdapter(
    private val animeList: List<Anime>,
    private val savedIds: Set<Int>,
    private val onBellClick: (Int, Boolean) -> Unit // Лямбда для клика по колокольчику
) : RecyclerView.Adapter<ScheduleAnimeAdapter.ScheduleViewHolder>() {

    inner class ScheduleViewHolder(private val binding: ItemAnimeScheduleBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(anime: Anime) {
            binding.textTitle.text = anime.title.romaji ?: "Без названия"
            binding.textScore.text = (anime.averageScore ?: 0).toString()

            Glide.with(binding.root.context)
                .load(anime.coverImage.large)
                .into(binding.imagePoster)

            // Конвертируем время выхода из timestamp в формат "ЧЧ:ММ"
            val airingAt = anime.nextAiringEpisode?.airingAt
            if (airingAt != null) {
                val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                val date = Date(airingAt * 1000L) // Умножаем на 1000, т.к. время в секундах
                binding.textTime.text = sdf.format(date)
            } else {
                binding.textTime.text = "--:--"
            }

            // Проверяем, есть ли тайтл в списках пользователя
            val isSaved = savedIds.contains(anime.id)
            if (isSaved) {
                binding.iconNotification.setImageResource(R.drawable.ic_notification_bell_alarm_sol)
                binding.iconNotification.setColorFilter(binding.root.context.getColor(R.color.orange_accent))
            } else {
                binding.iconNotification.setImageResource(R.drawable.ic_notification_bell_alarm_reg)
                binding.iconNotification.setColorFilter(binding.root.context.getColor(R.color.text_secondary))
            }

            // Обработка клика по колокольчику (добавление/удаление из списков)
            binding.iconNotification.setOnClickListener {
                onBellClick(anime.id, !isSaved)
            }

            // Обработка клика по самой карточке (переход на экран деталей)
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScheduleViewHolder {
        val binding = ItemAnimeScheduleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ScheduleViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ScheduleViewHolder, position: Int) {
        holder.bind(animeList[position])
    }

    override fun getItemCount() = animeList.size
}