package com.example.animouse.ui.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.animouse.R
import com.example.animouse.data.NotificationHelper
import com.example.animouse.data.database.UserAnimeEntity
import com.example.animouse.data.model.Anime
import com.example.animouse.ui.compose.AnimeCard
import com.example.animouse.ui.compose.AnimeListCard
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

    private val requestPermissionLauncher = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) { isGranted ->
        Toast.makeText(this, if (isGranted) "Уведомления разрешены!" else "Без разрешения МЫш не сможет напоминать о сериях", Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            MainScreen()
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshFavorites()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun MainScreen() {
        // Подписки на данные
        val allAnime by viewModel.allAnime.observeAsState(emptyList())
        val localAnime by viewModel.localAnime.observeAsState(emptyList())
        val statuses by viewModel.animeStatuses.observeAsState(emptyMap())
        val customBadges by viewModel.animeCustomBadges.observeAsState(emptyMap())
        val customPreviews by viewModel.customFolderPreviews.observeAsState(emptyList())
        val customFolderAnime by viewModel.customFolderAnime.observeAsState(emptyMap())

        // Состояния UI
        var currentTab by remember { mutableIntStateOf(0) }
        var isFavoritesOnly by remember { mutableStateOf(false) }
        var sortMethod by remember { mutableIntStateOf(0) }
        var openedFolderId by remember { mutableStateOf<String?>(null) }
        var openedFolderTitle by remember { mutableStateOf("") }

        // Системный BackButton: закрывает папку, если она открыта
        BackHandler(enabled = openedFolderId != null) {
            openedFolderId = null
        }

        Scaffold(
            bottomBar = {
                NavigationBar(containerColor = Color(0xFF121212)) {
                    NavigationBarItem(
                        icon = { Icon(painterResource(R.drawable.ic_eye_sol), contentDescription = "Онгоинги", modifier = Modifier.size(24.dp)) },
                        label = { Text("Онгоинги") },
                        selected = currentTab == 0,
                        onClick = { currentTab = 0; openedFolderId = null },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFF00BFA5), unselectedIconColor = Color(0xFFAAAAAA), indicatorColor = Color.Transparent)
                    )
                    NavigationBarItem(
                        icon = { Icon(painterResource(R.drawable.ic_calendar_sol), contentDescription = "Расписание", modifier = Modifier.size(24.dp)) },
                        label = { Text("Расписание") },
                        selected = currentTab == 1,
                        onClick = { currentTab = 1; openedFolderId = null },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFF00BFA5), unselectedIconColor = Color(0xFFAAAAAA), indicatorColor = Color.Transparent)
                    )
                    NavigationBarItem(
                        icon = { Icon(painterResource(R.drawable.ic_bookmark_sol), contentDescription = "Списки", modifier = Modifier.size(24.dp)) },
                        label = { Text("Списки") },
                        selected = currentTab == 2,
                        onClick = { currentTab = 2 },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFF00BFA5), unselectedIconColor = Color(0xFFAAAAAA), indicatorColor = Color.Transparent)
                    )
                }
            }
        ) { paddingValues ->
            Column(modifier = Modifier.fillMaxSize().background(Color(0xFF121212)).padding(paddingValues)) {

                // --- ТУЛБАР ---
                Row(modifier = Modifier.fillMaxWidth().background(Color(0xFF1E1E1E)).padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (openedFolderId != null) {
                        IconButton(onClick = { openedFolderId = null }) { Icon(painterResource(R.drawable.ic_arrow_reg), "Назад", tint = Color.White) }
                        Text("Списки | $openedFolderTitle", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    } else {
                        val title = when (currentTab) { 0 -> "Онгоинги"; 1 -> "Календарь аниме"; else -> "Списки аниме" }
                        Text(title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f).clickable { Toast.makeText(this@MainActivity, "AniMouse v1.0", Toast.LENGTH_SHORT).show() })

                        if (currentTab == 2) {
                            IconButton(onClick = { startActivity(Intent(this@MainActivity, NotesHubActivity::class.java)) }) {
                                Icon(painterResource(R.drawable.ic_pencil_square_sol), "Заметки", tint = Color(0xFF00BFA5))
                            }
                        } else {
                            IconButton(onClick = { startActivity(Intent(this@MainActivity, SearchActivity::class.java)) }) {
                                Icon(painterResource(R.drawable.ic_search_reg), "Поиск", tint = Color(0xFF00BFA5))
                            }
                        }
                    }
                }

                // --- КОНТЕНТ ---
                if (openedFolderId != null) {
                    // Рендер открытой папки
                    val folderAnime = if (openedFolderId!!.startsWith("custom_")) {
                        val listId = openedFolderId!!.removePrefix("custom_").toInt()
                        customFolderAnime[listId]?.map { mapEntityToAnime(it) } ?: emptyList()
                    } else {
                        localAnime.filter { it.status == openedFolderId }.map { mapEntityToAnime(it) }
                    }
                    AnimeGrid(folderAnime, emptyMap(), customBadges) // Внутри папки системный статус не пишем
                } else {
                    when (currentTab) {
                        0 -> {
                            // ОНГОИНГИ
                            FiltersBar(sortMethod, { sortMethod = (sortMethod + 1) % 4 }, isFavoritesOnly, { isFavoritesOnly = it })
                            val processedList = allAnime.filter { if (isFavoritesOnly) statuses.containsKey(it.id) else true }.let { list ->
                                when (sortMethod) {
                                    0 -> list.sortedByDescending { it.averageScore ?: 0 }
                                    1 -> list.sortedBy { it.averageScore ?: 0 }
                                    2 -> list.sortedByDescending { it.nextAiringEpisode?.episode ?: 0 }
                                    3 -> list.sortedBy { it.nextAiringEpisode?.episode ?: 0 }
                                    else -> list.sortedByDescending { it.averageScore ?: 0 }
                                }
                            }
                            AnimeGrid(processedList, statuses, customBadges)
                        }
                        1 -> {
                            // РАСПИСАНИЕ
                            FiltersBar(sortMethod, { sortMethod = (sortMethod + 1) % 4 }, isFavoritesOnly, { isFavoritesOnly = it })
                            ScheduleView(allAnime, isFavoritesOnly, customBadges)
                        }
                        2 -> {
                            // СПИСКИ
                            ListsView(statuses, localAnime, customPreviews, customFolderAnime,
                                onOpenFolder = { id, title -> openedFolderId = id; openedFolderTitle = title }
                            )
                        }
                    }
                }
            }
        }
    }

    // --- КОМПОНЕНТ: ПАНЕЛЬ ФИЛЬТРОВ ---
    @Composable
    fun FiltersBar(sortMethod: Int, onSortClick: () -> Unit, isFavoritesOnly: Boolean, onFavoritesChange: (Boolean) -> Unit) {
        val sortText = arrayOf("По популярности ↓", "По популярности ↑", "По эпизодам ↓", "По эпизодам ↑")[sortMethod]
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).background(Color(0xFF2A2A2A), RoundedCornerShape(12.dp)).padding(horizontal = 16.dp, vertical = 8.dp).clickable { onSortClick() }, verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(R.drawable.ic_sort_reg), "Сортировка", tint = Color(0xFFAAAAAA), modifier = Modifier.size(20.dp))
            Text(sortText, color = Color.White, fontSize = 14.sp, modifier = Modifier.padding(start = 8.dp).weight(1f))
            Text("Только избранное", color = Color(0xFFAAAAAA), fontSize = 12.sp, modifier = Modifier.padding(end = 8.dp))
            Switch(checked = isFavoritesOnly, onCheckedChange = onFavoritesChange, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF00BFA5)))
        }
    }

    // --- КОМПОНЕНТ: СЕТКА КАРТОЧЕК (ОНГОИНГИ / ПАПКИ) ---
    @Composable
    fun AnimeGrid(animeList: List<Anime>, statuses: Map<Int, String>, customBadges: Map<Int, List<MainViewModel.CustomFolderPreview>>) {
        LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(8.dp)) {
            items(animeList) { anime ->
                val currentStatus = statuses[anime.id]
                val (statusText, statusColor) = when (currentStatus) {
                    "WATCHING" -> "Смотрю" to Color(0xFF00BFA5)
                    "PLANNED" -> "В планах" to Color(0xFFFF9800)
                    "COMPLETED" -> "Просмотрено" to Color(0xFF4CAF50)
                    "DROPPED" -> "Брошено" to Color(0xFF757575)
                    else -> null to Color.Transparent
                }
                AnimeCard(
                    anime = anime, systemStatusText = statusText, systemStatusColor = statusColor, customBadges = customBadges[anime.id] ?: emptyList(),
                    onClick = { navigateToDetails(anime) },
                    onLongClick = { showBottomSheetDialog(anime) }
                )
            }
        }
    }

    // --- КОМПОНЕНТ: РАСПИСАНИЕ ---
    @Composable
    fun ScheduleView(animeList: List<Anime>, isFavoritesOnly: Boolean, customBadges: Map<Int, List<MainViewModel.CustomFolderPreview>>) {
        var selectedDay by remember { mutableIntStateOf(getTodayIndex()) }
        val daysTitles = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")

        ScrollableTabRow(selectedTabIndex = selectedDay, containerColor = Color.Transparent, contentColor = Color(0xFF00BFA5), edgePadding = 8.dp, divider = {}) {
            daysTitles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedDay == index, onClick = { selectedDay = index },
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp).background(if (selectedDay == index) Color(0xFF00BFA5) else Color(0xFF2A2A2A), RoundedCornerShape(16.dp))
                ) {
                    Text(title, color = if (selectedDay == index) Color(0xFF121212) else Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                }
            }
        }

        val dayAnime = animeList.filter { anime ->
            val airingAt = anime.nextAiringEpisode?.airingAt
            val isCorrectDay = airingAt != null && getDayOfWeekFromTimestamp(airingAt) == selectedDay
            val passFilter = if (isFavoritesOnly) NotificationHelper.isNotificationEnabled(this, anime.id) else true
            isCorrectDay && passFilter
        }.sortedBy { it.nextAiringEpisode?.airingAt }

        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
            items(dayAnime) { anime ->
                val airingAt = anime.nextAiringEpisode?.airingAt
                val formattedTime = if (airingAt != null) SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(airingAt * 1000L)) else "--:--"
                var isNotifying by remember { mutableStateOf(NotificationHelper.isNotificationEnabled(this@MainActivity, anime.id)) }

                AnimeListCard(
                    anime = anime, customBadges = customBadges[anime.id] ?: emptyList(), airingTime = formattedTime, isNotificationEnabled = isNotifying,
                    onNotificationClick = {
                        val airingAtSafe = anime.nextAiringEpisode?.airingAt?.toLong() ?: 0L
                        val episode = anime.nextAiringEpisode?.episode ?: 0
                        isNotifying = NotificationHelper.toggleNotification(this@MainActivity, anime.id, anime.title.romaji ?: "Аниме", airingAtSafe, episode)
                        Toast.makeText(this@MainActivity, if (isNotifying) "Мыш напомнит!" else "Промолчу!", Toast.LENGTH_SHORT).show()
                    },
                    onClick = { navigateToDetails(anime) },
                    onLongClick = { showBottomSheetDialog(anime) }
                )
            }
        }
    }

    // --- КОМПОНЕНТ: СПИСКИ (Диаграмма и папки) ---
    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun ListsView(
        statuses: Map<Int, String>, localAnime: List<UserAnimeEntity>, customPreviews: List<MainViewModel.CustomFolderPreview>,
        customFolderAnime: Map<Int, List<UserAnimeEntity>>, onOpenFolder: (String, String) -> Unit
    ) {
        val wCount = statuses.values.count { it == "WATCHING" }
        val pCount = statuses.values.count { it == "PLANNED" }
        val cCount = statuses.values.count { it == "COMPLETED" }
        val dCount = statuses.values.count { it == "DROPPED" }
        val total = wCount + pCount + cCount + dCount

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                // Карточка с диаграммой
                Card(modifier = Modifier.fillMaxWidth().padding(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)), shape = RoundedCornerShape(20.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Всего в списках: $total", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 12.dp))

                        // Цветная полоска диаграммы
                        if (total > 0) {
                            Row(modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))) {
                                if (wCount > 0) Box(modifier = Modifier.weight(wCount.toFloat()).fillMaxHeight().background(Color(0xFF00BFA5)))
                                if (pCount > 0) Box(modifier = Modifier.weight(pCount.toFloat()).fillMaxHeight().background(Color(0xFFFF9800)))
                                if (cCount > 0) Box(modifier = Modifier.weight(cCount.toFloat()).fillMaxHeight().background(Color(0xFF4CAF50)))
                                if (dCount > 0) Box(modifier = Modifier.weight(dCount.toFloat()).fillMaxHeight().background(Color(0xFF4B5563)))
                            }
                        }

                        // Легенда
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

                        Text("+ Создать пользовательский список", color = Color(0xFF00BFA5), fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth().padding(top = 16.dp).clickable { showCustomListManageDialog() })
                    }
                }
            }

            // Системные папки
            item { FolderRow("WATCHING", "Смотрю", wCount, localAnime, statuses, Color(0xFF00BFA5), onOpenFolder) }
            item { FolderRow("PLANNED", "В планах", pCount, localAnime, statuses, Color(0xFFFF9800), onOpenFolder) }
            item { FolderRow("COMPLETED", "Просмотрено", cCount, localAnime, statuses, Color(0xFF4CAF50), onOpenFolder) }
            item { FolderRow("DROPPED", "Брошено", dCount, localAnime, statuses, Color(0xFF4B5563), onOpenFolder) }

            // Кастомные папки
            if (customPreviews.isNotEmpty()) {
                item { Text("Пользовательские списки", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp)) }
                items(customPreviews) { preview ->
                    FolderRow("custom_${preview.id}", preview.name, preview.count, emptyList(), emptyMap(), Color(android.graphics.Color.parseColor(preview.colorHex)), onOpenFolder, preview.randomPosterUrl, isCustom = true, listId = preview.id)
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

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun FolderRow(id: String, title: String, count: Int, localAnime: List<UserAnimeEntity>, statuses: Map<Int, String>, color: Color, onOpenFolder: (String, String) -> Unit, customPoster: String? = null, isCustom: Boolean = false, listId: Int = -1) {
        val poster = customPoster ?: localAnime.firstOrNull { statuses[it.animeId] == id }?.posterUrl
        Row(
            modifier = Modifier.fillMaxWidth().combinedClickable(
                onClick = { onOpenFolder(id, title) },
                onLongClick = {
                    val listToShare = if (isCustom) viewModel.customFolderAnime.value?.get(listId) ?: emptyList() else localAnime.filter { statuses[it.animeId] == id }
                    if (isCustom) {
                        MaterialAlertDialogBuilder(this@MainActivity)
                            .setTitle("Управление").setMessage("Что сделать со списком «$title»?")
                            .setPositiveButton("Поделиться") { _, _ -> shareList(title, listToShare) }
                            .setNeutralButton("Редактировать") { _, _ -> showCustomListManageDialog(listId, title, String.format("#%06X", 0xFFFFFF and color.value.toInt())) }
                            .setNegativeButton("Удалить") { _, _ -> viewModel.deleteCustomList(listId) }.show()
                    } else {
                        MaterialAlertDialogBuilder(this@MainActivity).setTitle("Системный список: $title").setMessage("Его нельзя удалить, но можно отправить друзьям!").setPositiveButton("Поделиться") { _, _ -> shareList(title, listToShare) }.show()
                    }
                }
            ).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFF2A2A2A))) {
                if (poster != null) AsyncImage(model = poster, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            }
            Column(modifier = Modifier.padding(start = 16.dp).weight(1f)) {
                Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                Text("$count тайтлов", color = Color(0xFFAAAAAA), fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
            }
            Box(modifier = Modifier.width(4.dp).height(40.dp).background(color, RoundedCornerShape(2.dp)))
        }
    }

    // --- НАВИГАЦИЯ И ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ---
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

    // Все методы showBottomSheetDialog, shareList, getTodayIndex, getDayOfWeekFromTimestamp,
    // mapEntityToAnime и showCustomListManageDialog остаются БЕЗ ИЗМЕНЕНИЙ!
    // Просто вставь их сюда из своего старого MainActivity.kt

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
        try {
            val bottomSheetDialog = BottomSheetDialog(this)
            val sheetView = layoutInflater.inflate(R.layout.bottom_sheet_status, null)
            bottomSheetDialog.setContentView(sheetView)

            sheetView.findViewById<android.widget.TextView>(R.id.textSheetTitle).text = anime.title.romaji
            sheetView.findViewById<View>(R.id.btnWatching).setOnClickListener { viewModel.setAnimeStatus(anime.id, "WATCHING"); bottomSheetDialog.dismiss() }
            sheetView.findViewById<View>(R.id.btnPlanned).setOnClickListener { viewModel.setAnimeStatus(anime.id, "PLANNED"); bottomSheetDialog.dismiss() }
            sheetView.findViewById<View>(R.id.btnCompleted).setOnClickListener { viewModel.setAnimeStatus(anime.id, "COMPLETED"); bottomSheetDialog.dismiss() }
            sheetView.findViewById<View>(R.id.btnDropped).setOnClickListener { viewModel.setAnimeStatus(anime.id, "DROPPED"); bottomSheetDialog.dismiss() }
            sheetView.findViewById<View>(R.id.btnRemove).setOnClickListener { viewModel.removeAnimeFromLists(anime.id); bottomSheetDialog.dismiss() }
            sheetView.findViewById<View>(R.id.btnCreateCustomList).setOnClickListener { bottomSheetDialog.dismiss(); showCustomListManageDialog() }

            val container = sheetView.findViewById<android.widget.LinearLayout>(R.id.layoutCustomListsContainer)
            container.removeAllViews()
            val customLists = viewModel.customFolderPreviews.value ?: emptyList()
            val customAnimeMap = viewModel.customFolderAnime.value ?: emptyMap()

            for (listPreview in customLists) {
                val itemView = layoutInflater.inflate(R.layout.item_custom_list_option, container, false)
                val textName = itemView.findViewById<android.widget.TextView>(R.id.textCustomListName).apply { text = listPreview.name }
                val parsedColor = android.graphics.Color.parseColor(listPreview.colorHex)
                itemView.findViewById<View>(R.id.indicatorListColor).backgroundTintList = android.content.res.ColorStateList.valueOf(parsedColor)
                val isAlreadyInList = customAnimeMap[listPreview.id]?.any { it.animeId == anime.id } == true

                val iconCheck = itemView.findViewById<android.widget.ImageView>(R.id.iconCheck)
                if (isAlreadyInList) {
                    iconCheck.visibility = View.VISIBLE
                    iconCheck.imageTintList = android.content.res.ColorStateList.valueOf(parsedColor)
                    textName.setTextColor(parsedColor)
                } else {
                    iconCheck.visibility = View.GONE
                    textName.setTextColor(getColor(R.color.text_primary))
                }

                itemView.setOnClickListener {
                    if (isAlreadyInList) {
                        MaterialAlertDialogBuilder(this).setTitle("Удалить из списка?").setMessage("Вы уверены, что хотите убрать тайтл из списка «${listPreview.name}»?")
                            .setPositiveButton("Удалить") { _, _ -> viewModel.toggleAnimeInCustomList(anime.id, listPreview.id, false); bottomSheetDialog.dismiss() }.setNegativeButton("Отмена", null).show()
                    } else {
                        viewModel.toggleAnimeInCustomList(anime.id, listPreview.id, true)
                        bottomSheetDialog.dismiss()
                    }
                }
                container.addView(itemView)
            }
            bottomSheetDialog.show()
        } catch (e: Exception) { Toast.makeText(this, "Ошибка меню: ${e.message}", Toast.LENGTH_LONG).show() }
    }

    private fun showCustomListManageDialog(listId: Int? = null, currentName: String? = null, currentColorHex: String? = null) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_create_list, null)
        val alertDialog = MaterialAlertDialogBuilder(this).setView(dialogView).setBackground(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT)).create()
        val inputName = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.inputListName)
        val viewPreview = dialogView.findViewById<View>(R.id.viewColorPreview)
        val textHex = dialogView.findViewById<android.widget.TextView>(R.id.textColorHex)
        val seekRed = dialogView.findViewById<android.widget.SeekBar>(R.id.seekRed)
        val seekGreen = dialogView.findViewById<android.widget.SeekBar>(R.id.seekGreen)
        val seekBlue = dialogView.findViewById<android.widget.SeekBar>(R.id.seekBlue)

        if (listId != null) {
            dialogView.findViewById<android.widget.TextView>(R.id.textDialogTitle).text = "Настройка списка"
            inputName.setText(currentName); textHex.text = currentColorHex
            try {
                val parsed = android.graphics.Color.parseColor(currentColorHex)
                seekRed.progress = android.graphics.Color.red(parsed); seekGreen.progress = android.graphics.Color.green(parsed); seekBlue.progress = android.graphics.Color.blue(parsed)
                viewPreview.backgroundTintList = android.content.res.ColorStateList.valueOf(parsed)
            } catch (e: Exception) { }
        }

        val rgbListener = object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                val computedColor = android.graphics.Color.rgb(seekRed.progress, seekGreen.progress, seekBlue.progress)
                viewPreview.backgroundTintList = android.content.res.ColorStateList.valueOf(computedColor)
                textHex.text = String.format("#%02X%02X%02X", seekRed.progress, seekGreen.progress, seekBlue.progress)
            }
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
        }
        seekRed.setOnSeekBarChangeListener(rgbListener); seekGreen.setOnSeekBarChangeListener(rgbListener); seekBlue.setOnSeekBarChangeListener(rgbListener)
        dialogView.findViewById<View>(R.id.btnCancelList).setOnClickListener { alertDialog.dismiss() }
        dialogView.findViewById<View>(R.id.btnSaveList).setOnClickListener {
            val name = inputName.text.toString().trim()
            if (name.isNotEmpty()) {
                if (listId != null) { viewModel.updateCustomList(listId, name, textHex.text.toString()) } else { viewModel.createCustomList(name, textHex.text.toString()) }
                alertDialog.dismiss()
            } else inputName.error = "Введите название"
        }
        alertDialog.show()
    }

    private fun shareList(listName: String, animeList: List<UserAnimeEntity>) {
        if (animeList.isEmpty()) { Toast.makeText(this, "Этот список пока пуст!", Toast.LENGTH_SHORT).show(); return }
        val sb = java.lang.StringBuilder("Название списка: $listName\n\n")
        animeList.forEachIndexed { index, anime -> sb.append("${index + 1}. ${anime.title} - https://shikimori.one/animes/${anime.idMal}\n") }
        startActivity(Intent.createChooser(Intent().apply { action = Intent.ACTION_SEND; putExtra(Intent.EXTRA_TEXT, sb.toString()); type = "text/plain" }, "Поделиться списком"))
    }
}