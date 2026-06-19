package com.example.animouse.ui.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.CalendarContract
import android.text.Html
import android.view.View
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.animouse.R
import com.example.animouse.data.NotificationHelper
import com.example.animouse.data.database.NoteEntity
import com.example.animouse.ui.viewmodel.DetailsViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.ExperimentalMaterial3Api
import android.app.DownloadManager
import android.content.Context
import android.os.Environment
import androidx.compose.foundation.lazy.itemsIndexed
import android.content.ContentValues

import android.graphics.Bitmap
import android.os.Build
import android.provider.MediaStore
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toBitmap
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.animouse.ui.compose.ScreenshotViewerOverlay
import kotlinx.coroutines.withContext



@AndroidEntryPoint
class DetailsActivity : AppCompatActivity() {

    private val viewModel: DetailsViewModel by viewModels()

    // Сохраняем переменные уровня класса, чтобы BottomSheets имели к ним доступ
    private var currentAnimeId = -1
    private var idMal = -1
    private var currentTitle = "Без названия"
    private var posterUrl: String? = null
    private var score = 0
    private var totalEpisodesAniList = 0
    private var descEnglish = "Описание отсутствует"
    private var genres = arrayListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Извлекаем данные
        currentAnimeId = intent.getIntExtra("EXTRA_ID", -1)
        idMal = intent.getIntExtra("EXTRA_ID_MAL", -1)
        currentTitle = intent.getStringExtra("EXTRA_TITLE") ?: "Без названия"
        posterUrl = intent.getStringExtra("EXTRA_POSTER")
        score = intent.getIntExtra("EXTRA_SCORE", 0)
        totalEpisodesAniList = intent.getIntExtra("EXTRA_EPISODES_TOTAL", 0)
        descEnglish = intent.getStringExtra("EXTRA_DESC_ENG") ?: "Описание отсутствует"
        genres = intent.getStringArrayListExtra("EXTRA_GENRES") ?: arrayListOf()

        // 3. Загружаем данные ViewModel
        if (currentAnimeId != -1) {
            viewModel.loadStatus(currentAnimeId)
            viewModel.loadNotes(currentAnimeId)
            viewModel.loadCustomListsData(currentAnimeId)
        }
        // 👇 Безопасно обновляем данные с серверов
        if (idMal != -1) {
            viewModel.loadAnimeDetails(idMal)
            viewModel.loadScreenshots(idMal)
        }
        viewModel.loadAniListExtra(currentAnimeId, idMal)

        // 4. Отрисовываем интерфейс в Compose!
        setContent {
            DetailsScreen()
        }

