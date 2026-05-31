package com.example.animouse.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.animouse.data.model.Anime
import com.example.animouse.databinding.ItemAnimeBinding

class AnimeAdapter(
    private val animeList: List<Anime>
) : RecyclerView.Adapter<AnimeAdapter.AnimeViewHolder>() {

    inner class AnimeViewHolder(
        private val binding: ItemAnimeBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(anime: Anime) {

            binding.textTitle.text =
                anime.title.romaji

            binding.textEpisode.text =
                "Следующий эпизод: ${
                    anime.nextAiringEpisode?.episode ?: "?"
                }"

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