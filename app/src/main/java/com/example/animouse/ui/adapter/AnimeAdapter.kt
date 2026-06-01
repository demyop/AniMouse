package com.example.animouse.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.animouse.data.model.Anime
import com.example.animouse.databinding.ItemAnimeBinding

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AnimeAdapter(
    private val animeList: List<Anime>
) : RecyclerView.Adapter<AnimeAdapter.AnimeViewHolder>() {

    inner class AnimeViewHolder(
        private val binding: ItemAnimeBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(anime: Anime) {

            val isFavorite =
                FavoriteManager.favoriteIds.contains(anime.id)

            binding.buttonFavorite.setImageResource(
                if (isFavorite)
                    android.R.drawable.btn_star_big_on
                else
                    android.R.drawable.btn_star_big_off
            )

            binding.buttonFavorite.setOnClickListener {

                if (FavoriteManager.favoriteIds.contains(anime.id)) {

                    FavoriteManager.favoriteIds.remove(anime.id)

                } else {

                    FavoriteManager.favoriteIds.add(anime.id)

                }

                notifyItemChanged(adapterPosition)
            }

            binding.textTitle.text =
                anime.title.romaji

            val nextEpisode =
                anime.nextAiringEpisode

            if (nextEpisode != null) {

                val date = Date(
                    nextEpisode.airingAt * 1000
                )

                val formatter =
                    SimpleDateFormat(
                        "dd.MM.yyyy HH:mm",
                        Locale.getDefault()
                    )

                binding.textEpisode.text =
                    "Эпизод: ${nextEpisode.episode}\n" +
                            "Выход: ${formatter.format(date)}"

            } else {

                binding.textEpisode.text =
                    "Дата выхода неизвестна"
            }

            Glide.with(binding.root)
                .load(anime.coverImage.large)
                .into(binding.imagePoster)
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): AnimeViewHolder {

        val binding =
            ItemAnimeBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )

        return AnimeViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: AnimeViewHolder,
        position: Int
    ) {
        holder.bind(animeList[position])
    }

    override fun getItemCount() =
        animeList.size
}