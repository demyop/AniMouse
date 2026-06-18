package com.example.animouse.ui.compose

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.animouse.R
import java.text.SimpleDateFormat
import java.util.*

// ==========================================
// 1. ДИАЛОГ СОЗДАНИЯ И РЕДАКТИРОВАНИЯ СПИСКА
// ==========================================
@Composable
fun CustomListComposeDialog(
    listId: Int?,
    initialName: String,
    initialColorHex: String,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    val initialColor = remember(initialColorHex) {
        try { android.graphics.Color.parseColor(initialColorHex) }
        catch (_: Exception) { android.graphics.Color.rgb(255, 152, 0) }
    }

    var red by remember { mutableStateOf(android.graphics.Color.red(initialColor).toFloat()) }
    var green by remember { mutableStateOf(android.graphics.Color.green(initialColor).toFloat()) }
    var blue by remember { mutableStateOf(android.graphics.Color.blue(initialColor).toFloat()) }

    val computedColor = Color(red.toInt(), green.toInt(), blue.toInt())
    val hexText = String.format("#%02X%02X%02X", red.toInt(), green.toInt(), blue.toInt())

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E1E1E),
        title = { Text(if (listId != null) "Настройка списка" else "Создать новый список", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Название списка", color = Color(0xFFAAAAAA)) }, maxLines = 1,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFFF9800), unfocusedBorderColor = Color(0x339CA3AF), focusedLabelColor = Color(0xFFFF9800), focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(45.dp).clip(RoundedCornerShape(12.dp)).background(computedColor))
                    Text(text = hexText, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 12.dp))
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("Красный: ${red.toInt()}", color = Color(0xFFAAAAAA), fontSize = 12.sp)
                Slider(value = red, onValueChange = { red = it }, valueRange = 0f..255f, colors = SliderDefaults.colors(thumbColor = Color(0xFFFF9800), activeTrackColor = Color(0xFFFF9800)))
                Text("Зеленый: ${green.toInt()}", color = Color(0xFFAAAAAA), fontSize = 12.sp)
                Slider(value = green, onValueChange = { green = it }, valueRange = 0f..255f, colors = SliderDefaults.colors(thumbColor = Color(0xFFFF9800), activeTrackColor = Color(0xFFFF9800)))
                Text("Синий: ${blue.toInt()}", color = Color(0xFFAAAAAA), fontSize = 12.sp)
                Slider(value = blue, onValueChange = { blue = it }, valueRange = 0f..255f, colors = SliderDefaults.colors(thumbColor = Color(0xFFFF9800), activeTrackColor = Color(0xFFFF9800)))
            }
        },
        confirmButton = { TextButton(onClick = { if (name.trim().isNotEmpty()) onSave(name.trim(), hexText) }) { Text("Сохранить", color = Color(0xFFFF9800), fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена", color = Color(0xFFAAAAAA)) } }
    )
}

// ==========================================
// 2. МЕНЮ СТАТУСОВ АНИМЕ (BOTTOM SHEET)
// ==========================================
data class BottomSheetListState(val id: Int, val name: String, val colorHex: String, val isAdded: Boolean)

@Composable
fun StatusBottomSheetContent(
    animeTitle: String,
    customLists: List<BottomSheetListState>,
    onStatusSelect: (String) -> Unit,
    onRemove: () -> Unit,
    onCreateCustomList: () -> Unit,
    onToggleCustomList: (Int, Boolean) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().background(Color(0xFF1E1E1E), RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)).padding(20.dp)) {
        Text(text = animeTitle, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))

        // Системные статусы
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatusButton(modifier = Modifier.weight(1f), text = "Смотрю", color = Color(0xFF00BFA5)) { onStatusSelect("WATCHING") }
            StatusButton(modifier = Modifier.weight(1f), text = "В планах", color = Color(0xFFFF9800)) { onStatusSelect("PLANNED") }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatusButton(modifier = Modifier.weight(1f), text = "Просмотрено", color = Color(0xFF4CAF50)) { onStatusSelect("COMPLETED") }
            StatusButton(modifier = Modifier.weight(1f), text = "Брошено", color = Color(0xFF757575)) { onStatusSelect("DROPPED") }
        }

        Spacer(modifier = Modifier.height(16.dp))
        OutlinedButton(
            onClick = onRemove, modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF5350)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x80EF5350)), shape = RoundedCornerShape(12.dp)
        ) { Text("Удалить из списков", fontWeight = FontWeight.Bold) }

        Spacer(modifier = Modifier.height(20.dp))
        HorizontalDivider(color = Color(0x339CA3AF))
        Spacer(modifier = Modifier.height(16.dp))

        // Пользовательские списки
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Пользовательские списки", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("+ Создать", color = Color(0xFFFF9800), fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.clickable { onCreateCustomList() })
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp)) {
            items(customLists) { list ->
                val listColor = Color(android.graphics.Color.parseColor(list.colorHex))
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { onToggleCustomList(list.id, !list.isAdded) }.padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(14.dp).clip(CircleShape).background(listColor))
                    Text(text = list.name, color = if (list.isAdded) listColor else Color.White, fontSize = 16.sp, fontWeight = if (list.isAdded) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.padding(start = 12.dp).weight(1f))
                    if (list.isAdded) Icon(painterResource(R.drawable.ic_check_circle_sol), contentDescription = null, tint = listColor, modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}

@Composable
fun StatusButton(modifier: Modifier = Modifier, text: String, color: Color, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = modifier.height(48.dp), colors = ButtonDefaults.buttonColors(containerColor = color), shape = RoundedCornerShape(12.dp)) {
        Text(text, color = Color(0xFF121212), fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

// ========================================================
// 3. ПОЛНОСТЬЮ ПЕРЕРАБОТАННОЕ МЕНЮ ЗАМЕТОК (БЕЗ БАГОВ КЛАВИАТУРЫ)
// ========================================================
@Composable
fun NotesBottomSheetContent(
    notes: List<com.example.animouse.data.database.NoteEntity>,
    onSendClick: (String, com.example.animouse.data.database.NoteEntity?) -> Unit,
    onDeleteClick: (com.example.animouse.data.database.NoteEntity) -> Unit
) {
    var inputText by remember { mutableStateOf("") }
    var editingNote by remember { mutableStateOf<com.example.animouse.data.database.NoteEntity?>(null) }

    // Форматтер для красивого вывода даты и времени
    val dateFormatter = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) }

    // Главный контейнер на всю доступную высоту боттом-шита, учитывающий клавиатуру через windowInsets
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, start = 20.dp, end = 20.dp, bottom = 24.dp)
            .navigationBarsPadding()
            .imePadding() // Теперь он будет работать идеально!
    ) {
        // ... остальной код заголовка и списка ...
        // Заголовок окна
        Text(
            text = "Заметки тайтла",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Основная рабочая зона: список заметок.
        // weight(1f, fill = false) заставляет список занимать ВСЁ свободное место,
        // но сжиматься, когда вылезает клавиатура!
        Box(
            modifier = Modifier
                .weight(1f, fill = false)
                .fillMaxWidth()
        ) {
            if (notes.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Здесь пока нет заметок...", color = Color(0xFF757575), fontSize = 14.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(notes) { note ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                // Текст заметки
                                Text(
                                    text = note.content,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    lineHeight = 20.sp
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // Нижний бар карточки: Даты + Кнопки управления
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    // 🕒 ВЫВОД ДАТЫ (Пункт 2 твоего репорта)
                                    val isEdited = note.updatedAt > 0 && note.updatedAt != note.createdAt
                                    val displayTime = if (isEdited) note.updatedAt else note.createdAt
                                    val formattedDate = if (displayTime > 0) dateFormatter.format(Date(displayTime)) else "—"
                                    val dateText = if (isEdited) "изм. $formattedDate" else formattedDate

                                    Text(
                                        text = dateText,
                                        color = if (isEdited) Color(0xFFFF9800) else Color(0xFF757575),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )

                                    // Кнопки управления
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        IconButton(
                                            onClick = {
                                                inputText = note.content
                                                editingNote = note
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(painterResource(R.drawable.ic_pencil_square_sol), "Редактировать", tint = Color(0xFFFF9800), modifier = Modifier.size(18.dp))
                                        }

                                        IconButton(
                                            onClick = { onDeleteClick(note) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(painterResource(R.drawable.ic_trash_can_reg), "Удалить", tint = Color(0xFFEF5350), modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // БЛОК РЕДАКТИРОВАНИЯ И ВВОДА (Пункт 1 и 3 твоего репорта)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            // Индикатор режима редактирования
            if (editingNote != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp)
                        .background(Color(0x1AFF9800), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(painterResource(R.drawable.ic_pencil_line_reg), null, tint = Color(0xFFFF9800), modifier = Modifier.size(14.dp))
                        Text("Редактирование заметки...", color = Color(0xFFFF9800), fontSize = 12.sp, modifier = Modifier.padding(start = 6.dp))
                    }
                    Icon(
                        painter = painterResource(R.drawable.ic_trash_can_reg),
                        contentDescription = "Отмена",
                        tint = Color(0xFFAAAAAA),
                        modifier = Modifier
                            .size(16.dp)
                            .clickable {
                                inputText = ""
                                editingNote = null
                            }
                    )
                }
            }

            // Ряд с полем ввода и кнопкой отправки
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Напишите что-нибудь...", color = Color(0xFF757575), fontSize = 14.sp) },

                    // 👇 Задаем цвет вводимого текста напрямую через стиль
                    textStyle = TextStyle(color = Color.White, fontSize = 15.sp),

                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFFF9800),
                        unfocusedBorderColor = Color(0x339CA3AF),
                        cursorColor = Color(0xFFFF9800) // Сделали оранжевый курсор для красоты!
                    ),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 4
                )

                Spacer(modifier = Modifier.width(12.dp))

                IconButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            onSendClick(inputText.trim(), editingNote)
                            inputText = ""
                            editingNote = null
                        }
                    },
                    modifier = Modifier
                        .background(Color(0xFFFF9800), CircleShape)
                        .size(48.dp)
                ) {
                    Icon(
                        painter = painterResource(if (editingNote != null) R.drawable.ic_check_circle_sol else R.drawable.ic_rocket_reg),
                        contentDescription = "Сохранить",
                        tint = Color(0xFF121212),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}