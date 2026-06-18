package com.example.animouse.ui.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import coil.compose.AsyncImage
import com.example.animouse.R
import com.example.animouse.data.database.AppDatabase
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NotesHubActivity : AppCompatActivity() {

    private lateinit var database: AppDatabase

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Подключаем базу (в будущем перенесем это в Hilt, но пока оставим как есть, чтобы не сломать)
        database = androidx.room.Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "animouse_db"
        ).build()

        setContent {
            // Состояние списка заметок (Compose сам перерисует экран, когда список изменится)
            var notes by remember { mutableStateOf(emptyList<com.example.animouse.data.database.NoteWithAnime>()) }

            // Функция загрузки из базы
            fun loadNotes() {
                lifecycleScope.launch(Dispatchers.IO) {
                    val notesList = database.noteDao().getAllNotesWithAnime()
                    withContext(Dispatchers.Main) {
                        notes = notesList
                    }
                }
            }

            // Загружаем заметки при первом запуске экрана
            LaunchedEffect(Unit) {
                loadNotes()
            }

            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Мои заметки", color = Color.White, fontWeight = FontWeight.Bold) },
                        navigationIcon = {
                            IconButton(onClick = { finish() }) {
                                Icon(painterResource(R.drawable.ic_arrow_reg), contentDescription = "Назад", tint = Color.White)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1E1E1E))
                    )
                },
                containerColor = Color(0xFF121212)
            ) { padding ->
                if (notes.isEmpty()) {
                    // Пустой экран
                    Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                        Text(
                            text = "У вас пока нет заметок.\nОни появятся здесь, когда вы добавите их в карточке тайтла.",
                            color = Color(0xFFAAAAAA),
                            fontSize = 14.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                    }
                } else {
                    // Список заметок
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(padding),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
                    ) {
                        items(notes) { note ->
                            NoteHubItem(
                                note = note,
                                onClick = {
                                    val intent = Intent(this@NotesHubActivity, DetailsActivity::class.java).apply {
                                        putExtra("EXTRA_ID", note.animeId)
                                        putExtra("EXTRA_ID_MAL", note.idMal)
                                        putExtra("EXTRA_TITLE", note.animeTitle)
                                        putExtra("EXTRA_POSTER", note.animePosterUrl)
                                        putExtra("EXTRA_SCORE", 0)
                                        putExtra("EXTRA_EPISODES_TOTAL", 0)
                                    }
                                    startActivity(intent)
                                },
                                onLongClick = {
                                    MaterialAlertDialogBuilder(this@NotesHubActivity)
                                        .setTitle("Удалить заметку?")
                                        .setMessage("Вы уверены, что хотите навсегда удалить эту заметку для аниме «${note.animeTitle}»?")
                                        .setPositiveButton("Удалить") { _, _ ->
                                            lifecycleScope.launch(Dispatchers.IO) {
                                                database.noteDao().deleteById(note.noteId)
                                                withContext(Dispatchers.Main) {
                                                    loadNotes() // Перезагружаем список
                                                    Toast.makeText(this@NotesHubActivity, "Заметка удалена", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                        .setNegativeButton("Отмена", null)
                                        .show()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ========================================================
// COMPOSABLE ДЛЯ ОДНОЙ ЗАМЕТКИ (Заменяет item_hub_note.xml)
// ========================================================
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteHubItem(
    note: com.example.animouse.data.database.NoteWithAnime,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .height(130.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                .padding(12.dp)
        ) {
            // Левая часть: текст заметки и даты
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(end = 16.dp)
            ) {
                Text(
                    text = note.content,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.weight(1f))

                // ВАЖНО: Если у тебя в модели NoteWithAnime есть даты, раскомментируй эти строки!
                // Text(text = "Изм.: ${note.updatedAt}", color = Color(0xFFFF9800), fontSize = 10.sp)
                // Text(text = "Создана: ${note.createdAt}", color = Color(0xFFAAAAAA), fontSize = 10.sp, modifier = Modifier.padding(top = 2.dp))
            }

            // Правая часть: постер и название аниме
            Box(
                modifier = Modifier
                    .width(110.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(14.dp))
            ) {
                AsyncImage(
                    model = note.animePosterUrl,
                    contentDescription = "Постер",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Градиент поверх картинки (заменяет gradient_bg.xml)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(65.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))
                            )
                        )
                )

                // Название аниме поверх градиента
                Text(
                    text = note.animeTitle ?: "Без названия",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 8.dp, vertical = 8.dp)
                )
            }
        }
    }
}