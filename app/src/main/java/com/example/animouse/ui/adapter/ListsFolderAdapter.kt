package com.example.animouse.ui.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.example.animouse.databinding.ItemListFolderBinding

data class FolderItem(
    val id: String,             // Уникальный ID: "WATCHING", "PLANNED" или "custom_1"
    val title: String,
    val count: Int,
    val posterUrl: String?,
    val indicatorColorRes: Int? = null,   // Системный цвет (R.color...)
    val indicatorColorHex: String? = null, // Пользовательский цвет ("#FF0000")
    val isSeparator: Boolean = false
)

class ListsFolderAdapter(
    private val folders: List<FolderItem>,
    private val onFolderClick: (String) -> Unit,
    private val onFolderLongClick: (String) -> Unit // <-- ДОБАВИЛИ ВТОРОЙ ЛИСТЕНЕР
) : RecyclerView.Adapter<ListsFolderAdapter.FolderViewHolder>() {

    inner class FolderViewHolder(private val binding: ItemListFolderBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(folder: FolderItem) {
            if (folder.isSeparator) {
                binding.textSeparatorTitle.visibility = android.view.View.VISIBLE
                binding.textSeparatorTitle.text = folder.title
                binding.cardFolder.visibility = android.view.View.GONE
                return
            } else {
                binding.textSeparatorTitle.visibility = android.view.View.GONE
                binding.cardFolder.visibility = android.view.View.VISIBLE
            }

            binding.textListName.text = folder.title
            binding.textListCount.text = "Всего в списке: ${folder.count}"

            if (folder.indicatorColorHex != null) {
                binding.indicatorColor.setBackgroundColor(Color.parseColor(folder.indicatorColorHex))
            } else if (folder.indicatorColorRes != null) {
                binding.indicatorColor.setBackgroundResource(folder.indicatorColorRes)
            }

            if (folder.posterUrl != null) {
                Glide.with(binding.root.context)
                    .load(folder.posterUrl)
                    .transform(CenterCrop())
                    .into(binding.imageRandomPoster)
            } else {
                binding.imageRandomPoster.setImageDrawable(null)
            }

            // Обычный клик открывает папку
            binding.root.setOnClickListener { onFolderClick(folder.id) }

            // ДОЛГИЙ КЛИК вызывает меню управления
            binding.root.setOnLongClickListener {
                onFolderLongClick(folder.id)
                true
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