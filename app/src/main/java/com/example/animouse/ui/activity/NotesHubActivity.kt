package com.example.animouse.ui.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.animouse.data.database.AppDatabase
import com.example.animouse.databinding.ActivityNotesHubBinding
import com.example.animouse.ui.adapter.HubNotesAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NotesHubActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNotesHubBinding
    private lateinit var database: AppDatabase
    private lateinit var adapter: HubNotesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotesHubBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = androidx.room.Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "animouse_db"
        ).build()

        // Кнопка "Назад" в тулбаре
        binding.toolbarNotes.setNavigationOnClickListener {
            finish()
        }

        setupRecyclerView()
    }

    override fun onResume() {
        super.onResume()
        loadNotes() // Перезагружаем заметки каждый раз, когда возвращаемся на экран (вдруг мы что-то изменили)
    }

    private fun setupRecyclerView() {
        adapter = HubNotesAdapter(
            notes = emptyList(),
            onNoteClick = { note ->
                val intent = Intent(this, DetailsActivity::class.java).apply {
                    putExtra("EXTRA_ID", note.animeId)
                    putExtra("EXTRA_ID_MAL", note.idMal)
                    putExtra("EXTRA_TITLE", note.animeTitle)
                    putExtra("EXTRA_POSTER", note.animePosterUrl)
                    putExtra("EXTRA_SCORE", 0)
                    putExtra("EXTRA_EPISODES_TOTAL", 0)
                }
                startActivity(intent)
            },
            onNoteLongClick = { note ->
                // Диалог подтверждения удаления заметки
                com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                    .setTitle("Удалить заметку?")
                    .setMessage("Вы уверены, что хотите навсегда удалить эту заметку для аниме «${note.animeTitle}»?")
                    .setPositiveButton("Удалить") { _, _ ->
                        // Удаляем из базы данных через корутину
                        lifecycleScope.launch(Dispatchers.IO) {
                            database.noteDao().deleteById(note.noteId)
                            // Обновляем список на главном потоке
                            withContext(Dispatchers.Main) {
                                loadNotes()
                                android.widget.Toast.makeText(this@NotesHubActivity, "Заметка удалена", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    .setNegativeButton("Отмена", null)
                    .show()
            }
        )
        binding.recyclerNotes.layoutManager = LinearLayoutManager(this)
        binding.recyclerNotes.adapter = adapter
    }

    private fun loadNotes() {
        lifecycleScope.launch(Dispatchers.IO) {
            val notesList = database.noteDao().getAllNotesWithAnime()

            withContext(Dispatchers.Main) {
                if (notesList.isEmpty()) {
                    binding.recyclerNotes.visibility = View.GONE
                    binding.textEmptyNotes.visibility = View.VISIBLE
                } else {
                    binding.recyclerNotes.visibility = View.VISIBLE
                    binding.textEmptyNotes.visibility = View.GONE
                    adapter.updateData(notesList)
                }
            }
        }
    }
}