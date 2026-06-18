package com.example.animouse.ui.compose

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.animouse.R
import com.example.animouse.data.database.NoteWithAnime
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesHubScreen(
    notesWithAnime: List<NoteWithAnime>,
    onBackClick: () -> Unit,
    onSaveNote: (NoteWithAnime, String) -> Unit,
    onDeleteNote: (NoteWithAnime) -> Unit,
    onAnimeClick: (Int) -> Unit
) {
    val dateFormatter = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) }

    // Стейт для диалога редактирования
    var editingNote by remember { mutableStateOf<NoteWithAnime?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Дневник заметок", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(painterResource(R.drawable.ic_arrow_reg), "Назад", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1E1E1E))
            )
        },
        containerColor = Color(0xFF121212)
    ) { paddingValues ->
        if (notesWithAnime.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text("У вас пока нет ни одной заметки", color = Color(0xFF757575), fontSize = 16.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(notesWithAnime) { item ->
                    NoteHubCard(
                        noteWithAnime = item,
                        dateFormatter = dateFormatter,
                        onEditClick = { editingNote = item },
                        onDeleteClick = { onDeleteNote(item) },
                        onCardClick = { onAnimeClick(item.animeId) }
                    )
                }
            }
        }
    }

    // ДИАЛОГ РЕДАКТИРОВАНИЯ (Поверх списка)
    if (editingNote != null) {
        EditNoteDialog(
            initialText = editingNote!!.content,
            onDismiss = { editingNote = null },
            onSave = { newText ->
                onSaveNote(editingNote!!, newText)
                editingNote = null
            }
        )
    }
}

// --- КАРТОЧКА ЗАМЕТКИ С ОБЛОЖКОЙ ---
@Composable
fun NoteHubCard(
    noteWithAnime: NoteWithAnime,
    dateFormatter: SimpleDateFormat,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onCardClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onCardClick() },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp)) {
            // Левая часть: Название, Текст и Управление
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                // Название аниме (Оранжевый акцент)
                Text(
                    text = noteWithAnime.animeTitle,
                    color = Color(0xFFFF9800),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                // Текст заметки
                Text(
                    text = noteWithAnime.content,
                    color = Color.White,
                    fontSize = 15.sp,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Даты и кнопки
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val isEdited = noteWithAnime.updatedAt > 0 && noteWithAnime.updatedAt != noteWithAnime.createdAt
                    val displayTime = if (isEdited) noteWithAnime.updatedAt else noteWithAnime.createdAt
                    val formattedDate = if (displayTime > 0) dateFormatter.format(Date(displayTime)) else "—"
                    val dateText = if (isEdited) "изм. $formattedDate" else formattedDate

                    Text(
                        text = dateText,
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
                model = noteWithAnime.animePosterUrl,
                contentDescription = "Постер",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(70.dp)
                    .height(100.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
        }
    }
}

// --- ЧИСТЫЙ ДИАЛОГ РЕДАКТИРОВАНИЯ ---
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