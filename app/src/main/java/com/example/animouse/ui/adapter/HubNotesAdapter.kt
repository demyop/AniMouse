package com.example.animouse.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.example.animouse.data.database.NoteWithAnime
import com.example.animouse.databinding.ItemHubNoteBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HubNotesAdapter(
    private var notes: List<NoteWithAnime>,
    private val onNoteClick: (NoteWithAnime) -> Unit,
    private val onNoteLongClick: (NoteWithAnime) -> Unit // <-- ЛИСТЕНЕР ДЛЯ УДАЛЕНИЯ
) : RecyclerView.Adapter<HubNotesAdapter.HubNoteViewHolder>() {

    inner class HubNoteViewHolder(private val binding: ItemHubNoteBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(note: NoteWithAnime) {
            binding.textAnimeTitle.text = note.animeTitle
            binding.textNoteContent.text = note.content

            val sdf = SimpleDateFormat("d MMM, HH:mm", Locale.getDefault())
            binding.textNoteCreated.text = "Создана: ${sdf.format(Date(note.createdAt))}"

            if (note.updatedAt - note.createdAt > 2000) {
                binding.textNoteChanged.visibility = View.VISIBLE
                binding.textNoteChanged.text = "Изм.: ${sdf.format(Date(note.updatedAt))}"
            } else {
                binding.textNoteChanged.visibility = View.GONE
            }

            if (note.animePosterUrl != null) {
                Glide.with(binding.root.context)
                    .load(note.animePosterUrl)
                    .transform(CenterCrop())
                    .into(binding.imageAnimePoster)
            } else {
                binding.imageAnimePoster.setImageDrawable(null)
            }

            // Обычный клик открывает детали
            binding.root.setOnClickListener { onNoteClick(note) }

            // Долгий клик вызывает диалог удаления
            binding.root.setOnLongClickListener {
                onNoteLongClick(note)
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HubNoteViewHolder {
        val binding = ItemHubNoteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HubNoteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HubNoteViewHolder, position: Int) {
        holder.bind(notes[position])
    }

    override fun getItemCount() = notes.size

    fun updateData(newNotes: List<NoteWithAnime>) {
        notes = newNotes
        notifyDataSetChanged()
    }
}