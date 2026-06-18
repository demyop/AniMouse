package com.example.animouse.ui.compose

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.animouse.data.database.NoteWithAnime // Убедись, что моделька доступна
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.res.painterResource
import com.example.animouse.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesHubScreen(
    notes: List<NoteWithAnime>,
    onNoteClick: (NoteWithAnime) -> Unit,
    onNoteLongClick: (NoteWithAnime) -> Unit,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Мои заметки", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        // ✅ Добавили реальную иконку вместо комментария!
                        Icon(
                            painter = painterResource(id = R.drawable.ic_arrow_reg),
                            contentDescription = "Назад",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF121212))
            )
        },
        containerColor = Color(0xFF121212)
    ) { padding ->
        if (notes.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("У вас пока нет заметок.\nОни появятся здесь, когда вы добавите их в карточке тайтла.",
                    color = Color(0xFFAAAAAA), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(8.dp)) {
                items(notes) { note ->
                    NoteItem(note, onNoteClick, onNoteLongClick)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class) // ✅ 2. Даем официальное разрешение на использование combinedClickable
@Composable
fun NoteItem(
    note: NoteWithAnime,
    onClick: (NoteWithAnime) -> Unit,
    onLongClick: (NoteWithAnime) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .height(130.dp)
            // ✅ 3. УДАЛИЛИ старую строку .clickable { ... }, оставили только combinedClickable
            .combinedClickable(
                onClick = { onClick(note) },
                onLongClick = { onLongClick(note) }
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
    ) {
        Row(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text(note.content, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.weight(1f))
                Text("Изм.: ${note.updatedAt}", color = Color(0xFFFF9800), fontSize = 10.sp)
                Text("Создана: ${note.createdAt}", color = Color(0xFFAAAAAA), fontSize = 10.sp)
            }
            // Постер
            AsyncImage(
                model = note.animePosterUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.width(110.dp).fillMaxHeight().clip(RoundedCornerShape(14.dp))
            )
        }
    }
}