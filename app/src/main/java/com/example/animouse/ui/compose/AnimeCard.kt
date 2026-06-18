package com.example.animouse.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
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

// ========================================================
// 1. ВЕРТИКАЛЬНАЯ КАРТОЧКА (ДЛЯ ГЛАВНОГО ЭКРАНА И ПАПОК)
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
            .padding(8.dp)
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
                    item { Badge(text = systemStatusText, color = systemStatusColor) }
                }
                items(customBadges) { badge ->
                    Badge(text = badge.name, color = Color(android.graphics.Color.parseColor(badge.colorHex)))
                }
            }
            Column(modifier = Modifier.align(Alignment.BottomStart).padding(12.dp)) {
                Text(text = anime.title.romaji ?: "Без названия", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(bottom = 6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(painterResource(id = R.drawable.ic_star_sol), contentDescription = null, tint = Color(0xFFFF9800), modifier = Modifier.size(14.dp))
                    Text(text = (anime.averageScore ?: 0).toString(), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp))
                    Spacer(modifier = Modifier.weight(1f))
                    Text(text = "${(anime.nextAiringEpisode?.episode ?: 1) - 1} eps", color = Color(0xFF00BFA5), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ========================================================
// 2. ГОРИЗОНТАЛЬНАЯ КАРТОЧКА (ДЛЯ РАСПИСАНИЯ И ПОИСКА)
// ========================================================
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AnimeListCard(
    anime: Anime,
    customBadges: List<CustomFolderPreview>,
    airingTime: String?, // Например "17:30". Если передадим null - колонка скроется (как в Поиске)
    isNotificationEnabled: Boolean,
    onNotificationClick: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(onClick = onClick, onLongClick = onLongClick)
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
                    .clip(RoundedCornerShape(16.dp)) // Округление самого постера
            )

            // Центральная часть: Информация
            Column(
                modifier = Modifier
                    .weight(1f) // Заставляет колонку занять всё свободное место в центре
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

                // Тот самый 1dp разделитель!
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 6.dp),
                    thickness = 1.dp,
                    color = Color(0x339CA3AF)
                )

// Оценка и эпизоды
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_star_sol),
                        contentDescription = null,
                        tint = Color(0xFFFF9800),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = (anime.averageScore ?: 0).toString(),
                        color = Color(0xFFAAAAAA), // Цвет text_secondary
                        fontSize = 13.sp,
                        modifier = Modifier.padding(start = 4.dp)
                    )

                    // ✅ НОВАЯ УМНАЯ ЛОГИКА ЭПИЗОДОВ:
                    val nextEp = anime.nextAiringEpisode?.episode
                    val totalEp = if ((anime.episodes ?: 0) > 0) anime.episodes.toString() else "?"
                    val episodesString = if (nextEp != null) {
                        " • ${nextEp - 1} / $totalEp eps"
                    } else {
                        " • ? / $totalEp eps"
                    }

                    Text(
                        text = episodesString,
                        color = Color(0xFF00BFA5),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                // Плашки
                if (customBadges.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier.padding(top = 6.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(customBadges) { badge ->
                            Badge(text = badge.name, color = Color(android.graphics.Color.parseColor(badge.colorHex)))
                        }
                    }
                }
            }

            // Правая часть: Колокольчик и время выхода
            if (airingTime != null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .padding(4.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onNotificationClick() }
                        .padding(8.dp) // Зона клика вокруг колокольчика
                ) {
                    Icon(
                        painter = painterResource(
                            id = if (isNotificationEnabled) R.drawable.ic_notification_bell_alarm_sol else R.drawable.ic_notification_bell_alarm_reg
                        ),
                        contentDescription = "Notification",
                        tint = if (isNotificationEnabled) Color(0xFFFF9800) else Color(0xFFAAAAAA),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = airingTime,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 5.dp)
                    )
                }
            }
        }
    }
}

// ========================================================
// 3. МИНИ-КАРТОЧКА ДЛЯ ПОИСКА (По данным Shikimori)
// ========================================================
@Composable
fun SearchAnimeCard(
    anime: ShikimoriSearchResult,
    seasonYearText: String,
    onClick: () -> Unit
) {
    // В поиске у нас просто кликабельный ряд, без фона карточки
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Постер
        AsyncImage(
            model = "https://shikimori.one${anime.image?.original}",
            contentDescription = "Постер",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .width(60.dp)
                .height(85.dp)
                .clip(RoundedCornerShape(8.dp))
        )

        // Информация
        Column(
            modifier = Modifier
                .padding(start = 12.dp)
                .weight(1f)
        ) {
            // Название
            Text(
                text = anime.russian ?: anime.name,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Статус (Онгоинг / Вышло)
            val (statusText, statusColor) = when (anime.status) {
                "ongoing" -> "Онгоинг" to Color(0xFF00BFA5)
                "anons" -> "Анонс" to Color(0xFFFF9800)
                "released" -> "Вышло" to Color(0xFF4CAF50)
                else -> null to Color.Transparent
            }

            if (statusText != null) {
                Box(modifier = Modifier.padding(top = 4.dp)) {
                    Badge(text = statusText, color = statusColor)
                }
            }

            // Подзаголовок (Год, Сезон, Эпизоды, Оценка)
            val scoreText = if (anime.score != "0.0" && anime.score != null) "⭐ ${anime.score}" else "⭐ —"
            val episodesText = "Эп: ${anime.episodes_aired}/${if (anime.episodes > 0) anime.episodes else "?"}"

            Text(
                text = "$seasonYearText$episodesText • $scoreText",
                color = Color(0xFFAAAAAA),
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}


// Вспомогательный элемент плашки
@Composable
fun Badge(text: String, color: Color) {
    Box(
        modifier = Modifier
            .background(color = color, shape = RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(text = text, color = Color(0xFF121212), fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}