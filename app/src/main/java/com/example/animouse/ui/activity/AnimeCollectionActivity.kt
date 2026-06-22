package com.example.animouse.ui.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.animouse.R
import com.example.animouse.data.model.Anime
import com.example.animouse.data.model.CoverImage
import com.example.animouse.data.model.Title
import com.example.animouse.ui.compose.AnimeCard
import com.example.animouse.ui.viewmodel.AnimeCollectionViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AnimeCollectionActivity : AppCompatActivity() {
    private val viewModel: AnimeCollectionViewModel by viewModels()
    private val mainViewModel: com.example.animouse.ui.viewmodel.MainViewModel by viewModels() // 👈 Добавили
    private val isCreateDialogVisible = androidx.compose.runtime.mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Получаем команды от телевизора (MainActivity)
        val collectionType = intent.getStringExtra("EXTRA_COLLECTION_TYPE") ?: "TOP_100"
        val collectionTitle = intent.getStringExtra("EXTRA_COLLECTION_TITLE") ?: "Коллекция"

        setContent {
            CollectionScreen(collectionType, collectionTitle)
            // 👇 2. Рисуем диалог поверх экрана, если состояние true
            if (isCreateDialogVisible.value) {
                com.example.animouse.ui.compose.CustomListComposeDialog(
                    listId = null,
                    initialName = "",
                    initialColorHex = "#FFFF9800",
                    onDismiss = { isCreateDialogVisible.value = false },
                    onSave = { name, hexColor ->
                        mainViewModel.createCustomList(name, hexColor) // Сохраняем в БД!
                        isCreateDialogVisible.value = false
                    }
                )
            }
        }
    }

    @Composable
    fun CollectionScreen(collectionType: String, title: String) {
        val animeList by viewModel.animeList.collectAsState()
        val isLoading by viewModel.isLoading.collectAsState()
        val statuses by mainViewModel.animeStatuses.observeAsState(emptyMap())
        val customBadges by mainViewModel.animeCustomBadges.observeAsState(emptyMap())

        // Запрашиваем данные при старте экрана
        LaunchedEffect(collectionType) {
            viewModel.loadCollection(collectionType)
        }

        Column(modifier = Modifier.fillMaxSize().background(Color(0xFF121212))) {
            // Тулбар
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 12.dp)
                    .statusBarsPadding(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { finish() }) {
                    Icon(painterResource(R.drawable.ic_arrow_reg), contentDescription = "Назад", tint = Color.White)
                }
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
            }

            if (isLoading && animeList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFFFF9800))
                }
            } else {
                // 👇 ДОБАВЛЯЕМ СОРТИРОВКУ: Если это Топ 100, сортируем по оценке (по убыванию)
                val sortedList = if (collectionType == "TOP_100") {
                    animeList.sortedByDescending { it.score }
                } else {
                    animeList // Анонсы оставляем как есть, сервер Шикимори сам сортирует их по дате выхода
                }

                // ИДЕАЛЬНАЯ СЕТКА ИЗ СПИСКОВ
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp, start = 8.dp, end = 8.dp, top = 8.dp)
                ) {
                    // 👇 Передаем сюда наш отсортированный список (sortedList)
                    items(sortedList) { entity ->
                        val anime = mapEntityToAnimeForDiscovery(entity, collectionType)

                        // 👇 Высчитываем системную плашку
                        val currentStatus = statuses[anime.id]
                        val (statusText, statusColor) = when (currentStatus) {
                            "WATCHING" -> "Смотрю" to Color(0xFF00BFA5)
                            "PLANNED" -> "В планах" to Color(0xFFFF9800)
                            "COMPLETED" -> "Просмотрено" to Color(0xFF4CAF50)
                            "DROPPED" -> "Брошено" to Color(0xFF757575)
                            else -> null to Color.Transparent
                        }

                        AnimeCard(
                            anime = anime,
                            systemStatusText = statusText, // 👈 Передаем плашку
                            systemStatusColor = statusColor,
                            customBadges = customBadges[anime.id] ?: emptyList(), // 👈 Передаем бейджики
                            onClick = { navigateToDetails(anime) },
                            onLongClick = { showBottomSheetDialog(anime) } // 👈 Включаем долгое нажатие!
                        )
                    }
                }
            }
        }
    }

    // Тот самый маппер с главной страницы
    private fun mapEntityToAnimeForDiscovery(
        entity: com.example.animouse.data.database.AnimeListItemEntity,
        collectionType: String
    ): Anime {
        val status = when (collectionType) {
            "UPCOMING" -> "NOT_YET_RELEASED"
            "TOP_100" -> "FINISHED"
            else -> "RELEASING"
        }

        return Anime(
            id = entity.idMal, idMal = entity.idMal,
            title = Title(romaji = entity.titleRussian ?: entity.titleRomaji ?: "Без названия"),
            coverImage = CoverImage(large = entity.posterUrl ?: ""),
            averageScore = entity.score, episodes = entity.episodes,
            status = status, season = null, seasonYear = null,
            description = null, genres = emptyList(), nextAiringEpisode = null
        )
    }

    private fun navigateToDetails(anime: Anime) {
        val intent = Intent(this, DetailsActivity::class.java).apply {
            putExtra("EXTRA_ID", anime.id)
            putExtra("EXTRA_ID_MAL", anime.idMal ?: -1)
            putExtra("EXTRA_TITLE", anime.title.romaji)
            putExtra("EXTRA_POSTER", anime.coverImage.large)
            putExtra("EXTRA_SCORE", anime.averageScore ?: 0)
            putExtra("EXTRA_EPISODES_TOTAL", anime.episodes ?: 0)
        }
        startActivity(intent)
    }
    private fun showBottomSheetDialog(anime: Anime) {
        val bottomSheetDialog = com.google.android.material.bottomsheet.BottomSheetDialog(this)
        val composeView = androidx.compose.ui.platform.ComposeView(this)

        // 👇 1. ФИКС БЕЛЫХ УГЛОВ: Убиваем системный фон старого диалога
        bottomSheetDialog.setOnShowListener {
            val bottomSheet = bottomSheetDialog.findViewById<android.view.View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        }

        bottomSheetDialog.setContentView(composeView)

        composeView.setContent {
            val customPreviews by mainViewModel.customFolderPreviews.observeAsState(emptyList())
            val customFolderAnime by mainViewModel.customFolderAnime.observeAsState(emptyMap())

            // 👇 2. ФИКС ДЫРЫ ЖЕСТОВ: Обертка, которая красит фон до самого низа
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = Color(0xFF1E1E1E), // Твой фирменный темный фон
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp) // Красивые скругления
                    )
                    .navigationBarsPadding() // Закрашивает пустоту под полоской жестов
                    .padding(top = 16.dp, bottom = 8.dp) // Аккуратные отступы для контента
            ) {
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
                        mainViewModel.updateAnimeStatusUniversal(anime, status)
                        bottomSheetDialog.dismiss()
                    },
                    onRemove = {
                        mainViewModel.updateAnimeStatusUniversal(anime, null)
                        bottomSheetDialog.dismiss()
                    },
                    onCreateCustomList = {
                        bottomSheetDialog.dismiss()
                        isCreateDialogVisible.value = true // Вызываем красивый диалог
                    },
                    onToggleCustomList = { listId, isAdding ->
                        if (!isAdding) {
                            // 👇 МЕНЯЕМ БИЛДЕР НА СОВМЕСТИМЫЙ С НАШЕЙ ТЕМОЙ
                            androidx.appcompat.app.AlertDialog.Builder(this@AnimeCollectionActivity)
                                .setTitle("Удалить из списка?")
                                .setMessage("Убрать тайтл из этого списка?")
                                .setPositiveButton("Удалить") { _, _ ->
                                    mainViewModel.toggleAnimeInCustomListUniversal(anime, listId, false)
                                    bottomSheetDialog.dismiss()
                                }
                                .setNegativeButton("Отмена", null)
                                .show()
                        } else {
                            mainViewModel.toggleAnimeInCustomListUniversal(anime, listId, true)
                            bottomSheetDialog.dismiss()
                        }
                    }
                )
            }
        }
        bottomSheetDialog.show()
    }
}