package com.example.animouse.ui.compose

import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import coil.compose.AsyncImage
import com.example.animouse.R
import com.example.animouse.data.model.Anime
import com.example.animouse.ui.viewmodel.MainViewModel.CustomFolderPreview
import com.example.animouse.data.model.ShikimoriSearchResult
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.animouse.ui.activity.saveImageToGallery
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.animouse.data.model.AnimeNews

// ========================================================
// 1. ВЕРТИКАЛЬНАЯ КАРТОЧКА (С ИДЕАЛЬНОЙ ТИПОГРАФИКОЙ)
// ========================================================
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AnimeCard(
    anime: Anime,
    systemStatusText: String?,
    systemStatusColor: Color,
    customBadges: List<CustomFolderPreview>,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .padding(6.dp)
            .height(240.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
    ) {
        Box(
            modifier = Modifier.fillMaxSize().combinedClickable(onClick = onClick, onLongClick = onLongClick)
        ) {
            AsyncImage(
                model = anime.coverImage.large,
                contentDescription = "Постер",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier.fillMaxWidth().height(160.dp).align(Alignment.BottomCenter)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))))
            )
            LazyRow(modifier = Modifier.padding(8.dp).align(Alignment.TopStart), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (systemStatusText != null) {
                    item { StatusBadge(text = systemStatusText, color = systemStatusColor) }
                }
                items(customBadges) { badge ->
                    StatusBadge(text = badge.name, color = Color(android.graphics.Color.parseColor(badge.colorHex)))
                }
            }

            Column(
                modifier = Modifier.align(Alignment.BottomStart).padding(12.dp),
                verticalArrangement = Arrangement.Bottom
            ) {
                // 1. 👇 УБИВАЕМ "ВИСЯЧИЕ" ЦИФРЫ (Регулярка ищет пробел перед последней цифрой или римской цифрой) 👇
                val rawTitle = anime.title.romaji ?: "Без названия"
                val formattedTitle = remember(rawTitle) {
                    rawTitle.replace(Regex(" (\\d+|[IVX]+)$"), "\u00A0$1")
                }

                // 2. 👇 ФИКСИРУЕМ ВЫСОТУ ТЕКСТА В 2 СТРОКИ 👇
                Text(
                    text = formattedTitle,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(bottom = 2.dp)
                )

                // --- ЛОГИКА СЕРИЙ И СТАТУСА ---
                val totalEp = anime.episodes ?: 0
                val nextEp = anime.nextAiringEpisode?.episode
                val airedEp = if (nextEp != null && nextEp > 0) {
                    nextEp - 1
                } else if (totalEp > 0) {
                    totalEp
                } else {
                    0
                }

                val (statusLabel, seasonYear) = remember(anime) {
                    val status = anime.status?.lowercase() ?: ""
                    val season = anime.season?.uppercase() ?: ""
                    val year = anime.seasonYear

                    val translatedSeason = when (season) {
                        "WINTER" -> "Зима"; "SPRING" -> "Весна"; "SUMMER" -> "Лето"; "FALL" -> "Осень"; else -> ""
                    }
                    val combinedSeasonYear = if (year != null && translatedSeason.isNotEmpty()) "$year $translatedSeason" else if (year != null) "$year" else ""

                    when (status) {
                        "ongoing", "releasing" -> "Онгоинг" to ""
                        "anons", "upcoming" -> "Анонс" to combinedSeasonYear
                        "released", "finished", "completed" -> "Вышло" to combinedSeasonYear
                        else -> "" to ""
                    }
                }

                // 3. 👇 ФИКСИРУЕМ ВЫСОТУ НИЖНЕГО БЛОКА 👇
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp), // 👈 МАГИЯ: Жесткая высота! Блок больше не будет прыгать!
                    verticalAlignment = Alignment.Bottom // Все элементы прижимаются ко дну
                ) {
                    // Рейтинг (оборачиваем в Row, чтобы идеально выровнять звезду и цифру)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 2.dp) // Чуть приподнимаем от самого дна
                    ) {
                        Icon(painterResource(id = R.drawable.ic_star_sol), contentDescription = null, tint = Color(0xFFFF9800), modifier = Modifier.size(14.dp))
                        Text(text = (anime.averageScore ?: 0).toString(), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp))
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Статус по центру
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                        modifier = Modifier.fillMaxHeight() // Колонка занимает все 32dp
                    ) {
                        if (statusLabel.isNotEmpty()) {
                            Text(
                                text = statusLabel,
                                color = Color(0xFFAAAAAA),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (seasonYear.isNotEmpty()) {
                            Text(
                                text = seasonYear,
                                color = Color(0xFFAAAAAA),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Серии
                    Text(
                        text = "$airedEp eps",
                        color = Color(0xFF00BFA5),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 2.dp) // На одном уровне с рейтингом
                    )
                }
            }
        }
    }

}

