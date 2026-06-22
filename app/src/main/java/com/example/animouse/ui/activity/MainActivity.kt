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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
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
import androidx.compose.runtime.collectAsState
import com.example.animouse.data.model.Title
import com.example.animouse.data.model.CoverImage
import com.example.animouse.data.model.NextAiringEpisode
import com.example.animouse.ui.compose.NewsCard
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import com.example.animouse.ui.compose.NewsCard
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.Alignment

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
        val allAnime by viewModel.allAnime.collectAsState()
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

        // 👇 ДОБАВЛЯЕМ ДЛЯ СУПЕР-ЭКРАНА
        val discoveryState by viewModel.discoveryState.collectAsState()

        LaunchedEffect(Unit) {
            viewModel.loadDiscoveryScreenData() // Запускаем загрузку при старте
        }

        BackHandler(enabled = openedFolderId != null) { openedFolderId = null }

        // Избавляемся от Scaffold, используем Box, чтобы навигация плавала поверх контента
        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF121212))) {

            // Основной контент
            Column(modifier = Modifier.fillMaxSize()) {
                // --- ПРОЗРАЧНЫЙ ТУЛБАР ---
                Row(
                    modifier = Modifier.fillMaxWidth().background(Color.Transparent)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .statusBarsPadding(), // 👈 ДОБАВЛЯЕМ ЭТУ СТРОЧКУ (Отступ от шторки уведомлений),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (openedFolderId != null) {
                        IconButton(onClick = {
                            openedFolderId = null
                        }) {
                            Icon(
                                painterResource(R.drawable.ic_arrow_reg),
                                "Назад",
                                tint = Color.White
                            )
                        }
                        Text(
                            "Списки | $openedFolderTitle",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        // Меняем логику заголовков для трех табов
                        val title = when (currentTab) {
                            0 -> "AniMouse | Главная"
                            1 -> if (isGridView) "AniMouse | Онгоинги" else "AniMouse | Календарь"
                            else -> "AniMouse | Списки аниме"
                        }
                        Text(
                            title,
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )

                        // Иконки справа в зависимости от таба
                        // Иконки справа в зависимости от таба
                        when (currentTab) {
                            0 -> {
                                Row { // 👈 Твой Row для двух иконок
                                    // 1. НАША ОБНОВЛЕННАЯ РАКЕТА (РАНДОМ)
                                    IconButton(
                                        onClick = {
                                            // Бросаем кубик (запускаем ракету!)
                                            viewModel.rollRandomAnime { randomAnime ->
                                                val intent = Intent(this@MainActivity, DetailsActivity::class.java).apply {
                                                    putExtra("EXTRA_ID", randomAnime.id)
                                                    putExtra("EXTRA_ID_MAL", randomAnime.id)
                                                    putExtra("EXTRA_TITLE", randomAnime.russian ?: randomAnime.name)
                                                    putExtra("EXTRA_POSTER", "https://shikimori.one${randomAnime.image?.original}")
                                                    putExtra("EXTRA_SCORE", randomAnime.score?.toFloatOrNull() ?: 0f)
                                                    putExtra("EXTRA_EPISODES_TOTAL", randomAnime.episodes)
                                                }
                                                startActivity(intent)
                                            }
                                        },
                                        // Блокируем кнопку от двойных нажатий, пока идет загрузка
                                        enabled = !viewModel.isRandomLoading.value
                                    ) {
                                        // Магия UI: Меняем ракету на крутилку во время загрузки
                                        if (viewModel.isRandomLoading.value) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(24.dp),
                                                color = Color(0xFFFF9800),
                                                strokeWidth = 2.dp
                                            )
                                        } else {
                                            Icon(
                                                painterResource(R.drawable.ic_rocket_reg),
                                                contentDescription = "Случайное",
                                                tint = Color(0xFFFF9800)
                                            )
                                        }
                                    }

                                    // 2. ТВОЯ КНОПКА ПОИСКА (Остается без изменений)
                                    IconButton(onClick = {
                                        startActivity(Intent(this@MainActivity, SearchActivity::class.java))
                                    }) {
                                        Icon(
                                            painterResource(R.drawable.ic_search_reg),
                                            contentDescription = "Поиск",
                                            tint = Color(0xFFFF9800)
                                        )
                                    }
                                }
                            }

                            1 -> {
                                IconButton(onClick = {
                                    startActivity(
                                        Intent(
                                            this@MainActivity,
                                            SearchActivity::class.java
                                        )
                                    )
                                }) {
                                    Icon(
                                        painterResource(R.drawable.ic_search_reg),
                                        "Поиск",
                                        tint = Color(0xFFFF9800)
                                    )
                                }
                            }

                            2 -> {
                                IconButton(onClick = {
                                    startActivity(
                                        Intent(
                                            this@MainActivity,
                                            NotesHubActivity::class.java
                                        )
                                    )
                                }) {
                                    Icon(
                                        painterResource(R.drawable.ic_pencil_square_sol),
                                        "Заметки",
                                        tint = Color(0xFFFF9800)
                                    )
                                }
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
                        localAnime.filter { it.status == openedFolderId }
                            .map { mapEntityToAnime(it) }
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
                                        val listId =
                                            openedFolderId!!.removePrefix("custom_").toInt()
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
                            // НОВЫЙ СУПЕР-ЭКРАН
                            DiscoveryScreen(
                                state = discoveryState,
                                statuses = statuses,
                                customBadges = customBadges,
                                onAnimeClick = { navigateToDetails(it) },
                                // 👇 ТЕПЕРЬ ВКЛЮЧАЕМ ИМЕННО СЕТКУ ПРИ ПЕРЕХОДЕ
                                onNavigateToOngoing = {
                                    isGridView = true // 🎯 Железно включаем сетку
                                    currentTab = 1    // Переключаем таб
                                },
                                onNavigateToCollection = { collectionType, title ->
                                    // 👇 ТЕПЕРЬ ОТКРЫВАЕТСЯ НАШ НОВЫЙ ЭКРАН С СЕТКОЙ
                                    val intent = Intent(this@MainActivity, AnimeCollectionActivity::class.java).apply {
                                        putExtra("EXTRA_COLLECTION_TYPE", collectionType)
                                        putExtra("EXTRA_COLLECTION_TITLE", title)
                                    }
                                    startActivity(intent)
                                }
                            )
                        }

                        1 -> {
                            // 🪄 МАГИЧЕСКИЙ ПЕРЕХОДНИК:
                            // Превращаем новые чистые Entity из БД в старые классы Anime для UI
                            val mappedAllAnime = allAnime.map { entity ->
                                Anime(
                                    id = entity.idMal,
                                    idMal = entity.idMal,
                                    title = Title(
                                        romaji = entity.titleRussian ?: entity.titleRomaji
                                        ?: "Без названия"
                                    ),
                                    coverImage = CoverImage(large = entity.posterUrl ?: ""),
                                    averageScore = entity.score,
                                    episodes = entity.episodes,
                                    status = "RELEASING",
                                    season = null,
                                    seasonYear = null,
                                    description = null,
                                    genres = emptyList(),
                                    // ВОСКРЕШАЕМ КАЛЕНДАРЬ! (Теперь по-настоящему)
                                    nextAiringEpisode = if (entity.nextAiringAt != null && entity.nextEpisode != null) {
                                        NextAiringEpisode(
                                            airingAt = entity.nextAiringAt!!,
                                            timeUntilAiring = 0,
                                            episode = entity.nextEpisode!!
                                        )
                                    } else null
                                )
                            }

                            if (isGridView) {
                                NarrowFiltersBar(
                                    sortMethod,
                                    { sortMethod = it },
                                    isFavoritesOnly,
                                    { isFavoritesOnly = it })
                                // 👇 Заменили allAnime на mappedAllAnime
                                val processedList = mappedAllAnime.filter {
                                    if (isFavoritesOnly) statuses.containsKey(it.id) else true
                                }.let { list ->
                                    when (sortMethod) {
                                        0 -> list.sortedByDescending { it.averageScore ?: 0 }
                                        1 -> list.sortedBy { it.averageScore ?: 0 }
                                        2 -> list.sortedByDescending {
                                            it.nextAiringEpisode?.episode ?: 0
                                        }

                                        3 -> list.sortedBy { it.nextAiringEpisode?.episode ?: 0 }
                                        else -> list.sortedByDescending { it.averageScore ?: 0 }
                                    }
                                }
                                AnimeGrid(
                                    animeList = processedList,
                                    statuses = statuses,
                                    customBadges = customBadges,
                                    openedFolderId = null,
                                    onAnimeClick = { navigateToDetails(it) },
                                    onAnimeLongClick = { showBottomSheetDialog(it) } // 👈 Обрати внимание, чтобы тут не было ошибок!
                                )
                            } else {
                                // И здесь тоже используем mappedAllAnime
                                ScheduleViewWithDates(
                                    mappedAllAnime,
                                    isFavoritesOnly,
                                    customBadges,
                                    sortMethod,
                                    { sortMethod = it },
                                    { isFavoritesOnly = it })
                            }
                        }

                        2 -> {
                            ListsView(
                                statuses = statuses,
                                localAnime = localAnime,
                                customPreviews = customPreviews,
                                customFolderAnime = customFolderAnime,
                                onOpenFolder = { id, title ->
                                    openedFolderId = id; openedFolderTitle = title
                                },
                                // 👇 ПЕРЕДАЕМ НАШУ ЛОГИКУ СЮДА:
                                onManageListClick = { id, name, color ->
                                    editListId = id
                                    editListName = name
                                    editListColor = color
                                    isCreateDialogVisible =
                                        true // Тут MainScreen всё прекрасно видит!
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
                    if (currentTab == 1) {
                        isGridView = !isGridView
                        Toast.makeText(
                            this@MainActivity,
                            if (isGridView) "Режим: Сетка" else "Режим: Календарь",
                            Toast.LENGTH_SHORT
                        ).show()
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
    fun NarrowFiltersBar(
        sortMethod: Int,
        onSortMethodChange: (Int) -> Unit,
        isFavoritesOnly: Boolean,
        onFavoritesChange: (Boolean) -> Unit
    ) {
        var isDropExpanded by remember { mutableStateOf(false) }
        val sortText = arrayOf(
            "По популярности ↓",
            "По популярности ↑",
            "По эпизодам ↓",
            "По эпизодам ↑"
        )[sortMethod]

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .background(Color(0xFF1E1E1E), RoundedCornerShape(24.dp)) // Сильное закругление
                .padding(horizontal = 16.dp, vertical = 4.dp) // Более узкий
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.weight(1f).clickable { isDropExpanded = true }
                    .padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painterResource(R.drawable.ic_filter_funnel_reg),
                        "Сортировка",
                        tint = Color(0xFFAAAAAA),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        sortText,
                        color = Color.White,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                DropdownMenu(
                    expanded = isDropExpanded,
                    onDismissRequest = { isDropExpanded = false },
                    modifier = Modifier.background(Color(0xFF2A2A2A))
                ) {
                    arrayOf(
                        "По популярности ↓",
                        "По популярности ↑",
                        "По эпизодам ↓",
                        "По эпизодам ↑"
                    ).forEachIndexed { index, option ->
                        DropdownMenuItem(text = {
                            Text(
                                option,
                                color = Color.White,
                                fontSize = 13.sp
                            )
                        }, onClick = { onSortMethodChange(index); isDropExpanded = false })
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Избранное",
                        color = Color(0xFFAAAAAA),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(end = 6.dp)
                    )
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
    fun ScheduleViewWithDates(
        animeList: List<Anime>,
        isFavoritesOnly: Boolean,
        customBadges: Map<Int, List<MainViewModel.CustomFolderPreview>>,
        sortMethod: Int,
        onSortMethodChange: (Int) -> Unit,
        onFavoritesChange: (Boolean) -> Unit
    ) {
        var selectedDay by remember { mutableIntStateOf(getTodayIndex()) }
        val daysTitles = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")

        val datesNumbers = remember {
            val list = mutableListOf<String>()
            val cal = Calendar.getInstance().apply {
                firstDayOfWeek = Calendar.MONDAY; set(
                Calendar.DAY_OF_WEEK,
                Calendar.MONDAY
            )
            }
            for (i in 0..6) {
                list.add(cal.get(Calendar.DAY_OF_MONTH).toString()); cal.add(
                    Calendar.DAY_OF_MONTH,
                    1
                )
            }
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
            val isCorrectDay =
                airingAt != null && getDayOfWeekFromTimestamp(airingAt) == selectedDay
            val passFilter = if (isFavoritesOnly) NotificationHelper.isNotificationEnabled(
                this@MainActivity,
                anime.id
            ) else true
            isCorrectDay && passFilter
        }.sortedBy { it.nextAiringEpisode?.airingAt }

        // Добавляем отступ снизу, чтобы карточки не прятались за меню
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            items(dayAnime) { anime ->
                val airingAt = anime.nextAiringEpisode?.airingAt
                val formattedTime = if (airingAt != null) SimpleDateFormat(
                    "HH:mm",
                    Locale.getDefault()
                ).format(Date(airingAt * 1000L)) else "--:--"
                var isNotifying by remember {
                    mutableStateOf(
                        NotificationHelper.isNotificationEnabled(
                            this@MainActivity,
                            anime.id
                        )
                    )
                }

                com.example.animouse.ui.compose.AnimeListCard(
                    anime = anime,
                    customBadges = customBadges[anime.id] ?: emptyList(),
                    airingTime = formattedTime,
                    isNotificationEnabled = isNotifying,
                    onNotificationClick = {
                        val airingAtSafe = anime.nextAiringEpisode?.airingAt?.toLong() ?: 0L
                        val episode = anime.nextAiringEpisode?.episode ?: 0
                        isNotifying = NotificationHelper.toggleNotification(
                            this@MainActivity,
                            anime.id,
                            anime.title.romaji ?: "Аниме",
                            airingAtSafe,
                            episode
                        )
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
    fun CustomBottomNavigationBar(
        currentTab: Int,
        isGridView: Boolean,
        modifier: Modifier = Modifier,
        onTabClick: (Int) -> Unit,
        onTabLongClick: () -> Unit
    ) {
        Column(modifier = modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier.fillMaxWidth().height(24.dp).background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0xFF121212)
                        )
                    )
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth().background(Color(0xFF121212))
                    .navigationBarsPadding().height(60.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // --- ТАБ 0: ГЛАВНАЯ ---
                val isSelected0 = currentTab == 0
                Column(
                    modifier = Modifier.weight(1f).fillMaxHeight().clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onTabClick(0) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // 👇 ВПИШИ ИКОНКИ ДЛЯ "ГЛАВНАЯ" (например ic_star_sol / ic_star_reg)
                    val icon0 = if (isSelected0) R.drawable.ic_star_sol else R.drawable.ic_star_reg
                    Icon(
                        painter = painterResource(icon0),
                        contentDescription = null,
                        tint = if (isSelected0) Color(0xFFFF9800) else Color(0xFFAAAAAA),
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        "Главная",
                        color = if (isSelected0) Color(0xFFFF9800) else Color(0xFFAAAAAA),
                        fontSize = 11.sp,
                        fontWeight = if (isSelected0) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                // --- ТАБ 1: ОНГОИНГИ ---
                val isSelected1 = currentTab == 1
                val icon1 = when {
                    isGridView && isSelected1 -> R.drawable.ic_media_sol // 👇 Твои иконки для Онгоингов/Календаря
                    isGridView && !isSelected1 -> R.drawable.ic_media_reg
                    !isGridView && isSelected1 -> R.drawable.ic_calendar_sol
                    else -> R.drawable.ic_calendar_reg
                }
                Column(
                    modifier = Modifier.weight(1f).fillMaxHeight().combinedClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onTabClick(1) },
                        onLongClick = onTabLongClick
                    ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(icon1),
                        contentDescription = null,
                        tint = if (isSelected1) Color(0xFFFF9800) else Color(0xFFAAAAAA),
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = if (isGridView) "Онгоинги" else "Расписание",
                        color = if (isSelected1) Color(0xFFFF9800) else Color(0xFFAAAAAA),
                        fontSize = 11.sp,
                        fontWeight = if (isSelected1) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                // --- ТАБ 2: СПИСКИ ---
                val isSelected2 = currentTab == 2
                Column(
                    modifier = Modifier.weight(1f).fillMaxHeight().clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onTabClick(2) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // 👇 ВПИШИ ИКОНКИ ДЛЯ "СПИСКОВ"
                    val icon2 =
                        if (isSelected2) R.drawable.ic_bookmark_sol else R.drawable.ic_bookmark_reg
                    Icon(
                        painter = painterResource(icon2),
                        contentDescription = null,
                        tint = if (isSelected2) Color(0xFFFF9800) else Color(0xFFAAAAAA),
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        "Списки",
                        color = if (isSelected2) Color(0xFFFF9800) else Color(0xFFAAAAAA),
                        fontSize = 11.sp,
                        fontWeight = if (isSelected2) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.padding(top = 2.dp)
                    )
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
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp, start = 8.dp, end = 8.dp, top = 8.dp)
        ) {
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

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Всего в списках: $total",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        if (total > 0) {
                            Row(
                                modifier = Modifier.fillMaxWidth().height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                            ) {
                                if (wCount > 0) Box(
                                    modifier = Modifier.weight(wCount.toFloat()).fillMaxHeight()
                                        .background(Color(0xFF00BFA5))
                                )
                                if (pCount > 0) Box(
                                    modifier = Modifier.weight(pCount.toFloat()).fillMaxHeight()
                                        .background(Color(0xFFFF9800))
                                )
                                if (cCount > 0) Box(
                                    modifier = Modifier.weight(cCount.toFloat()).fillMaxHeight()
                                        .background(Color(0xFF4CAF50))
                                )
                                if (dCount > 0) Box(
                                    modifier = Modifier.weight(dCount.toFloat()).fillMaxHeight()
                                        .background(Color(0xFF4B5563))
                                )
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
            item {
                FolderRow(
                    "WATCHING",
                    "Смотрю",
                    wCount,
                    wAnime.mapNotNull { it.posterUrl }.take(3),
                    Color(0xFF00BFA5),
                    onOpenFolder
                ) { shareList("Смотрю", wAnime) }
            }

            val pAnime = localAnime.filter { statuses[it.animeId] == "PLANNED" }
            item {
                FolderRow(
                    "PLANNED",
                    "В планах",
                    pCount,
                    pAnime.mapNotNull { it.posterUrl }.take(3),
                    Color(0xFFFF9800),
                    onOpenFolder
                ) { shareList("В планах", pAnime) }
            }

            val cAnime = localAnime.filter { statuses[it.animeId] == "COMPLETED" }
            item {
                FolderRow(
                    "COMPLETED",
                    "Просмотрено",
                    cCount,
                    cAnime.mapNotNull { it.posterUrl }.take(3),
                    Color(0xFF4CAF50),
                    onOpenFolder
                ) { shareList("Просмотрено", cAnime) }
            }

            val dAnime = localAnime.filter { statuses[it.animeId] == "DROPPED" }
            item {
                FolderRow(
                    "DROPPED",
                    "Брошено",
                    dCount,
                    dAnime.mapNotNull { it.posterUrl }.take(3),
                    Color(0xFF4B5563),
                    onOpenFolder
                ) { shareList("Брошено", dAnime) }
            }

            // Шаг 1.1: Перенесли заголовок и добавили кнопку "+ Создать" в один ряд
// Заголовок и кнопка "+ Создать"
            item {
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Пользовательские списки",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
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
                                .setPositiveButton("Поделиться") { _, _ ->
                                    shareList(
                                        preview.name,
                                        listAnime
                                    )
                                }
                                .setNeutralButton("Редактировать") { _, _ ->
                                    // 👇 Вместо прямых переменных вызываем колбэк!
                                    onManageListClick(preview.id, preview.name, preview.colorHex)
                                }
                                .setNegativeButton("Удaлить") { _, _ ->
                                    viewModel.deleteCustomList(
                                        preview.id
                                    )
                                }
                                .show()
                        }
                    )
                }
            }
        }
    }

    @Composable
    fun LegendItem(color: Color, text: String) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            Box(modifier = Modifier.size(10.dp).background(color, RoundedCornerShape(5.dp)))
            Text(
                text,
                color = Color(0xFFAAAAAA),
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 6.dp)
            )
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        // Иконка глазика + текст
                        Icon(
                            painterResource(R.drawable.ic_eye_sol),
                            contentDescription = null,
                            tint = color,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            "Всего в списке: $count",
                            color = Color(0xFFAAAAAA),
                            fontSize = 13.sp,
                            modifier = Modifier.padding(start = 6.dp)
                        )
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
                            modifier = Modifier.padding(end = 12.dp).width(46.dp)
                                .fillMaxHeight(0.8f)
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
            putStringArrayListExtra(
                "EXTRA_GENRES",
                java.util.ArrayList(anime.genres ?: emptyList())
            )
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
        id = entity.animeId,
        idMal = entity.idMal,
        title = com.example.animouse.data.model.Title(romaji = entity.title),
        coverImage = com.example.animouse.data.model.CoverImage(large = entity.posterUrl ?: ""),
        averageScore = entity.score,
        episodes = entity.episodesTotal,
        description = "Данные из оффлайн-списка",
        genres = emptyList(),
        status = entity.animeStatus,
        season = entity.season,
        seasonYear = entity.seasonYear,
        nextAiringEpisode = com.example.animouse.data.model.NextAiringEpisode(
            0,
            0,
            entity.episodesAired + 1
        )
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
                    id = preview.id,
                    name = preview.name,
                    colorHex = preview.colorHex,
                    isAdded = isAdded
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

    private fun showCustomListManageDialog(
        listId: Int? = null,
        currentName: String? = null,
        currentColorHex: String? = null
    ) { /* Оставлено без изменений */
    }

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
    // Вспомогательный компонент для заголовков (Текст слева, "Показать всё" справа)


    @Composable
    fun SectionHeader(title: String, onShowAllClick: () -> Unit) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp) // Сначала задаем бока
                .padding(top = 24.dp, bottom = 8.dp), // Затем верх и низ
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("Показать всё", color = Color(0xFFFF9800), fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { onShowAllClick() })
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun DiscoveryScreen(
        state: MainViewModel.DiscoveryUiState,
        statuses: Map<Int, String>,
        customBadges: Map<Int, List<MainViewModel.CustomFolderPreview>>,
        onAnimeClick: (Anime) -> Unit,
        // 👇 1. ДОБАВИЛИ СЮДА В ШАПКУ
        onNavigateToOngoing: () -> Unit,
        onNavigateToCollection: (String, String) -> Unit
    ) {
        val context = androidx.compose.ui.platform.LocalContext.current
        HorizontalDivider(color = Color(0xFF2A2A2A), modifier = Modifier.padding(horizontal = 15.dp))
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp, top = 8.dp)
        ) {
        // 1. ЖЕЛЕЗНАЯ КАРУСЕЛЬ НОВОСТЕЙ
            item {
                // Заголовок теперь рисуется ВСЕГДА, он больше не обернут в if
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Новости Шикимори",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = {
                        val intent = Intent(this@MainActivity, AllNewsActivity::class.java)
                        startActivity(intent)
                    }) {
                        Text("Показать больше", color = Color(0xFFFF9800), fontWeight = FontWeight.Bold)
                    }
                }

                // А вот контент внутри меняется плавно:
                if (state.news.isNotEmpty()) {
                    val pagerState = rememberPagerState(pageCount = { state.news.size })
                    HorizontalPager(
                        state = pagerState,
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        pageSpacing = 8.dp
                    ) { page ->
                        val newsItem = state.news[page]
                        NewsCard(news = newsItem, onClick = {
                            com.example.animouse.data.model.NewsDataHolder.selectedNews = newsItem
                            val intent = Intent(this@MainActivity, NewsDetailsActivity::class.java)
                            startActivity(intent)
                        })
                    }
                } else {
                    // Лоадер высотой с карточку новости (чтобы контент ниже не прыгал)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFFFF9800))
                    }
                }
            }
            // 2. В ЭТОМ СЕЗОНЕ (Тренды)
            if (state.trending.isNotEmpty()) {
                item {
                    SectionHeader(title = "В этом сезоне", onShowAllClick = { onNavigateToOngoing() })
                    LazyRow(contentPadding = PaddingValues(horizontal = 10.dp)) {
                        items(state.trending) { entity ->
                            val anime = mapEntityToAnimeForDiscovery(entity)
                            Box(modifier = Modifier.width(160.dp)) {
                                AnimeCard(anime = anime, systemStatusText = null, systemStatusColor = Color.Transparent, customBadges = customBadges[anime.id] ?: emptyList(), onClick = { onAnimeClick(anime) }, onLongClick = { })
                            }
                        }
                    }
                }
            }

            // 3. БУДУЩИЕ ХИТЫ (Анонсы)
            if (state.upcoming.isNotEmpty()) {
                item {
                    // 👇 ПРИВЯЗАЛИ КОЛБЭК
                    SectionHeader(title = "Будущие хиты", onShowAllClick = { onNavigateToCollection("UPCOMING", "Будущие хиты") })
                    LazyRow(contentPadding = PaddingValues(horizontal = 10.dp)) {
                        items(state.upcoming) { entity ->
                            val anime = mapEntityToAnimeForDiscovery(entity)
                            Box(modifier = Modifier.width(160.dp)) {
                                AnimeCard(anime = anime, systemStatusText = null, systemStatusColor = Color.Transparent, customBadges = customBadges[anime.id] ?: emptyList(), onClick = { onAnimeClick(anime) }, onLongClick = { })
                            }
                        }
                    }
                }
            }

            // 4. ТОП-100 ВСЕХ ВРЕМЕН
            if (state.top100.isNotEmpty()) {
                item {
                    // 👇 ПРИВЯЗАЛИ КОЛБЭК
                    SectionHeader(title = "Топ 100 за всё время", onShowAllClick = { onNavigateToCollection("TOP_100", "Топ 100") })
                    val sortedTop = state.top100.sortedByDescending { it.score }

                    LazyRow(contentPadding = PaddingValues(horizontal = 10.dp)) {
                        items(sortedTop) { entity ->
                            val anime = mapEntityToAnimeForDiscovery(entity)
                            Box(modifier = Modifier.width(160.dp)) {
                                AnimeCard(anime = anime, systemStatusText = null, systemStatusColor = Color.Transparent, customBadges = customBadges[anime.id] ?: emptyList(), onClick = { onAnimeClick(anime) }, onLongClick = { })
                            }
                        }
                    }
                }
            }

            // 5. ФУТЕР: ИСТОЧНИКИ ДАННЫХ
            item {
                Spacer(modifier = Modifier.height(32.dp))
                HorizontalDivider(color = Color(0xFF2A2A2A), modifier = Modifier.padding(horizontal = 32.dp))
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Данные предоставлены:",
                        color = Color(0xFFAAAAAA),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Shikimori",
                            color = Color(0xFFFF9800),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable {
                                context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://shikimori.one")))
                            }
                        )
                        Text(
                            text = "AniList",
                            color = Color(0xFF00BFA5),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable {
                                context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://anilist.co")))
                            }
                        )
                    }
                    Text(
                        text = "Красноярск 2026    AniMouse v2.0",
                        color = Color(0xFF555555),
                        fontSize = 10.sp,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
            }
        }
    } // Конец DiscoveryScreen

    // Маппер специально для каруселей
    fun mapEntityToAnimeForDiscovery(entity: com.example.animouse.data.database.AnimeListItemEntity): Anime {
        // 👇 ЧИНИМ СТАТУСЫ: Умная логика плашек в зависимости от карусели
        val status = when (entity.listType) {
            "UPCOMING" -> "NOT_YET_RELEASED" // Анонс
            "TOP_100" -> "FINISHED" // Стальной алхимик скажет спасибо!
            else -> "RELEASING" // Для текущего сезона
        }

        return Anime(
            id = entity.idMal, idMal = entity.idMal, title = Title(romaji = entity.titleRussian ?: entity.titleRomaji ?: "Без названия"),
            coverImage = CoverImage(large = entity.posterUrl ?: ""), averageScore = entity.score, episodes = entity.episodes,
            status = status, // 👈 Передаем правильный статус
            season = null, seasonYear = null, description = null, genres = emptyList(), nextAiringEpisode = null
        )
    }
}