        // Обработка AniList Экстра-данных (если пришли из поиска)
        viewModel.aniListExtra.observe(this) { extraData ->
            if (currentAnimeId == -1 && extraData?.id != null) {
                currentAnimeId = extraData.id
                viewModel.loadStatus(currentAnimeId)
                viewModel.loadNotes(currentAnimeId)
                viewModel.loadCustomListsData(currentAnimeId)
            }
        }
    }

    // =========================================================================
    // МАГИЯ COMPOSE НАЧИНАЕТСЯ ЗДЕСЬ
    // =========================================================================
    @OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
    @Composable
    fun DetailsScreen() {
        var selectedScreenshotIndex by remember { mutableStateOf<Int?>(null) }
        val screenshots by viewModel.screenshots.observeAsState(emptyList())
        val scrollState = rememberScrollState()
        val coroutineScope = rememberCoroutineScope()
        // Подписываемся на все LiveData
        val animeDetails by viewModel.animeDetails.observeAsState()
        val aniListExtra by viewModel.aniListExtra.observeAsState()
        val notes by viewModel.notes.observeAsState(emptyList())
        val activeCustomListIds by viewModel.activeCustomListIds.observeAsState(emptyList())
        val allCustomLists by viewModel.allCustomLists.observeAsState(emptyList())
        val currentStatus by viewModel.currentStatus.observeAsState()
        val error by viewModel.error.observeAsState()

        var isDescriptionExpanded by remember { mutableStateOf(false) }
        var isMenuExpanded by remember { mutableStateOf(false) }
        var isNotesSheetVisible by remember { mutableStateOf(false) } // 👈 Добавили стейт
        var isRefreshing by remember { mutableStateOf(false) } // 👈 Добавили флаг обновления
        val pullRefreshState = rememberPullToRefreshState() // 👈 Добавляем это

        // Обновляем тайтл, если пришел с Шикимори
        val displayTitle = animeDetails?.russian?.takeIf { it.isNotBlank() } ?: currentTitle

        // Логика эпизодов
        // Логика эпизодов (Гибридная: AniList + Shikimori)
        val totalEp = if (totalEpisodesAniList > 0) totalEpisodesAniList else (animeDetails?.episodes ?: 0)

        val shikiStatus = animeDetails?.status ?: ""
        val nextAniEp = aniListExtra?.nextAiringEpisode?.episode

        val airedEp = if (nextAniEp != null && nextAniEp > 0) {
            nextAniEp - 1 // AniList знает точное расписание онгоинга
        } else if (shikiStatus.lowercase() in listOf("released", "finished", "completed")) {
            totalEp // Если тайтл завершен, значит вышли все серии
        } else {
            animeDetails?.episodes_aired ?: 0 // Страховочный фолбэк на Шикимори
        }

        // Логика описания
        val rawDesc = animeDetails?.description
        val displayDesc = if (!rawDesc.isNullOrBlank() && rawDesc != "Описание отсутствует.") {
            rawDesc.replace(Regex("\\[.*?\\]"), "") // Убираем BB-коды
        } else {
            Html.fromHtml(descEnglish, Html.FROM_HTML_MODE_LEGACY).toString().trim().ifEmpty { "Описание отсутствует" }
        }

        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF121212))) {
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = {
                    isRefreshing = true
                    coroutineScope.launch {
                        // Повторяем ту же логику загрузки, что была в onCreate
                        if (currentAnimeId != -1) {
                            viewModel.loadStatus(currentAnimeId)
                            viewModel.loadNotes(currentAnimeId)
                            viewModel.loadCustomListsData(currentAnimeId)
                        }
                        // 👇 Безопасные вызовы к API Шикимори
                        if (idMal != -1) {
                            viewModel.loadAnimeDetails(idMal)
                            viewModel.loadScreenshots(idMal)
                        }

                        // Вызов к API AniList
                        viewModel.loadAniListExtra(currentAnimeId, idMal)

                        // Небольшая искусственная задержка
                        kotlinx.coroutines.delay(500)
                        isRefreshing = false
                    }
                },
                state = pullRefreshState, // 👈 Передаем состояние
                // 👈 ВОТ ПРАВИЛЬНАЯ КАСТОМИЗАЦИЯ ЦВЕТОВ
                indicator = {
                    PullToRefreshDefaults.Indicator(
                        modifier = Modifier.align(Alignment.TopCenter),
                        isRefreshing = isRefreshing,
                        state = pullRefreshState,
                        color = Color(0xFFFF9800),        // Наш любимый оранжевый
                        containerColor = Color(0xFF1E1E1E) // Темный фон кружка
                    )
                },
                modifier = Modifier.fillMaxSize()
            ) {
                // Основной скроллируемый контент
                Column(modifier = Modifier.verticalScroll(scrollState).padding(bottom = 40.dp)) {

                // 1. ПОСТЕР С ПАРАЛЛАКСОМ И ГРАДИЕНТОМ
                Box(
                    modifier = Modifier
                        .height(450.dp)
                        .fillMaxWidth()
                        .graphicsLayer {
                            // Параллакс эффект! Картинка едет в два раза медленнее скролла
                            translationY = scrollState.value * 0.5f
                        }
                ) {
                    AsyncImage(
                        model = posterUrl,
                        contentDescription = "Постер",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    // Градиент сверху вниз
                    Box(
                        modifier = Modifier.fillMaxSize().background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Transparent, Color(0xFF121212)),
                                startY = 0f,
                                endY = Float.POSITIVE_INFINITY
                            )
                        )
                    )
                }

                // 2. ИНФОРМАЦИОННЫЙ БЛОК (БЕЗ ДУБЛИКАТОВ И С СЕЗОНОМ)
                Column(modifier = Modifier.padding(horizontal = 20.dp).offset(y = (-40).dp)) {
                    Text(
                        text = displayTitle,
                        color = Color.White,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        style = TextStyle(shadow = Shadow(color = Color(0x80000000), offset = Offset(0f, 4f), blurRadius = 8f))
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(painterResource(R.drawable.ic_star_sol), null, tint = Color(0xFFFF9800), modifier = Modifier.size(18.dp))
                        Text(if (score > 0) "$score%" else "—", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 6.dp))
                        Text("|", color = Color(0xFF9CA3AF), fontSize = 16.sp, modifier = Modifier.padding(horizontal = 12.dp))
                        Text("$airedEp / ${if (totalEp > 0) totalEp else "?"} эп.", color = Color.White, fontSize = 15.sp)

                        Spacer(modifier = Modifier.weight(1f)) // Выталкиваем плашку в самый правый край

                        // Плашка статуса выхода (теперь одна, с годом и сезоном!)
                        val shikiStatus = animeDetails?.status
                        if (shikiStatus != null) {
                            val aniSeason = aniListExtra?.season
                            val aniYear = aniListExtra?.seasonYear
                            val translatedSeason = when (aniSeason?.uppercase()) {
                                "WINTER" -> "Зима"; "SPRING" -> "Весна"; "SUMMER" -> "Лето"; "FALL" -> "Осень"; else -> ""
                            }
                            // 🏁 Починили: вернули $translatedSeason на место!
                            val seasonSuffix = if (aniYear != null && translatedSeason.isNotEmpty()) " • $aniYear $translatedSeason" else ""

                            val (statusText, bgColor) = when (shikiStatus.lowercase()) {
                                "ongoing", "releasing" -> "Онгоинг" to Color(0xFF00BFA5)
                                "anons", "upcoming" -> "Анонс$seasonSuffix" to Color(0xFFFF9800)
                                "released", "finished" -> "Вышло$seasonSuffix" to Color(0xFF4CAF50)
                                else -> "" to Color.Transparent
                            }

                            if (statusText.isNotEmpty()) {
                                Box(modifier = Modifier.background(bgColor, RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                                    Text(statusText, color = Color(0xFF121212), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
                // 3. ТЕГИ
                FlowRow(
                    modifier = Modifier.padding(horizontal = 20.dp).offset(y = (-20).dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DetailBadge("ID: $currentAnimeId", Color(0xFF1E1E1E), Color(0xFFAAAAAA))

                    val activeLists = allCustomLists.filter { activeCustomListIds.contains(it.id) }
                    for (list in activeLists) {
                        DetailBadge(list.name, Color(android.graphics.Color.parseColor(list.colorHex)), Color(0xFF121212))
                    }

                    // 👈 Добавили серый цвет и иконку
                    for (genre in genres) {
                        DetailBadge(genre, Color(0xFF1E1E1E), Color(0xFFAAAAAA), R.drawable.ic_tag_reg)
                    }
                }

                // 4. КНОПКИ ДЕЙСТВИЙ
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val (btnText, btnColor, btnTextColor) = when (currentStatus) {
                        "WATCHING" -> Triple("Смотрю", Color(0xFF00BFA5), Color(0xFF121212))
                        "PLANNED" -> Triple("В планах", Color(0xFFFF9800), Color(0xFF121212))
                        "COMPLETED" -> Triple("Просмотрено", Color(0xFF4CAF50), Color(0xFF121212))
                        "DROPPED" -> Triple("Брошено", Color(0xFF1E1E1E), Color.White)
                        else -> Triple("Добавить в списки", Color(0xFF1E1E1E), Color.White)
                    }

                    Button(
                        onClick = { showBottomSheetDialog(currentAnimeId, idMal, displayTitle, posterUrl, score, totalEpisodesAniList) },
                        colors = ButtonDefaults.buttonColors(containerColor = btnColor),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f).height(56.dp)
                    ) {
                        Text(btnText, color = btnTextColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    OutlinedButton(
                        onClick = { isNotesSheetVisible = true }, // 👈 МЕНЯЕМ ЗДЕСЬ
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.size(56.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(
                            painterResource(if (notes.isNotEmpty()) R.drawable.ic_pencil_square_sol else R.drawable.ic_pencil_square_reg),
                            contentDescription = "Заметки",
                            tint = Color(0xFF00BFA5)
                        )
                    }
                }
                    // ==========================================
                    // 📸 ГАЛЕРЕЯ СКРИНШОТОВ (С плавной загрузкой)
                    // ==========================================
                    if (screenshots.isNotEmpty()) {
                        Text(
                            text = "Кадры",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 20.dp, top = 24.dp, bottom = 12.dp)
                        )

                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            itemsIndexed(screenshots) { index, url ->
                                Box(
                                    modifier = Modifier
                                        .width(240.dp)
                                        .height(135.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF2A2A2A))
                                        // 👇 ЭТОТ КЛИК САМЫЙ МОЩНЫЙ
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = ripple()
                                        ) {
                                            selectedScreenshotIndex = index
                                        }
                                ) {
                                    AsyncImage(
                                        model = url,
                                        contentDescription = "Скриншот",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                    }
                // 5. ОПИСАНИЕ
                Text("Сюжет", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 20.dp, top = 20.dp))

                Text(
                    text = displayDesc,
                    color = Color(0xFFAAAAAA),
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    maxLines = if (isDescriptionExpanded) Int.MAX_VALUE else 5,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp).animateContentSize()
                )

                Text(
                    text = if (isDescriptionExpanded) "Свернуть" else "Читать далее...",
                    color = Color(0xFFFF9800),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 20.dp).clickable { isDescriptionExpanded = !isDescriptionExpanded }
                )

                // 6. ТРЕЙЛЕР
                val trailer = aniListExtra?.trailer
                if (trailer != null && trailer.site == "youtube" && trailer.id != null) {
                    Text("Трейлер", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 20.dp, top = 32.dp))
                    Box(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp).fillMaxWidth().height(200.dp).clip(RoundedCornerShape(16.dp)).clickable {
                            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=${trailer.id}")))
                        }
                    ) {
                        AsyncImage(model = "https://img.youtube.com/vi/${trailer.id}/maxresdefault.jpg", contentDescription = "Трейлер", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().background(Color.Black), alpha = 0.7f)
                        Icon(painterResource(R.drawable.ic_media_sol), null, tint = Color.White, modifier = Modifier.size(48.dp).align(Alignment.Center))
                    }
                }

                // 8. СВЯЗАННЫЕ ТАЙТЛЫ
                val relations = aniListExtra?.relations?.edges?.filter { it.node.title.romaji != null }
                if (!relations.isNullOrEmpty()) {
                    Text("Связанное", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 20.dp, top = 32.dp))
                    LazyRow(
                        modifier = Modifier.padding(top = 12.dp),
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(relations) { relation ->
                            // Маленькая карточка для связанных тайтлов
                            Column(modifier = Modifier.width(120.dp).clickable {
                                val intent = Intent(this@DetailsActivity, DetailsActivity::class.java).apply {
                                    putExtra("EXTRA_ID", relation.node.id)
                                    putExtra("EXTRA_TITLE", relation.node.title.romaji)
                                    putExtra("EXTRA_POSTER", relation.node.coverImage?.large)
                                    putExtra("EXTRA_SCORE", relation.node.averageScore ?: 0)
                                    putExtra("EXTRA_EPISODES_TOTAL", relation.node.episodes ?: 0)
                                }
                                startActivity(intent)
                            }) {
                                AsyncImage(
                                    model = relation.node.coverImage?.large,
                                    contentDescription = "Постер",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxWidth().height(170.dp).clip(RoundedCornerShape(12.dp))
                                )
                                Text(relation.relationType ?: "", color = Color(0xFF00BFA5), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp))
                                Text(relation.node.title.romaji ?: "", color = Color.White, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
                            }
                        }
                    }
                }
            }
            } // 👈 Закрыли PullToRefreshBox

