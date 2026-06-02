package com.example.animouse.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.example.animouse.R
import com.example.animouse.databinding.ItemListFolderBinding

// Вспомогательный класс для хранения данных папки
data class FolderItem(
    val statusId: String,       // "WATCHING", "PLANNED" и т.д.
    val title: String,          // "Смотрю", "В планах"
    val count: Int,             // Количество тайтлов
    val posterUrl: String?,     // Ссылка на случайный постер
    val indicatorColorRes: Int  // Цвет боковой полоски
)

class ListsFolderAdapter(
    private val folders: List<FolderItem>,
    private val onFolderClick: (String) -> Unit // Клик для открытия папки
) : RecyclerView.Adapter<ListsFolderAdapter.FolderViewHolder>() {

    inner class FolderViewHolder(private val binding: ItemListFolderBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(folder: FolderItem) {
            binding.textListName.text = folder.title
            binding.textListCount.text = "Всего в списке: ${folder.count}"
            binding.indicatorColor.setBackgroundResource(folder.indicatorColorRes)

            // Если в папке есть аниме, грузим постер. Если нет — оставляем пустой фон.
            if (folder.posterUrl != null) {
                Glide.with(binding.root.context)
                    .load(folder.posterUrl)
                    .transform(CenterCrop()) // Чтобы постер красиво заполнял правую часть
                    .into(binding.imageRandomPoster)
            } else {
                binding.imageRandomPoster.setImageDrawable(null)
            }

            // Обрабатываем клик по всей карточке-папке
            binding.root.setOnClickListener {
                onFolderClick(folder.statusId)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FolderViewHolder {
        val binding = ItemListFolderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FolderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FolderViewHolder, position: Int) {
        holder.bind(folders[position])
    }

    override fun getItemCount() = folders.size
}