// ========================================================
// 2. ГОРИЗОНТАЛЬНАЯ КАРТОЧКА (ДЛЯ РАСПИСАНИЯ) + ПРОГРЕСС БАР
// ========================================================
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AnimeListCard(
    anime: Anime,
    customBadges: List<CustomFolderPreview>,
    airingTime: String?,
    isNotificationEnabled: Boolean,
    onNotificationClick: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
    ) {
        Column(
            modifier = Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Левая часть: Постер
                AsyncImage(
                    model = anime.coverImage.large,
                    contentDescription = "Poster",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .width(80.dp)
                        .height(120.dp)
                        .clip(RoundedCornerShape(16.dp))
                )

                // Центральная часть: Информация
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 10.dp, end = 2.dp)
                ) {
                    Text(
                        text = anime.title.romaji ?: "Без названия",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    // --- 📈 ПРОГРЕСС-БАР И СЕРИИ ---
                    val totalEp = anime.episodes ?: 0
                    val nextEp = anime.nextAiringEpisode?.episode

                    val airedEp = if (nextEp != null && nextEp > 0) {
                        nextEp - 1
                    } else if (totalEp > 0) {
                        totalEp
                    } else {
                        0
                    }

                    val progressPct = if (totalEp > 0) airedEp.toFloat() / totalEp.toFloat() else 0f

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .height(3.dp)
                            .clip(RoundedCornerShape(1.5.dp))
                            .background(Color(0xFF2A2A2A))
                    ) {
                        Box(modifier = Modifier.fillMaxWidth(progressPct.coerceIn(0f, 1f)).fillMaxHeight().background(Color(0xFF00BFA5)))
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(painterResource(id = R.drawable.ic_star_sol), contentDescription = null, tint = Color(0xFFFF9800), modifier = Modifier.size(14.dp))
                        Text(
                            text = (anime.averageScore ?: 0).toString(),
                            color = Color(0xFFAAAAAA),
                            fontSize = 13.sp,
                            modifier = Modifier.padding(start = 4.dp)
                        )

                        val episodesString = if (totalEp > 0) " • $airedEp / $totalEp eps" else " • $airedEp / ? eps"

                        Text(
                            text = episodesString,
                            color = Color(0xFF00BFA5),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }

                    if (customBadges.isNotEmpty()) {
                        LazyRow(
                            modifier = Modifier.padding(top = 6.dp, bottom = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(customBadges) { badge ->
                                StatusBadge(text = badge.name, color = Color(android.graphics.Color.parseColor(badge.colorHex)))
                            }
                        }
                    }
                }

                // Правая часть: Колокольчик
                if (airingTime != null) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .padding(4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onNotificationClick() }
                            .padding(8.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = if (isNotificationEnabled) R.drawable.ic_notification_bell_alarm_sol else R.drawable.ic_notification_bell_alarm_reg),
                            contentDescription = "Notification",
                            tint = if (isNotificationEnabled) Color(0xFFFF9800) else Color(0xFFAAAAAA),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(text = airingTime, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 5.dp))
                    }
                }
            }
        }
    }
}