// ПОВЕРХ СКРОЛЛА: Тулбар с кнопками Назад и Меню
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 40.dp, start = 8.dp, end = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = { finish() }) { Icon(painterResource(R.drawable.ic_arrow_reg), null, tint = Color.White) }

                // Обернули кнопку в Box, чтобы меню выпадало прямо под ней
                Box {
                    IconButton(onClick = { isMenuExpanded = true }) {
                        Icon(painterResource(R.drawable.ic_menu_dots_reg), null, tint = Color.White)
                    }

                    DropdownMenu(
                        expanded = isMenuExpanded,
                        onDismissRequest = { isMenuExpanded = false },
                        modifier = Modifier.background(Color(0xFF1E1E1E)) // Темный фон, как у карточек
                    ) {
                        val isNotifying = NotificationHelper.isNotificationEnabled(this@DetailsActivity, currentAnimeId)

                        DropdownMenuItem(
                            text = { Text(if (isNotifying) "Больше не напоминать" else "Напомнить!", color = Color.White) },
                            leadingIcon = {
                                Icon(
                                    painterResource(if (isNotifying) R.drawable.ic_notification_bell_off_sol else R.drawable.ic_notification_bell_alarm_sol),
                                    contentDescription = null,
                                    tint = Color(0xFFFF9800)
                                )
                            },
                            onClick = {
                                val nextEp = aniListExtra?.nextAiringEpisode
                                val newState = NotificationHelper.toggleNotification(this@DetailsActivity, currentAnimeId, currentTitle, nextEp?.airingAt?.toLong() ?: 0L, nextEp?.episode ?: 0)
                                Toast.makeText(this@DetailsActivity, if (newState) "Напомню!" else "Не буду", Toast.LENGTH_SHORT).show()
                                isMenuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Поделиться ссылкой", color = Color.White) },
                            leadingIcon = { Icon(painterResource(R.drawable.ic_share_network_sol), null, tint = Color(0xFFFF9800)) },
                            onClick = {
                                startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, "Слежу за «${currentTitle}»!\nhttps://shikimori.one/animes/$idMal")
                                }, "Поделиться"))
                                isMenuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("В Google Календарь", color = Color.White) },
                            leadingIcon = { Icon(painterResource(R.drawable.ic_calendar_sol), null, tint = Color(0xFFFF9800)) },
                            onClick = {
                                val nextEp = aniListExtra?.nextAiringEpisode
                                if (nextEp != null && nextEp.airingAt > 0) {
                                    startActivity(Intent(Intent.ACTION_INSERT).apply {
                                        data = CalendarContract.Events.CONTENT_URI
                                        putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, nextEp.airingAt * 1000L)
                                        putExtra(CalendarContract.Events.TITLE, "AniMouse: «${currentTitle}»")
                                        putExtra(CalendarContract.Events.RRULE, "FREQ=WEEKLY;COUNT=1")
                                    })
                                } else Toast.makeText(this@DetailsActivity, "Дата неизвестна", Toast.LENGTH_SHORT).show()
                                isMenuExpanded = false
                            }
                        )
                    }
                }
            }
            // 👇 ДОБАВЛЯЕМ ПРЯМО ПЕРЕД ЗАКРЫТИЕМ BOX В DetailsScreen 👇
            if (isNotesSheetVisible) {
                @OptIn(ExperimentalMaterial3Api::class)
                ModalBottomSheet(
                    onDismissRequest = { isNotesSheetVisible = false },
                    containerColor = Color(0xFF1E1E1E), // Темный фон шторки
                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true) // 👈 Заставляет шторку сразу открыться полностью (важно для клавиатуры)
                ) {
                    val notesList by viewModel.notes.observeAsState(emptyList())

                    com.example.animouse.ui.compose.NotesBottomSheetContent(
                        notes = notesList,
                        onSendClick = { text, editingNote ->
                            if (editingNote != null) {
                                viewModel.addOrUpdateNote(editingNote.copy(content = text))
                            } else {
                                viewModel.addOrUpdateNote(com.example.animouse.data.database.NoteEntity(animeId = currentAnimeId, content = text))
                            }
                        },
                        onDeleteClick = { note -> viewModel.deleteNote(note) }
                    )
                }
            }
        } // 👈 Это закрывающая скобка Box(modifier = Modifier.fillMaxSize().background(Color(0xFF121212)))
        if (selectedScreenshotIndex != null) {
            ScreenshotViewerOverlay(
                screenshots = screenshots,
                initialIndex = selectedScreenshotIndex!!,
                onDismiss = { selectedScreenshotIndex = null },
                context = LocalContext.current
            )
        }
        // --- ДИАЛОГ ПРОСМОТРА СКРИНШОТОВ ---
    } // 👈 Это закрытие DetailsScreen

    // Компонент тега
    // Компонент тега с поддержкой иконки
    @Composable
    fun DetailBadge(text: String, bgColor: Color, textColor: Color, iconRes: Int? = null) {
        Row(
            modifier = Modifier.background(bgColor, RoundedCornerShape(8.dp)).padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (iconRes != null) {
                Icon(painterResource(iconRes), contentDescription = null, tint = textColor, modifier = Modifier.size(14.dp).padding(end = 4.dp))
            }
            Text(text, color = textColor, fontSize = 14.sp)
        }
    }

    private fun showBottomSheetDialog(animeId: Int, idMal: Int, title: String, posterUrl: String?, score: Int, epTotalAniList: Int) {
        val bottomSheetDialog = BottomSheetDialog(this)
        val composeView = androidx.compose.ui.platform.ComposeView(this)
        bottomSheetDialog.setContentView(composeView)

        composeView.setContent {
            val allLists by viewModel.allCustomLists.observeAsState(emptyList())
            val activeIds by viewModel.activeCustomListIds.observeAsState(emptyList())

            val customLists = allLists.map { list ->
                com.example.animouse.ui.compose.BottomSheetListState(
                    id = list.id, name = list.name, colorHex = list.colorHex, isAdded = activeIds.contains(list.id)
                )
            }

            val currentDetails by viewModel.animeDetails.observeAsState()
            val extraData by viewModel.aniListExtra.observeAsState()

            val epAired = currentDetails?.episodes_aired ?: 0
            val epTotal = if (epTotalAniList > 0) epTotalAniList else (currentDetails?.episodes ?: 0)
            val animeReleaseStatus = currentDetails?.status
            val animeSeason = extraData?.season
            val animeSeasonYear = extraData?.seasonYear

            com.example.animouse.ui.compose.StatusBottomSheetContent(
                animeTitle = title,
                customLists = customLists,
                onStatusSelect = { status ->
                    viewModel.updateStatus(animeId, idMal, status, title, posterUrl, score, epTotal, epAired, animeReleaseStatus, animeSeason, animeSeasonYear)
                    bottomSheetDialog.dismiss()
                },
                onRemove = {
                    viewModel.updateStatus(animeId, idMal, null, title, posterUrl, score, epTotal, epAired, animeReleaseStatus, animeSeason, animeSeasonYear)
                    bottomSheetDialog.dismiss()
                },
                onCreateCustomList = {
                    bottomSheetDialog.dismiss()
                    showCreateListDialog(animeId)
                },
                onToggleCustomList = { listId, isAdding ->
                    if (!isAdding) {
                        MaterialAlertDialogBuilder(this@DetailsActivity)
                            .setTitle("Удалить из списка?")
                            .setMessage("Убрать тайтл из этого списка?")
                            .setPositiveButton("Удалить") { _, _ ->
                                viewModel.toggleAnimeInCustomList(listId, animeId, idMal, title, posterUrl, score, epTotal, epAired, animeReleaseStatus, false)
                                bottomSheetDialog.dismiss()
                            }
                            .setNegativeButton("Отмена", null).show()
                    } else {
                        viewModel.toggleAnimeInCustomList(listId, animeId, idMal, title, posterUrl, score, epTotal, epAired, animeReleaseStatus, true)
                        bottomSheetDialog.dismiss()
                    }
                }
            )
        }
        bottomSheetDialog.show()
    }

    private fun showCreateListDialog(currentAnimeId: Int) {
        // Создаем Compose-оболочку
        val composeView = androidx.compose.ui.platform.ComposeView(this)

        // Вставляем ее в классический Android AlertDialog
        val dialog = com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setView(composeView)
            .setBackground(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
            .create()

        composeView.setContent {
            com.example.animouse.ui.compose.CustomListComposeDialog(
                listId = null,
                initialName = "",
                initialColorHex = "#FFFF9800",
                onDismiss = { dialog.dismiss() },
                onSave = { name, hex ->
                    viewModel.createNewCustomList(name, hex, currentAnimeId)
                    android.widget.Toast.makeText(this@DetailsActivity, "Список '$name' создан!", android.widget.Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
            )
        }
        dialog.show()
    }

    private suspend fun showDebugDialog(title: String, message: String) {
        withContext(Dispatchers.Main) {
            MaterialAlertDialogBuilder(this@DetailsActivity)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Понятно", null)
                .show()
        }
    }
}

suspend fun saveImageToGallery(context: Context, url: String) {
    try {
        // 1. Просим Coil отдать нам картинку (он возьмет её из кэша!)
        val request = ImageRequest.Builder(context)
            .data(url)
            .build()
        val result = context.imageLoader.execute(request)

        if (result is SuccessResult) {
            val bitmap = result.drawable.toBitmap()

            // 2. Сохраняем Bitmap в галерею телефона
            val filename = "animouse_screen_${System.currentTimeMillis()}.jpg"
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/AniMouse")
                }
            }

            val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Сохранено в галерею! 📸", Toast.LENGTH_SHORT).show()
                }
                return
            }
        }
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "Не удалось получить картинку", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "Ошибка сохранения!", Toast.LENGTH_SHORT).show()
        }
    }
}