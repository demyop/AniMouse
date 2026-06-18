package com.example.animouse.ui.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.animouse.R
import com.example.animouse.data.NotificationHelper
import com.example.animouse.data.database.UserAnimeEntity
import com.example.animouse.data.model.Anime
import com.example.animouse.ui.compose.AnimeCard
import com.example.animouse.ui.compose.CustomListComposeDialog
import com.example.animouse.ui.viewmodel.MainViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MainScreen() }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshFavorites()
    }

    @Composable
    fun MainScreen() {
        val allAnime by viewModel.allAnime.observeAsState(emptyList())
        val localAnime by viewModel.localAnime.observeAsState(emptyList())
        val statuses by viewModel.animeStatuses.observeAsState(emptyMap())
        val customBadges by viewModel.animeCustomBadges.observeAsState(emptyMap())
        val customPreviews by viewModel.customFolderPreviews.observeAsState(emptyList())
        val customFolderAnime by viewModel.customFolderAnime.observeAsState(emptyMap())

        var currentTab by remember { mutableIntStateOf(0) }
        var isGridView by remember { mutableStateOf(false) }
        var isFavoritesOnly by remember { mutableStateOf(false) }
        var sortMethod by remember { mutableIntStateOf(0) }
        var openedFolderId by remember { mutableStateOf<String?>(null) }
        var openedFolderTitle by remember { mutableStateOf("") }
        var isCreateDialogVisible by remember { mutableStateOf(false) }
        var editListId by remember { mutableStateOf<Int?>(null) }
        var editListName by remember { mutableStateOf("") }
        var editListColor by remember { mutableStateOf("#FFFF9800") }


        BackHandler(enabled = openedFolderId != null) { openedFolderId = null }

        // Избавляемся от Scaffold, используем Box, чтобы навигация плавала поверх контента
        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF121212))) {

            // Основной контент
            Column(modifier = Modifier.fillMaxSize()) {
                // --- ПРОЗРАЧНЫЙ ТУЛБАР ---
                Row(
                    modifier = Modifier.fillMaxWidth().background(Color.Transparent).padding(horizontal = 16.dp, vertical = 12.dp).statusBarsPadding(), // 👈 ДОБАВЛЯЕМ ЭТУ СТРОЧКУ (Отступ от шторки уведомлений),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (openedFolderId != null) {
                        IconButton(onClick = { openedFolderId = null }) { Icon(painterResource(R.drawable.ic_arrow_reg), "Назад", tint = Color.White) }
                        Text("Списки | $openedFolderTitle", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    } else {
                        val title = when (currentTab) {
                            0 -> if (isGridView) "AniMouse | Онгоинги" else "AniMouse | Календарь"
                            else -> "AniMouse | Списки аниме"
                        }
                        Text(title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))

                        if (currentTab == 1) {
                            IconButton(onClick = { startActivity(Intent(this@MainActivity, NotesHubActivity::class.java)) }) {
                                Icon(painterResource(R.drawable.ic_pencil_square_sol), "Заметки", tint = Color(0xFFFF9800))
                            }
                        } else {
                            IconButton(onClick = { startActivity(Intent(this@MainActivity, SearchActivity::class.java)) }) {
                                Icon(painterResource(R.drawable.ic_search_reg), "Поиск", tint = Color(0xFFFF9800))
                            }
                        }
                    }
                }

// --- КОНТЕНТ ---
                if (openedFolderId != null) {
                    val folderAnime = if (openedFolderId!!.startsWith("custom_")) {
                        val listId = openedFolderId!!.removePrefix("custom_").toInt()
                        customFolderAnime[listId]?.map { mapEntityToAnime(it) } ?: emptyList()
                    } else {
                        localAnime.filter { it.status == openedFolderId }.map { mapEntityToAnime(it) }
                    }

                    AnimeGrid(
                        animeList = folderAnime,
                        statuses = statuses,
                        customBadges = customBadges,
                        openedFolderId = openedFolderId, // 👈 ПЕРЕДАЕМ АКТУАЛЬНУЮ ПАПКУ
                        onAnimeClick = { navigateToDetails(it) },
                        onAnimeLongClick = { anime ->
                            // 👈 БЫСТРОЕ УДАЛЕНИЕ ИЗ СПИСКА
                            MaterialAlertDialogBuilder(this@MainActivity)
                                .setTitle("Удаление")
                                .setMessage("Точно удалить «${anime.title.romaji ?: "тайтл"}» из этого списка?")
                                .setPositiveButton("Удалить") { _, _ ->
                                    if (openedFolderId!!.startsWith("custom_")) {
                                        val listId = openedFolderId!!.removePrefix("custom_").toInt()
                                        viewModel.toggleAnimeInCustomList(anime.id, listId, false)
                                    } else {
                                        viewModel.removeAnimeFromLists(anime.id)
                                    }
                                }
                                .setNegativeButton("Отмена", null)
                                .show()
                        }
                    )
                } else {
                    when (currentTab) {
                        0 -> {
                            if (isGridView) {
                                NarrowFiltersBar(sortMethod, { sortMethod = it }, isFavoritesOnly, { isFavoritesOnly = it })
                                val processedList = allAnime.filter { if (isFavoritesOnly) statuses.containsKey(it.id) else true }.let { list ->
                                    // ... сортировка ... (оставь свою логику сортировки)
                                    when (sortMethod) {
                                        0 -> list.sortedByDescending { it.averageScore ?: 0 }
                                        1 -> list.sortedBy { it.averageScore ?: 0 }
                                        2 -> list.sortedByDescending { it.nextAiringEpisode?.episode ?: 0 }
                                        3 -> list.sortedBy { it.nextAiringEpisode?.episode ?: 0 }
                                        else -> list.sortedByDescending { it.averageScore ?: 0 }
                                    }
                                }
                                AnimeGrid(
                                    animeList = processedList,
                                    statuses = statuses,
                                    customBadges = customBadges,
                                    openedFolderId = null, // 👈 НА ГЛАВНОМ ЭКРАНЕ ПОКАЗЫВАЕМ ВСЕ ТЕГИ
                                    onAnimeClick = { navigateToDetails(it) },
                                    onAnimeLongClick = { showBottomSheetDialog(it) }
                                )
                            } else {
                                ScheduleViewWithDates(allAnime, isFavoritesOnly, customBadges, sortMethod, { sortMethod = it }, { isFavoritesOnly = it })
                            }
                        }
                        1 -> {
                            ListsView(
                                statuses = statuses,
                                localAnime = localAnime,
                                customPreviews = customPreviews,
                                customFolderAnime = customFolderAnime,
                                onOpenFolder = { id, title -> openedFolderId = id; openedFolderTitle = title },
                                // 👇 ПЕРЕДАЕМ НАШУ ЛОГИКУ СЮДА:
                                onManageListClick = { id, name, color ->
                                    editListId = id
                                    editListName = name
                                    editListColor = color
                                    isCreateDialogVisible = true // Тут MainScreen всё прекрасно видит!
                                }
                            )
                        }
                    }
                }
            }

            // --- НИЖНЕЕ МЕНЮ С ПРОЗРАЧНЫМ ГРАДИЕНТОМ ---
            CustomBottomNavigationBar(
                currentTab = currentTab,
                isGridView = isGridView,
                modifier = Modifier.align(Alignment.BottomCenter), // 👈 Оставили только выравнивание
                onTabClick = { tab -> currentTab = tab; openedFolderId = null },
                onTabLongClick = {
                    if (currentTab == 0) {
                        isGridView = !isGridView
                        Toast.makeText(this@MainActivity, if (isGridView) "Режим: Сетка" else "Режим: Календарь", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
        if (isCreateDialogVisible) {
            CustomListComposeDialog(
                listId = editListId,
                initialName = editListName,
                initialColorHex = editListColor,
                onDismiss = { isCreateDialogVisible = false },
                onSave = { name, hexColor ->
                    if (editListId != null) {
                        viewModel.updateCustomList(editListId!!, name, hexColor)
                    } else {
                        viewModel.createCustomList(name, hexColor)
                    }
                    isCreateDialogVisible = false
                }
            )
        }
    }

    // --- ФИЛЬТРЫ (Скругленные, темный свитч, оранжевый акцент) ---
    @Composable
    fun NarrowFiltersBar(sortMethod: Int, onSortMethodChange: (Int) -> Unit, isFavoritesOnly: Boolean, onFavoritesChange: (Boolean) -> Unit) {
        var isDropExpanded by remember { mutableStateOf(false) }
        val sortText = arrayOf("По популярности ↓", "По популярности ↑", "По эпизодам ↓", "По эпизодам ↑")[sortMethod]

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .background(Color(0xFF1E1E1E), RoundedCornerShape(24.dp)) // Сильное закругление
                .padding(horizontal = 16.dp, vertical = 4.dp) // Более узкий
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.weight(1f).clickable { isDropExpanded = true }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(painterResource(R.drawable.ic_filter_funnel_reg), "Сортировка", tint = Color(0xFFAAAAAA), modifier = Modifier.size(16.dp))
                    Text(sortText, color = Color.White, fontSize = 13.sp, modifier = Modifier.padding(start = 8.dp))
                }
                DropdownMenu(expanded = isDropExpanded, onDismissRequest = { isDropExpanded = false }, modifier = Modifier.background(Color(0xFF2A2A2A))) {
                    arrayOf("По популярности ↓", "По популярности ↑", "По эпизодам ↓", "По эпизодам ↑").forEachIndexed { index, option ->
                        DropdownMenuItem(text = { Text(option, color = Color.White, fontSize = 13.sp) }, onClick = { onSortMethodChange(index); isDropExpanded = false })
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Избранное", color = Color(0xFFAAAAAA), fontSize = 12.sp, modifier = Modifier.padding(end = 6.dp))
                    Switch(
                        checked = isFavoritesOnly,
                        onCheckedChange = onFavoritesChange,
                        modifier = Modifier.scale(0.7f),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFFFF9800), // Оранжевый активный
                            uncheckedThumbColor = Color(0xFFAAAAAA),
                            uncheckedTrackColor = Color(0xFF2A2A2A) // Темный неактивный
                        )
                    )
                }
            }
        }
    }

    // --- КАЛЕНДАРЬ (Крупные числа, оранжевая рамка) ---
    @Composable
    fun ScheduleViewWithDates(animeList: List<Anime>, isFavoritesOnly: Boolean, customBadges: Map<Int, List<MainViewModel.CustomFolderPreview>>, sortMethod: Int, onSortMethodChange: (Int) -> Unit, onFavoritesChange: (Boolean) -> Unit) {
        var selectedDay by remember { mutableIntStateOf(getTodayIndex()) }
        val daysTitles = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")

        val datesNumbers = remember {
            val list = mutableListOf<String>()
            val cal = Calendar.getInstance().apply { firstDayOfWeek = Calendar.MONDAY; set(Calendar.DAY_OF_WEEK, Calendar.MONDAY) }
            for (i in 0..6) { list.add(cal.get(Calendar.DAY_OF_MONTH).toString()); cal.add(Calendar.DAY_OF_MONTH, 1) }
            list
        }

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            items(7) { index ->
                val isSelected = selectedDay == index
                val activeColor = Color(0xFFFF9800)

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Transparent) // Нет заливки!
                        .border(
                            width = if (isSelected) 2.dp else 0.dp,
                            color = if (isSelected) activeColor else Color.Transparent, // Оранжевая рамка
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable { selectedDay = index }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = datesNumbers[index],
                        color = if (isSelected) activeColor else Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp // Крупный шрифт числа
                    )
                    Text(
                        text = daysTitles[index],
                        color = if (isSelected) activeColor else Color(0xFFAAAAAA),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }

        NarrowFiltersBar(sortMethod, onSortMethodChange, isFavoritesOnly, onFavoritesChange)

        val dayAnime = animeList.filter { anime ->
            val airingAt = anime.nextAiringEpisode?.airingAt
            val isCorrectDay = airingAt != null && getDayOfWeekFromTimestamp(airingAt) == selectedDay
            val passFilter = if (isFavoritesOnly) NotificationHelper.isNotificationEnabled(this@MainActivity, anime.id) else true
            isCorrectDay && passFilter
        }.sortedBy { it.nextAiringEpisode?.airingAt }

        // Добавляем отступ снизу, чтобы карточки не прятались за меню
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 100.dp)) {
            items(dayAnime) { anime ->
                val airingAt = anime.nextAiringEpisode?.airingAt
                val formattedTime = if (airingAt != null) SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(airingAt * 1000L)) else "--:--"
                var isNotifying by remember { mutableStateOf(NotificationHelper.isNotificationEnabled(this@MainActivity, anime.id)) }

                com.example.animouse.ui.compose.AnimeListCard(
                    anime = anime,
                    customBadges = customBadges[anime.id] ?: emptyList(),
                    airingTime = formattedTime,
                    isNotificationEnabled = isNotifying,
                    onNotificationClick = {
                        val airingAtSafe = anime.nextAiringEpisode?.airingAt?.toLong() ?: 0L
                        val episode = anime.nextAiringEpisode?.episode ?: 0
                        isNotifying = NotificationHelper.toggleNotification(this@MainActivity, anime.id, anime.title.romaji ?: "Аниме", airingAtSafe, episode)
                    },
                    onClick = { navigateToDetails(anime) },
                    onLongClick = { showBottomSheetDialog(anime) }
                )
            }
        }
    }

    // --- НИЖНЕЕ МЕНЮ (Градиент, иконки ic_media) ---
