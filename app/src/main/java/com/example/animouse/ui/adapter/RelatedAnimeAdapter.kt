package com.example.animouse.ui.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.animouse.data.model.RelationEdge
import com.example.animouse.databinding.ItemAnimeRelatedBinding
import com.example.animouse.ui.activity.DetailsActivity

class RelatedAnimeAdapter(private val relations: List<RelationEdge>) :
    RecyclerView.Adapter<RelatedAnimeAdapter.RelatedViewHolder>() {

    inner class RelatedViewHolder(private val binding: ItemAnimeRelatedBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(edge: RelationEdge) {
            val anime = edge.node
            binding.textRelatedTitle.text = anime.title.romaji ?: "Без названия"
            binding.textRelatedType.text = translateRelation(edge.relationType)

            Glide.with(binding.root.context).load(anime.coverImage.large).into(binding.imageRelatedPoster)

            // При клике открываем новую карточку поверх старой!
            binding.root.setOnClickListener {
                val context = binding.root.context
                val intent = Intent(context, DetailsActivity::class.java).apply {
                    putExtra("EXTRA_ID", anime.id)
                    putExtra("EXTRA_ID_MAL", anime.idMal ?: -1)
                    putExtra("EXTRA_TITLE", anime.title.romaji)
                    putExtra("EXTRA_POSTER", anime.coverImage.large)
                    putExtra("EXTRA_SCORE", anime.averageScore ?: 0)
                }
                context.startActivity(intent)
            }
        }
    }

    // Переводим типы связей на русский
    private fun translateRelation(type: String?): String {
        return when (type) {
            "ADAPTATION" -> "Адаптация"
            "PREQUEL" -> "Приквел"
            "SEQUEL" -> "Сиквел"
            "PARENT" -> "Основная история"
            "SIDE_STORY" -> "Спин-офф"
            "CHARACTER" -> "Персонаж"
            "SUMMARY" -> "Саммари"
            "ALTERNATIVE" -> "Альтернатива"
            "SPIN_OFF" -> "Спин-офф"
            else -> type ?: "Связанное"
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RelatedViewHolder {
        val binding = ItemAnimeRelatedBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RelatedViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RelatedViewHolder, position: Int) {
        holder.bind(relations[position])
    }

    override fun getItemCount() = relations.size
}