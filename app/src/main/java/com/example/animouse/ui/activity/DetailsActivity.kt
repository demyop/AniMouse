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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.animouse.R
import com.example.animouse.data.NotificationHelper
import com.example.animouse.data.api.KodikParser
import com.example.animouse.data.database.CustomListEntity
import com.example.animouse.data.database.NoteEntity
import com.example.animouse.ui.viewmodel.DetailsViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class DetailsActivity : AppCompatActivity() {

    private val viewModel: DetailsViewModel by viewModels()
    private var exoPlayer: ExoPlayer? = null

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

        // 2. Инициализируем плеер
        if (idMal != -1) {
            exoPlayer = ExoPlayer.Builder(this).build()
            lifecycleScope.launch {
                try {
                    val playerPageUrl = "https://kodik.site/find-player?shikimoriID=$idMal"
                    val directVideoUrl = KodikParser.extractDirectLink(playerPageUrl)

                    if (directVideoUrl != null) {
                        val dataSourceFactory = DefaultHttpDataSource.Factory()
                            .setDefaultRequestProperties(mapOf("Referer" to "https://kodik.site/"))
                        val mediaSource = DefaultMediaSourceFactory(this@DetailsActivity)
                            .setDataSourceFactory(dataSourceFactory)
                            .createMediaSource(MediaItem.fromUri(directVideoUrl))
                        exoPlayer?.setMediaSource(mediaSource)
                        exoPlayer?.prepare()
                        exoPlayer?.playWhenReady = true
                    } else {
                        showDebugDialog("Парсер не справился 🕵️‍♂️", "Кодик спрятал ссылку.")
                    }
                } catch (e: Exception) {
                    showDebugDialog("Блокировка провайдером 🛑", "ВКЛЮЧИ VPN! Ошибка: ${e.message}")
                }
            }
        }

        // 3. Загружаем данные ViewModel
        if (currentAnimeId != -1) {
            viewModel.loadStatus(currentAnimeId)
            viewModel.loadNotes(currentAnimeId)
            viewModel.loadCustomListsData(currentAnimeId)
        }
        viewModel.loadAnimeDetails(idMal)
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

    override fun onDestroy() {
        super.onDestroy()
        exoPlayer?.release()
        exoPlayer = null
    }

    // =========================================================================
    // МАГИЯ COMPOSE НАЧИНАЕТСЯ ЗДЕСЬ
    // =========================================================================
    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    fun DetailsScreen() {
        val scrollState = rememberScrollState()

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

        // Обновляем тайтл, если пришел с Шикимори
        val displayTitle = animeDetails?.russian?.takeIf { it.isNotBlank() } ?: currentTitle

        // Логика эпизодов
        val totalEp = if (totalEpisodesAniList > 0) totalEpisodesAniList else (animeDetails?.episodes ?: 0)
        val airedEp = animeDetails?.episodes_aired ?: 0

        // Логика описания
        val rawDesc = animeDetails?.description
        val displayDesc = if (!rawDesc.isNullOrBlank() && rawDesc != "Описание отсутствует.") {
            rawDesc.replace(Regex("\\[.*?\\]"), "") // Убираем BB-коды
        } else {
            Html.fromHtml(descEnglish, Html.FROM_HTML_MODE_LEGACY).toString().trim().ifEmpty { "Описание отсутствует" }
        }

        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF121212))) {

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

                // 2. ИНФОРМАЦИОННЫЙ БЛОК
                Column(modifier = Modifier.padding(horizontal = 20.dp).offset(y = (-40).dp)) {
                    Text(
                        text = displayTitle,
                        color = Color.White,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        style = TextStyle(shadow = Shadow(color = Color(0x80000000), offset = Offset(0f, 4f), blurRadius = 8f))
                    )

                    Row(
                        modifier = Modifier.padding(top = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(painterResource(R.drawable.ic_star_sol), null, tint = Color(0xFFFF9800), modifier = Modifier.size(18.dp))
                        Text(if (score > 0) "$score%" else "—", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 6.dp))
                        Text("|", color = Color(0xFF9CA3AF), fontSize = 16.sp, modifier = Modifier.padding(horizontal = 12.dp))
                        Text("$airedEp / ${if (totalEp > 0) totalEp else "?"} эп.", color = Color.White, fontSize = 15.sp)
                    }

                    // Плашка статуса выхода
                    val shikiStatus = animeDetails?.status
                    if (shikiStatus != null) {
                        val aniSeason = aniListExtra?.season
                        val aniYear = aniListExtra?.seasonYear
                        val translatedSeason = when (aniSeason?.uppercase()) {
                            "WINTER" -> "Зима"; "SPRING" -> "Весна"; "SUMMER" -> "Лето"; "FALL" -> "Осень"; else -> ""
                        }
                        val seasonSuffix = if (aniYear != null && translatedSeason.isNotEmpty()) " • $aniYear $translatedSeason" else ""

                        val (statusText, bgColor) = when (shikiStatus.lowercase()) {
                            "ongoing", "releasing" -> "Онгоинг" to Color(0xFF00BFA5)
                            "anons", "upcoming" -> "Анонс$seasonSuffix" to Color(0xFFFF9800)
                            "released", "finished" -> "Вышло$seasonSuffix" to Color(0xFF4CAF50)
                            else -> "" to Color.Transparent
                        }

                        if (statusText.isNotEmpty()) {
                            Box(modifier = Modifier.padding(top = 8.dp).background(bgColor, RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                                Text(statusText, color = Color(0xFF121212), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // 3. ТЕГИ (FlowRow сам переносит элементы на новую строку)
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

                    for (genre in genres) {
                        DetailBadge(genre, Color(0xFF1E1E1E), Color.White)
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
                        onClick = { showNotesBottomSheet(currentAnimeId) },
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

                // 7. ПЛЕЕР (Интеграция XML ExoPlayer в Compose)
                if (idMal != -1) {
                    Text("Смотреть", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 20.dp, top = 32.dp))
                    Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp).fillMaxWidth().height(220.dp).clip(RoundedCornerShape(16.dp))) {
                        AndroidView(
                            factory = { context ->
                                PlayerView(context).apply { player = exoPlayer }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Row(modifier = Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(painterResource(R.drawable.ic_notification_bell_alarm_sol), null, tint = Color(0xFFAAAAAA), modifier = Modifier.size(16.dp))
                        Text(" Плеер стороннего сервиса. Реклама не связана с AniMouse.", color = Color(0xFFAAAAAA), fontSize = 10.sp, modifier = Modifier.padding(start = 6.dp))
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
        }
    }

    // Компонент тега
    @Composable
    fun DetailBadge(text: String, bgColor: Color, textColor: Color) {
        Box(modifier = Modifier.background(bgColor, RoundedCornerShape(8.dp)).padding(horizontal = 12.dp, vertical = 6.dp)) {
            Text(text, color = textColor, fontSize = 14.sp)
        }
    }

    // =========================================================================
    // СТАРЫЕ XML ФУНКЦИИ (Bottom Sheets). ИХ НЕ ТРОГАЛИ!
    // =========================================================================

    private fun showBottomSheetDialog(animeId: Int, idMal: Int, title: String, posterUrl: String?, score: Int, epTotalAniList: Int) {
        try {
            val bottomSheetDialog = BottomSheetDialog(this)
            val sheetView = layoutInflater.inflate(R.layout.bottom_sheet_status, null)
            bottomSheetDialog.setContentView(sheetView)

            val titleView = sheetView.findViewById<android.widget.TextView>(R.id.textSheetTitle)
            titleView.text = title

            val currentDetails = viewModel.animeDetails.value
            val epAired = currentDetails?.episodes_aired ?: 0
            val epTotal = if (epTotalAniList > 0) epTotalAniList else (currentDetails?.episodes ?: 0)
            val animeReleaseStatus = currentDetails?.status
            val extraData = viewModel.aniListExtra.value
            val animeSeason = extraData?.season
            val animeSeasonYear = extraData?.seasonYear

            fun saveWithStatus(newStatus: String?) {
                viewModel.updateStatus(animeId, idMal, newStatus, title, posterUrl, score, epTotal, epAired, animeReleaseStatus, animeSeason, animeSeasonYear)
                bottomSheetDialog.dismiss()
            }

            sheetView.findViewById<View>(R.id.btnWatching).setOnClickListener { saveWithStatus("WATCHING") }
            sheetView.findViewById<View>(R.id.btnPlanned).setOnClickListener { saveWithStatus("PLANNED") }
            sheetView.findViewById<View>(R.id.btnCompleted).setOnClickListener { saveWithStatus("COMPLETED") }
            sheetView.findViewById<View>(R.id.btnDropped).setOnClickListener { saveWithStatus("DROPPED") }
            sheetView.findViewById<View>(R.id.btnRemove).setOnClickListener { saveWithStatus(null) }

            sheetView.findViewById<View>(R.id.btnCreateCustomList).setOnClickListener {
                bottomSheetDialog.dismiss()
                showCreateListDialog(animeId)
            }

            val container = sheetView.findViewById<android.widget.LinearLayout>(R.id.layoutCustomListsContainer)

            viewModel.allCustomLists.observe(this) { allLists ->
                val activeIds = viewModel.activeCustomListIds.value ?: emptyList()
                container.removeAllViews()

                for (list in allLists) {
                    val itemView = layoutInflater.inflate(R.layout.item_custom_list_option, container, false)
                    val indicator = itemView.findViewById<View>(R.id.indicatorListColor)
                    val textName = itemView.findViewById<android.widget.TextView>(R.id.textCustomListName)
                    val iconCheck = itemView.findViewById<android.widget.ImageView>(R.id.iconCheck)

                    textName.text = list.name
                    val parsedColor = android.graphics.Color.parseColor(list.colorHex)
                    indicator.backgroundTintList = android.content.res.ColorStateList.valueOf(parsedColor)

                    val isAlreadyInList = activeIds.contains(list.id)

                    if (isAlreadyInList) {
                        iconCheck.visibility = android.view.View.VISIBLE
                        iconCheck.imageTintList = android.content.res.ColorStateList.valueOf(parsedColor)
                        textName.setTextColor(parsedColor)
                    } else {
                        iconCheck.visibility = android.view.View.GONE
                        textName.setTextColor(getColor(R.color.text_primary))
                    }

                    itemView.setOnClickListener {
                        if (isAlreadyInList) {
                            MaterialAlertDialogBuilder(this)
                                .setTitle("Удалить из списка?")
                                .setMessage("Вы уверены, что хотите убрать тайтл из списка «${list.name}»?")
                                .setPositiveButton("Удалить") { _, _ -> viewModel.toggleAnimeInCustomList(list.id, animeId, idMal, title, posterUrl, score, epTotal, epAired, animeReleaseStatus, false) }
                                .setNegativeButton("Отмена", null).show()
                        } else {
                            viewModel.toggleAnimeInCustomList(list.id, animeId, idMal, title, posterUrl, score, epTotal, epAired, animeReleaseStatus, true)
                        }
                    }
                    container.addView(itemView)
                }
            }
            bottomSheetDialog.show()
        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showNotesBottomSheet(animeId: Int) {
        val bottomSheetDialog = BottomSheetDialog(this)
        val sheetView = layoutInflater.inflate(R.layout.bottom_sheet_notes, null)
        bottomSheetDialog.setContentView(sheetView)

        val recyclerNotes = sheetView.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recyclerNotes)
        val inputNote = sheetView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.inputNote)
        val btnSendNote = sheetView.findViewById<android.widget.ImageButton>(R.id.btnSendNote)
        val textEmptyNotes = sheetView.findViewById<android.widget.TextView>(R.id.textEmptyNotes)

        var editingNote: NoteEntity? = null

        val adapter = com.example.animouse.ui.adapter.NotesAdapter(
            onEditClick = { note ->
                inputNote.setText(note.content)
                editingNote = note
                inputNote.requestFocus()
            },
            onDeleteClick = { note -> viewModel.deleteNote(note) }
        )
        recyclerNotes.adapter = adapter

        viewModel.notes.observe(this) { notesList ->
            adapter.submitList(notesList)
            textEmptyNotes.visibility = if (notesList.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
            if (notesList.isNotEmpty()) recyclerNotes.scrollToPosition(0)
        }

        btnSendNote.setOnClickListener {
            val text = inputNote.text.toString().trim()
            if (text.isNotEmpty()) {
                if (editingNote != null) {
                    viewModel.addOrUpdateNote(editingNote!!.copy(content = text))
                    editingNote = null
                } else {
                    viewModel.addOrUpdateNote(NoteEntity(animeId = animeId, content = text))
                }
                inputNote.text?.clear()
            }
        }
        bottomSheetDialog.show()
    }

    private fun showCreateListDialog(currentAnimeId: Int) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_create_list, null)
        val alertDialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setBackground(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
            .create()

        val inputName = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.inputListName)
        val viewPreview = dialogView.findViewById<View>(R.id.viewColorPreview)
        val textHex = dialogView.findViewById<android.widget.TextView>(R.id.textColorHex)

        val seekRed = dialogView.findViewById<android.widget.SeekBar>(R.id.seekRed)
        val seekGreen = dialogView.findViewById<android.widget.SeekBar>(R.id.seekGreen)
        val seekBlue = dialogView.findViewById<android.widget.SeekBar>(R.id.seekBlue)

        val rgbListener = object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                val computedColor = android.graphics.Color.rgb(seekRed.progress, seekGreen.progress, seekBlue.progress)
                viewPreview.backgroundTintList = android.content.res.ColorStateList.valueOf(computedColor)
                textHex.text = String.format("#%02X%02X%02X", seekRed.progress, seekGreen.progress, seekBlue.progress)
            }
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
        }

        seekRed.setOnSeekBarChangeListener(rgbListener)
        seekGreen.setOnSeekBarChangeListener(rgbListener)
        seekBlue.setOnSeekBarChangeListener(rgbListener)

        dialogView.findViewById<View>(R.id.btnCancelList).setOnClickListener { alertDialog.dismiss() }
        dialogView.findViewById<View>(R.id.btnSaveList).setOnClickListener {
            val listName = inputName.text.toString().trim()
            if (listName.isNotEmpty()) {
                viewModel.createNewCustomList(listName, textHex.text.toString(), currentAnimeId)
                Toast.makeText(this, "Список '$listName' создан!", Toast.LENGTH_SHORT).show()
                alertDialog.dismiss()
            } else inputName.error = "Введите название"
        }
        alertDialog.show()
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