// --- НИЖНЕЕ МЕНЮ (Резкий градиент + Сплошная черная подложка) ---
    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun CustomBottomNavigationBar(currentTab: Int, isGridView: Boolean, modifier: Modifier = Modifier, onTabClick: (Int) -> Unit, onTabLongClick: () -> Unit) {
        Column(
            modifier = modifier.fillMaxWidth()
        ) {
            // 1. Короткий и резкий градиент над меню (высота 24dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color(0xFF121212))
                        )
                    )
            )

            // 2. Сплошная черная подложка
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF121212)) // Черный фон заливает всё до самого низа
                    .navigationBarsPadding() // 👈 ВАЖНО: Отступ для жестов теперь ВНУТРИ черного фона
                    .height(60.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                val isSelected0 = currentTab == 0
                val icon0 = when {
                    isGridView && isSelected0 -> R.drawable.ic_media_sol
                    isGridView && !isSelected0 -> R.drawable.ic_media_reg
                    !isGridView && isSelected0 -> R.drawable.ic_calendar_sol
                    else -> R.drawable.ic_calendar_reg
                }

                Column(
                    modifier = Modifier.weight(1f).fillMaxHeight()
                        .combinedClickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onTabClick(0) },
                            onLongClick = onTabLongClick
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
                ) {
                    Icon(painter = painterResource(icon0), contentDescription = null, tint = if (isSelected0) Color(0xFFFF9800) else Color(0xFFAAAAAA), modifier = Modifier.size(24.dp))
                    Text(text = if (isGridView) "Онгоинги" else "Расписание", color = if (isSelected0) Color(0xFFFF9800) else Color(0xFFAAAAAA), fontSize = 11.sp, fontWeight = if (isSelected0) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.padding(top = 2.dp))
                }

                val isSelected1 = currentTab == 1
                Column(
                    modifier = Modifier.weight(1f).fillMaxHeight().clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onTabClick(1) },
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
                ) {
                    Icon(painter = painterResource(if (isSelected1) R.drawable.ic_bookmark_sol else R.drawable.ic_bookmark_reg), contentDescription = null, tint = if (isSelected1) Color(0xFFFF9800) else Color(0xFFAAAAAA), modifier = Modifier.size(24.dp))
                    Text("Списки", color = if (isSelected1) Color(0xFFFF9800) else Color(0xFFAAAAAA), fontSize = 11.sp, fontWeight = if (isSelected1) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.padding(top = 2.dp))
                }
            }
        }
    }

    // --- СЕТКА ОНГОИНГОВ / ПАПОК ---
    @Composable
    fun AnimeGrid(
        animeList: List<Anime>,
        statuses: Map<Int, String>,
        customBadges: Map<Int, List<MainViewModel.CustomFolderPreview>>,
        openedFolderId: String?, // 👈 ДОБАВИЛИ ПАРАМЕТР: Сетка теперь знает, где мы находимся
        onAnimeClick: (Anime) -> Unit,
        onAnimeLongClick: (Anime) -> Unit
    ) {
        LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 100.dp, start = 8.dp, end = 8.dp, top = 8.dp)) {
            items(animeList) { anime ->
                val currentStatus = statuses[anime.id]

                // 1. УМНАЯ ЛОГИКА СИСТЕМНЫХ СТАТУСОВ:
                // Если мы открыли системную папку (например, "WATCHING") и статус совпадает — скрываем плашку!
                val (statusText, statusColor) = if (openedFolderId == currentStatus) {
                    null to Color.Transparent
                } else {
                    when (currentStatus) {
                        "WATCHING" -> "Смотрю" to Color(0xFF00BFA5)
                        "PLANNED" -> "В планах" to Color(0xFFFF9800)
                        "COMPLETED" -> "Просмотрено" to Color(0xFF4CAF50)
                        "DROPPED" -> "Брошено" to Color(0xFF757575)
                        else -> null to Color.Transparent
                    }
                }

                // 2. УМНАЯ ЛОГИКА КАСТОМНЫХ СПИСКОВ:
                // Если мы в кастомной папке, фильтруем её бейджик из списка
                val rawBadges = customBadges[anime.id] ?: emptyList()
                val filteredBadges = if (openedFolderId?.startsWith("custom_") == true) {
                    val openId = openedFolderId.removePrefix("custom_").toIntOrNull()
                    rawBadges.filter { it.id != openId } // Убираем тег текущей папки
                } else {
                    rawBadges
                }

                AnimeCard(
                    anime = anime,
                    systemStatusText = statusText,
                    systemStatusColor = statusColor,
                    customBadges = filteredBadges, // 👈 Передаем отфильтрованный список!
                    onClick = { onAnimeClick(anime) },
                    onLongClick = { onAnimeLongClick(anime) }
                )
            }
        }
    }

