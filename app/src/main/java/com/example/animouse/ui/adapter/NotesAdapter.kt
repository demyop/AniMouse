package com.example.animouse.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.animouse.data.database.NoteEntity
import com.example.animouse.databinding.ItemNoteBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotesAdapter(
    private val onEditClick: (NoteEntity) -> Unit,
    private val onDeleteClick: (NoteEntity) -> Unit
) : RecyclerView.Adapter<NotesAdapter.NoteViewHolder>() {

    private var notes: List<NoteEntity> = emptyList()

    fun submitList(newNotes: List<NoteEntity>) {
        notes = newNotes
        notifyDataSetChanged()
    }

    inner class NoteViewHolder(private val binding: ItemNoteBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(note: NoteEntity) {
            binding.textNoteContent.text = note.content

            // Форматируем время
            val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
            val createdStr = sdf.format(Date(note.createdAt))
            val updatedStr = sdf.format(Date(note.updatedAt))

            // Если время обновления больше времени создания хотя бы на пару секунд — выводим обе даты
            if (note.updatedAt - note.createdAt > 1000) {
                binding.textNoteDate.text = "Создано: $createdStr (Изм: $updatedStr)"
            } else {
                binding.textNoteDate.text = "Создано: $createdStr"
            }

            binding.btnEditNote.setOnClickListener { onEditClick(note) }
            binding.btnDeleteNote.setOnClickListener { onDeleteClick(note) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val binding = ItemNoteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NoteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        holder.bind(notes[position])
    }

    override fun getItemCount() = notes.size
}