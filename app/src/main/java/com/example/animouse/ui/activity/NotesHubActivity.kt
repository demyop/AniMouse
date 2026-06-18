package com.example.animouse.ui.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import coil.compose.AsyncImage
import com.example.animouse.R
import com.example.animouse.data.database.AppDatabase
import com.example.animouse.data.database.NoteEntity
import com.example.animouse.data.database.NoteWithAnime
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotesHubActivity : AppCompatActivity() {

    private lateinit var database: AppDatabase

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        database = androidx.room.Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "animouse_db"
        ).build()

        setContent {
            var notes by remember { mutableStateOf(emptyList<NoteWithAnime>()) }

            // Стейт для диалога редактирования
            var editingNote by remember { mutableStateOf<NoteWithAnime?>(null) }
            val dateFormatter = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) }

            fun loadNotes() {
                lifecycleScope.launch(Dispatchers.IO) {
                    val notesList = database.noteDao().getAllNotesWithAnime()
                    withContext(Dispatchers.Main) { notes = notesList }
                }
            }

            LaunchedEffect(Unit) { loadNotes() }

            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Мои заметки", color = Color.White, fontWeight = FontWeight.Bold) },
                        navigationIcon = {
                            IconButton(onClick = { finish() }) { Icon(painterResource(R.drawable.ic_arrow_reg), "Назад", tint = Color.White) }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1E1E1E))
                    )
                },
                containerColor = Color(0xFF121212)
            ) { padding ->
                if (notes.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                        Text(
                            text = "У вас пока нет заметок.\nОни появятся здесь, когда вы добавите их в карточке тайтла.",
                            color = Color(0xFFAAAAAA), fontSize = 14.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.padding(horizontal = 32.dp)
                        )
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)) {
                        items(notes) { note ->
                            NoteHubCard(
                                note = note,
                                dateFormatter = dateFormatter,
                                onCardClick = {
                                    startActivity(Intent(this@NotesHubActivity, DetailsActivity::class.java).apply {
                                        putExtra("EXTRA_ID", note.animeId)
                                        putExtra("EXTRA_ID_MAL", note.idMal)
                                        putExtra("EXTRA_TITLE", note.animeTitle)
                                        putExtra("EXTRA_POSTER", note.animePosterUrl)
                                    })
                                },
                                onEditClick = { editingNote = note },
                                onDeleteClick = {
                                    MaterialAlertDialogBuilder(this@NotesHubActivity)
                                        .setTitle("Удалить заметку?")
                                        .setMessage("Навсегда удалить эту заметку для аниме «${note.animeTitle}»?")
                                        .setPositiveButton("Удалить") { _, _ ->
                                            lifecycleScope.launch(Dispatchers.IO) {
                                                database.noteDao().deleteById(note.noteId)
                                                withContext(Dispatchers.Main) {
                                                    loadNotes()
                                                    Toast.makeText(this@NotesHubActivity, "Заметка удалена", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                        .setNegativeButton("Отмена", null).show()
                                }
                            )
                        }
                    }
                }

                // --- ТОТ САМЫЙ ДИАЛОГ РЕДАКТИРОВАНИЯ ---
                if (editingNote != null) {
                    EditNoteDialog(
                        initialText = editingNote!!.content,
                        onDismiss = { editingNote = null },
                        onSave = { newText ->
                            lifecycleScope.launch(Dispatchers.IO) {
                                // ВОТ ОНА — КОНКРЕТНАЯ СБОРКА СУЩНОСТИ ДЛЯ БАЗЫ ДАННЫХ!
                                val updatedNoteEntity = NoteEntity(
                                    id = editingNote!!.noteId, // Обязательно передаем старый ID
                                    animeId = editingNote!!.animeId,
                                    content = newText, // Наш новый исправленный текст
                                    createdAt = editingNote!!.createdAt,
                                    updatedAt = System.currentTimeMillis() // Ставим текущее время
                                )

                                // Вставляем или обновляем в базе (убедись, что метод называется именно так, обычно это insert с REPLACE)
                                database.noteDao().insert(updatedNoteEntity)

                                withContext(Dispatchers.Main) {
                                    loadNotes() // Перезагружаем список, чтобы увидеть изменения
                                    editingNote = null // Закрываем диалог
                                    Toast.makeText(this@NotesHubActivity, "Изменения сохранены", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

// ========================================================
// COMPOSABLE КАРТОЧКИ ЗАМЕТКИ (С ПОЛНОЙ ПРИВЯЗКОЙ К НИЗУ)
// ========================================================
@Composable
fun NoteHubCard(
    note: NoteWithAnime,
    dateFormatter: SimpleDateFormat,
    onCardClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onCardClick() },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        shape = RoundedCornerShape(16.dp)
    ) {
        // Мы добавляем .height(IntrinsicSize.Min), чтобы Row знал высоту картинки
        Row(
            modifier = Modifier
                .padding(12.dp)
                .height(IntrinsicSize.Min)
        ) {
            // Левая часть: Название, Текст и Управление
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight() // 👈 1. Колонка теперь занимает всю высоту
                    .padding(end = 12.dp)
            ) {
                // Название аниме
                Text(
                    text = note.animeTitle ?: "Без названия",
                    color = Color(0xFFFF9800),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                // Текст заметки
                Text(
                    text = note.content,
                    color = Color.White,
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )

                // 👈 2. МАГИЯ ЗДЕСЬ: Умный разделитель выталкивает всё вниз
                Spacer(modifier = Modifier.weight(1f))

                // Нижний бар (Дата + Кнопки) - ТЕПЕРЬ ВСЕГДА ВНИЗУ
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp), // Небольшой отступ от текста, если он длинный
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val isEdited = note.updatedAt > 0 && note.updatedAt != note.createdAt
                    val displayTime = if (isEdited) note.updatedAt else note.createdAt
                    val formattedDate = if (displayTime > 0) dateFormatter.format(Date(displayTime)) else "—"

                    Text(
                        text = if (isEdited) "изм. $formattedDate" else formattedDate,
                        color = if (isEdited) Color(0xFFFF9800) else Color(0xFF757575),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(onClick = onEditClick, modifier = Modifier.size(32.dp)) {
                            Icon(painterResource(R.drawable.ic_pencil_square_sol), "Редактировать", tint = Color(0xFFFF9800), modifier = Modifier.size(18.dp))
                        }
                        IconButton(onClick = onDeleteClick, modifier = Modifier.size(32.dp)) {
                            Icon(painterResource(R.drawable.ic_trash_can_reg), "Удалить", tint = Color(0xFFEF5350), modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            // Правая часть: Обложка тайтла
            AsyncImage(
                model = note.animePosterUrl,
                contentDescription = "Постер",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(70.dp)
                    .height(100.dp) // Фиксированная высота постера определяет высоту всей Row
                    .clip(RoundedCornerShape(12.dp))
            )
        }
    }
}

// ========================================================
// ДИАЛОГ РЕДАКТИРОВАНИЯ
// ========================================================
@Composable
fun EditNoteDialog(
    initialText: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var text by remember { mutableStateOf(initialText) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E1E1E),
        title = { Text("Редактирование", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(color = Color.White, fontSize = 15.sp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFFF9800),
                    unfocusedBorderColor = Color(0x339CA3AF),
                    cursorColor = Color(0xFFFF9800)
                ),
                shape = RoundedCornerShape(12.dp),
                maxLines = 6
            )
        },
        confirmButton = {
            TextButton(onClick = { if (text.isNotBlank()) onSave(text.trim()) }) {
                Text("Сохранить", color = Color(0xFFFF9800), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена", color = Color(0xFFAAAAAA))
            }
        }
    )
}