// --- СПИСКИ ---
// --- ОБНОВЛЕННЫЕ СПИСКИ ---
// --- СПИСКИ (ОБНОВЛЕННАЯ ШАПКА ФУНКЦИИ) ---
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ListsView(
    statuses: Map<Int, String>,
    localAnime: List<UserAnimeEntity>,
    customPreviews: List<MainViewModel.CustomFolderPreview>,
    customFolderAnime: Map<Int, List<UserAnimeEntity>>,
    onOpenFolder: (String, String) -> Unit,
    // 👇 ДОБАВЛЯЕМ ЭТОТ КОЛБЭК: он принимает id, имя и цвет для открытия диалога
    onManageListClick: (Int?, String, String) -> Unit
) {
    val wCount = statuses.values.count { it == "WATCHING" }
    val pCount = statuses.values.count { it == "PLANNED" }
    val cCount = statuses.values.count { it == "COMPLETED" }
    val dCount = statuses.values.count { it == "DROPPED" }
    val total = wCount + pCount + cCount + dCount

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 100.dp)) {
            item {
                Card(modifier = Modifier.fillMaxWidth().padding(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)), shape = RoundedCornerShape(20.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Всего в списках: $total", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 12.dp))
                        if (total > 0) {
                            Row(modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))) {
                                if (wCount > 0) Box(modifier = Modifier.weight(wCount.toFloat()).fillMaxHeight().background(Color(0xFF00BFA5)))
                                if (pCount > 0) Box(modifier = Modifier.weight(pCount.toFloat()).fillMaxHeight().background(Color(0xFFFF9800)))
                                if (cCount > 0) Box(modifier = Modifier.weight(cCount.toFloat()).fillMaxHeight().background(Color(0xFF4CAF50)))
                                if (dCount > 0) Box(modifier = Modifier.weight(dCount.toFloat()).fillMaxHeight().background(Color(0xFF4B5563)))
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                            Column(modifier = Modifier.weight(1f)) {
                                LegendItem(Color(0xFF00BFA5), "Смотрю: $wCount")
                                LegendItem(Color(0xFF4CAF50), "Просмотрено: $cCount")
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                LegendItem(Color(0xFFFF9800), "В планах: $pCount")
                                LegendItem(Color(0xFF4B5563), "Брошено: $dCount")
                            }
                        }
                        // ✂️ УДАЛИЛИ ОТСЮДА СТАРУЮ КНОПКУ ТЕКСТА СОЗДАНИЯ
                    }
                }
            }

            // Системные папки
            val wAnime = localAnime.filter { statuses[it.animeId] == "WATCHING" }
            item { FolderRow("WATCHING", "Смотрю", wCount, wAnime.mapNotNull { it.posterUrl }.take(3), Color(0xFF00BFA5), onOpenFolder) { shareList("Смотрю", wAnime) } }

            val pAnime = localAnime.filter { statuses[it.animeId] == "PLANNED" }
            item { FolderRow("PLANNED", "В планах", pCount, pAnime.mapNotNull { it.posterUrl }.take(3), Color(0xFFFF9800), onOpenFolder) { shareList("В планах", pAnime) } }

            val cAnime = localAnime.filter { statuses[it.animeId] == "COMPLETED" }
            item { FolderRow("COMPLETED", "Просмотрено", cCount, cAnime.mapNotNull { it.posterUrl }.take(3), Color(0xFF4CAF50), onOpenFolder) { shareList("Просмотрено", cAnime) } }

            val dAnime = localAnime.filter { statuses[it.animeId] == "DROPPED" }
            item { FolderRow("DROPPED", "Брошено", dCount, dAnime.mapNotNull { it.posterUrl }.take(3), Color(0xFF4B5563), onOpenFolder) { shareList("Брошено", dAnime) } }

            // Шаг 1.1: Перенесли заголовок и добавили кнопку "+ Создать" в один ряд