// ========================================================
// 3. МИНИ-КАРТОЧКА ДЛЯ ПОИСКА (По данным Shikimori)
// ========================================================
@Composable
fun SearchAnimeCard(anime: ShikimoriSearchResult, seasonYearText: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = "https://shikimori.one${anime.image?.original}",
            contentDescription = "Постер",
            contentScale = ContentScale.Crop,
            modifier = Modifier.width(60.dp).height(85.dp).clip(RoundedCornerShape(8.dp))
        )

        Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
            Text(text = anime.russian ?: anime.name, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)

            val (statusText, statusColor) = when (anime.status) {
                "ongoing" -> "Онгоинг" to Color(0xFF00BFA5)
                "anons" -> "Анонс" to Color(0xFFFF9800)
                "released" -> "Вышло" to Color(0xFF4CAF50)
                else -> null to Color.Transparent
            }

            if (statusText != null) {
                Box(modifier = Modifier.padding(top = 4.dp)) { StatusBadge(text = statusText, color = statusColor) }
            }

            val scoreText = if (anime.score != "0.0" && anime.score != null) "⭐ ${anime.score}" else "⭐ —"
            val episodesText = "Эп: ${anime.episodes_aired}/${if (anime.episodes > 0) anime.episodes else "?"}"

            Text(text = "$seasonYearText$episodesText • $scoreText", color = Color(0xFFAAAAAA), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

// Переименовали, чтобы не конфликтовал с системным Badge!
@Composable
fun StatusBadge(text: String, color: Color) {
    Box(modifier = Modifier.background(color = color, shape = RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
        Text(text = text, color = Color(0xFF121212), fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ScreenshotViewerOverlay(
    screenshots: List<String>,
    initialIndex: Int,
    onDismiss: () -> Unit,
    context: Context
) {
    val pagerState = rememberPagerState(initialPage = initialIndex) { screenshots.size }
    val coroutineScope = rememberCoroutineScope() // Для запуска сохранения картинки

    // Обрабатываем физическую кнопку "Назад"
    BackHandler(onBack = onDismiss)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black) // Черный фон теперь ТОЧНО залезет под все шторки
            // Блокируем клики, чтобы не нажать на элементы списка позади черного экрана
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {})
    ) {
        // Карусель картинок
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            AsyncImage(
                model = screenshots[page],
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Верхняя панель: Кнопка Назад и Счетчик (Безопасный отступ от чёлки!)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .statusBarsPadding() // 👈 Идеально отступает от статус-бара
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onDismiss) {
                Icon(painterResource(R.drawable.ic_arrow_reg), contentDescription = "Закрыть", tint = Color.White)
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "${pagerState.currentPage + 1} / ${screenshots.size}",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(end = 16.dp)
            )
            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.size(48.dp)) // Баланс для центрирования
        }

        // Кнопка Скачать (Безопасный отступ от полоски жестов!)
        Button(
            onClick = {
                // Запускаем корутину для сохранения
                coroutineScope.launch {
                    saveImageToGallery(context, screenshots[pagerState.currentPage])
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding() // 👈 Идеально отступает от нижней полоски
                .padding(bottom = 24.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Скачать кадр", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}
// ========================================================
// 4. КАРТОЧКА НОВОСТИ (Обложка + Данные снизу)
// ========================================================
@Composable
fun NewsCard(news: AnimeNews, onClick: () -> Unit) {
    // Главный контейнер, который обрабатывает клик по всей области
    Column(
        modifier = Modifier
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        // 1. ВЕРХНЯЯ ЧАСТЬ: Карточка с обложкой и заголовком
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp), // 👈 Высота самой картинки
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
        ) {
            Box(modifier = Modifier.fillMaxSize()) {

                // Отрисовка фона/картинки
                if (news.imageUrl != null) {
                    AsyncImage(
                        model = news.imageUrl,
                        contentDescription = "Обложка новости",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.linearGradient(colors = listOf(Color(0xFF2A2A2A), Color(0xFF1A1A1A))))
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_media_reg),
                            contentDescription = null,
                            tint = Color(0xFF333333),
                            modifier = Modifier.size(64.dp).align(Alignment.Center)
                        )
                    }
                }

                // Темный градиент для читаемости текста (стал пониже, так как текста меньше)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(100.dp)
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))))
                )

                // Только заголовок поверх картинки
                Text(
                    text = news.title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp)
                )
            }
        }

        // 2. НИЖНЯЯ ЧАСТЬ: Дата и Теги (Вынесены под карточку!)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, start = 4.dp, end = 4.dp), // Отступы для визуального баланса
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Слева: Красивая дата
            Text(
                text = news.date,
                color = Color(0xFFAAAAAA),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )

            // Справа: Теги
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                news.tags.forEach { tag ->
                    StatusBadge(text = tag, color = Color(0xFFAAAAAA))
                }
            }
        }
    }
}