// Заголовок и кнопка "+ Создать"
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Пользовательские списки", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(
                    text = "+ Создать",
                    color = Color(0xFFFF9800),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.clickable {
                        // 👇 Вместо прямых переменных вызываем колбэк!
                        onManageListClick(null, "", "#FFFF9800")
                    }
                )
            }
        }

            // Кастомные папки
            if (customPreviews.isNotEmpty()) {
                items(customPreviews) { preview ->
                    val listAnime = customFolderAnime[preview.id] ?: emptyList()
                    FolderRow(
                        id = "custom_${preview.id}",
                        title = preview.name,
                        count = preview.count,
                        posters = listAnime.mapNotNull { it.posterUrl }.take(3),
                        color = Color(android.graphics.Color.parseColor(preview.colorHex)),
                        onOpenFolder = onOpenFolder,
                        onActionClick = {
                            MaterialAlertDialogBuilder(this@MainActivity)
                                .setTitle("Управление")
                                .setMessage("Что сделать со списком «${preview.name}»?")
                                .setPositiveButton("Поделиться") { _, _ -> shareList(preview.name, listAnime) }
                                .setNeutralButton("Редактировать") { _, _ ->
                                    // 👇 Вместо прямых переменных вызываем колбэк!
                                    onManageListClick(preview.id, preview.name, preview.colorHex)
                                }
                                .setNegativeButton("Удaлить") { _, _ -> viewModel.deleteCustomList(preview.id) }
                                .show()
                        }
                    )
                }
            }
        }
    }

    @Composable
    fun LegendItem(color: Color, text: String) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
            Box(modifier = Modifier.size(10.dp).background(color, RoundedCornerShape(5.dp)))
            Text(text, color = Color(0xFFAAAAAA), fontSize = 12.sp, modifier = Modifier.padding(start = 6.dp))
        }
    }

    // --- КАРТОЧКА ПАПКИ (Тонкая рамка, полоса слева, стопка обложек) ---
    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun FolderRow(
        id: String, title: String, count: Int, posters: List<String>, color: Color,
        onOpenFolder: (String, String) -> Unit, onActionClick: () -> Unit
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .combinedClickable(
                    onClick = { onOpenFolder(id, title) },
                    onLongClick = onActionClick
                ),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
            border = BorderStroke(1.dp, Color(0x339CA3AF)) // Та самая тонкая рамка
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().height(80.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Полоска цвета слева
                Box(modifier = Modifier.width(4.dp).fillMaxHeight().background(color))

                // 2. Текстовая часть
                Column(modifier = Modifier.weight(1f).padding(start = 16.dp)) {
                    Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                        // Иконка глазика + текст
                        Icon(painterResource(R.drawable.ic_eye_sol), contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
                        Text("Всего в списке: $count", color = Color(0xFFAAAAAA), fontSize = 13.sp, modifier = Modifier.padding(start = 6.dp))
                    }
                }

                // 3. Стопка обложек справа
                Box(
                    modifier = Modifier.fillMaxHeight().width(110.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    if (posters.isNotEmpty()) {
                        // Рисуем в обратном порядке, чтобы первая обложка была сверху
                        posters.reversed().forEachIndexed { index, url ->
                            val paddingEnd = (index * 22).dp
                            AsyncImage(
                                model = url,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .padding(end = paddingEnd + 12.dp) // Сдвигаем влево
                                    .width(46.dp)
                                    .fillMaxHeight(0.8f) // Чуть меньше высоты карточки
                                    .clip(RoundedCornerShape(8.dp))
                                    // Толстая темная обводка поверх картинки, чтобы визуально "вырезать" её из стопки
                                    .border(2.dp, Color(0xFF1E1E1E), RoundedCornerShape(8.dp))
                            )
                        }
                    } else {
                        // Заглушка, если список пуст
                        Box(
                            modifier = Modifier.padding(end = 12.dp).width(46.dp).fillMaxHeight(0.8f)
                                .background(Color(0xFF2A2A2A), RoundedCornerShape(8.dp))
                                .border(2.dp, Color(0xFF1E1E1E), RoundedCornerShape(8.dp))
                        )
                    }
                }
            }
        }
    }

    private fun navigateToDetails(anime: Anime) {
        startActivity(Intent(this, DetailsActivity::class.java).apply {
            putExtra("EXTRA_ID", anime.id)
            putExtra("EXTRA_ID_MAL", anime.idMal ?: -1)
            putExtra("EXTRA_TITLE", anime.title.romaji)
            putExtra("EXTRA_POSTER", anime.coverImage.large)
            putExtra("EXTRA_SCORE", anime.averageScore ?: 0)
            putExtra("EXTRA_EPISODES_TOTAL", anime.episodes ?: 0)
            putStringArrayListExtra("EXTRA_GENRES", java.util.ArrayList(anime.genres ?: emptyList()))
        })
    }

    private fun getTodayIndex(): Int {
        val dayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        return when (dayOfWeek) {
            Calendar.MONDAY -> 0; Calendar.TUESDAY -> 1; Calendar.WEDNESDAY -> 2; Calendar.THURSDAY -> 3; Calendar.FRIDAY -> 4; Calendar.SATURDAY -> 5; Calendar.SUNDAY -> 6; else -> 0
        }
    }

    private fun getDayOfWeekFromTimestamp(timestampSec: Long): Int {
        val cal = Calendar.getInstance().apply { timeInMillis = timestampSec * 1000 }
        return when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 0; Calendar.TUESDAY -> 1; Calendar.WEDNESDAY -> 2; Calendar.THURSDAY -> 3; Calendar.FRIDAY -> 4; Calendar.SATURDAY -> 5; Calendar.SUNDAY -> 6; else -> 0
        }
    }

    private fun mapEntityToAnime(entity: UserAnimeEntity): Anime = Anime(
        id = entity.animeId, idMal = entity.idMal, title = com.example.animouse.data.model.Title(romaji = entity.title),
        coverImage = com.example.animouse.data.model.CoverImage(large = entity.posterUrl ?: ""), averageScore = entity.score,
        episodes = entity.episodesTotal, description = "Данные из оффлайн-списка", genres = emptyList(), status = entity.animeStatus,
        season = entity.season, seasonYear = entity.seasonYear, nextAiringEpisode = com.example.animouse.data.model.NextAiringEpisode(0, 0, entity.episodesAired + 1)
    )

    private fun showBottomSheetDialog(anime: Anime) {
        val bottomSheetDialog = BottomSheetDialog(this)
        val composeView = androidx.compose.ui.platform.ComposeView(this)

        bottomSheetDialog.setContentView(composeView)

        composeView.setContent {
            val customPreviews by viewModel.customFolderPreviews.observeAsState(emptyList())
            val customFolderAnime by viewModel.customFolderAnime.observeAsState(emptyMap())

            val customLists = customPreviews.map { preview ->
                val isAdded = customFolderAnime[preview.id]?.any { it.animeId == anime.id } == true
                com.example.animouse.ui.compose.BottomSheetListState(
                    id = preview.id, name = preview.name, colorHex = preview.colorHex, isAdded = isAdded
                )
            }

            com.example.animouse.ui.compose.StatusBottomSheetContent(
                animeTitle = anime.title.romaji ?: "Без названия",
                customLists = customLists,
                onStatusSelect = { status ->
                    viewModel.setAnimeStatus(anime.id, status)
                    bottomSheetDialog.dismiss()
                },
                onRemove = {
                    viewModel.removeAnimeFromLists(anime.id)
                    bottomSheetDialog.dismiss()
                },
                onCreateCustomList = {
                    bottomSheetDialog.dismiss()
                    showCustomListManageDialog()
                },
                onToggleCustomList = { listId, isAdding ->
                    if (!isAdding) {
                        MaterialAlertDialogBuilder(this@MainActivity)
                            .setTitle("Удалить из списка?")
                            .setMessage("Убрать тайтл из этого списка?")
                            .setPositiveButton("Удалить") { _, _ ->
                                viewModel.toggleAnimeInCustomList(anime.id, listId, false)
                                bottomSheetDialog.dismiss()
                            }
                            .setNegativeButton("Отмена", null).show()
                    } else {
                        viewModel.toggleAnimeInCustomList(anime.id, listId, true)
                        bottomSheetDialog.dismiss()
                    }
                }
            )
        }
        bottomSheetDialog.show()
    }
    private fun showCustomListManageDialog(listId: Int? = null, currentName: String? = null, currentColorHex: String? = null) { /* Оставлено без изменений */ }
    private fun shareList(listName: String, animeList: List<UserAnimeEntity>) {
        if (animeList.isEmpty()) {
            Toast.makeText(this, "Этот список пока пуст!", Toast.LENGTH_SHORT).show()
            return
        }

        val sb = java.lang.StringBuilder("Название списка: $listName\n\n")

        animeList.forEachIndexed { index, anime ->
            // Форматируем каждую строку: 1. Название - ссылка
            sb.append("${index + 1}. ${anime.title} - https://shikimori.one/animes/${anime.idMal}\n")
        }

        // Добавляем ту самую милую подпись с отступом
        sb.append("\nСписок подготовлен в приложении AniMouse ₍ᐢ•͈༝•͈ᐢ₎♡")

        // Вызываем системное меню "Поделиться"
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, sb.toString())
            type = "text/plain"
        }
        startActivity(Intent.createChooser(shareIntent, "Поделиться списком"))
    }
}