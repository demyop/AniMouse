package com.example.animouse.ui.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.HtmlCompat
import android.widget.TextView
import coil.compose.AsyncImage
import com.example.animouse.R
import com.example.animouse.data.api.ShikimoriApi
import com.example.animouse.data.model.AnimeNews
import com.example.animouse.data.model.ShikimoriAnimeDetails
import com.example.animouse.data.model.ShikimoriSearchResult
import com.example.animouse.ui.compose.SearchAnimeCard
import com.example.animouse.ui.compose.StatusBadge
import com.example.animouse.ui.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject


@AndroidEntryPoint
class NewsDetailsActivity : AppCompatActivity() {


    @Inject
    lateinit var shikiApi: ShikimoriApi

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 👇 ДОСТАЕМ НОВОСТЬ ИЗ ХОЛДЕРА
        val news = com.example.animouse.data.model.NewsDataHolder.selectedNews
        if (news == null) {
            finish() // Если данных почему-то нет, закрываем экран
            return
        }

        setContent {
            NewsDetailsScreen(news)
        }
    }

    @Composable
    fun NewsDetailsScreen(news: AnimeNews) {
        var linkedAnime by remember { mutableStateOf<ShikimoriAnimeDetails?>(null) }

        // Загружаем данные связанного тайтла для шапки
        LaunchedEffect(news.linkedAnimeIdMal) {
            if (news.linkedAnimeIdMal != null) {
                try {
                    val details = withContext(Dispatchers.IO) {
                        shikiApi.getAnimeDetails(news.linkedAnimeIdMal)
                    }
                    linkedAnime = details
                } catch (e: Exception) {
                    // Игнорируем ошибки сети, карточка просто не покажется
                }
            }
        }

        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF121212))) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp) // Место для кнопок внизу
            ) {
                // 1. НАЗАД И ШАПКА СВЯЗАННОГО ТАЙТЛА
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 8.dp, top = 8.dp, end = 16.dp).statusBarsPadding(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { finish() }) {
                            Icon(painterResource(R.drawable.ic_arrow_reg), contentDescription = "Назад", tint = Color.White) // 👈 Поставишь свою ic_arrow_reg
                        }
                        Text("Новостной хаб", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Связанный тайтл в стиле Поиска (из твоего же AnimeCard.kt)
                if (linkedAnime != null) {
                    item {
                        val searchResult = ShikimoriSearchResult(
                            id = linkedAnime!!.id,
                            name = linkedAnime!!.name ?: "",
                            russian = linkedAnime!!.russian,
                            score = linkedAnime!!.score,
                            episodes = linkedAnime!!.episodes,
                            episodes_aired = linkedAnime!!.episodes_aired,
                            status = linkedAnime!!.status,
                            image = linkedAnime!!.image, // 👈 Никаких переделок! Твой SearchAnimeCard сам всё сделает правильно
                            aired_on = linkedAnime!!.aired_on
                        )

                        Box(
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .background(Color(0xFF1E1E1E), RoundedCornerShape(12.dp))
                        ) {
                            SearchAnimeCard(
                                anime = searchResult,
                                seasonYearText = linkedAnime!!.aired_on?.substringBefore("-") ?: "",
                                onClick = {
                                    // Переход на детали этого аниме
                                    val intent = Intent(this@NewsDetailsActivity, DetailsActivity::class.java).apply {
                                        putExtra("EXTRA_ID", linkedAnime!!.id)
                                        putExtra("EXTRA_ID_MAL", linkedAnime!!.id)
                                        putExtra("EXTRA_TITLE", linkedAnime!!.russian ?: linkedAnime!!.name)
                                        putExtra("EXTRA_POSTER", "https://shikimori.one" + linkedAnime!!.image?.original)
                                        putExtra("EXTRA_SCORE", linkedAnime!!.score?.toFloatOrNull() ?: 0f)
                                        putExtra("EXTRA_EPISODES_TOTAL", linkedAnime!!.episodes)
                                    }
                                    startActivity(intent)
                                }
                            )
                        }
                    }
                }

                // 2. ЗАГОЛОВОК НОВОСТИ
                item {
                    Text(
                        text = news.title,
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }

                // 3. СОДЕРЖИМОЕ (body-inner) с поддержкой HTML и цитат
                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        AndroidView(
                            factory = { context ->
                                TextView(context).apply {
                                    setTextColor(android.graphics.Color.WHITE)
                                    textSize = 15f
                                    // Включаем кликабельность ссылок внутри новости!
                                    linksClickable = true
                                    movementMethod = android.text.method.LinkMovementMethod.getInstance()
                                }
                            },
                            update = { textView ->
                                // Избавляемся от лишних тегов цитат, заменяя их на отступы, или отдаем HtmlCompat
                                val rawHtml = news.bodyHtml
                                    .replace("<div class=\"b-quote\"><div class=\"quote-content\">", "<br><i>« ")
                                    .replace("</div></div>", " »</i><br>")
                                textView.text = HtmlCompat.fromHtml(rawHtml, HtmlCompat.FROM_HTML_MODE_LEGACY)
                            }
                        )
                    }
                }

                // 4. РАЗДЕЛ МЕДИА (Если картинок в ленте b-shiki_wall больше 0)
                if (news.mediaUrls.isNotEmpty()) {
                    item {
                        Text(
                            text = "Медиа материалы:",
                            color = Color(0xFFFF9800),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 8.dp)
                        )
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(news.mediaUrls) { url ->
                                AsyncImage(
                                    model = url,
                                    contentDescription = "Медиа",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .width(240.dp)
                                        .height(140.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF1E1E1E))
                                        .clickable {
                                            // При клике открываем картинку во весь экран в браузере
                                            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                        }
                                )
                            }
                        }
                    }
                }

                // 5. ДАТА И ТЕГИ
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, top = 24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = news.fullDate,
                            color = Color(0xFFAAAAAA),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            news.tags.forEach { tag ->
                                StatusBadge(text = tag, color = Color(0xFF1E1E1E))
                            }
                        }
                    }
                }
            }

            // 6. НИЖНЯЯ ПАНЕЛЬ С КНОПКАМИ (Плавающая поверх контента)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color(0xFF121212))
                    .navigationBarsPadding()
                    .padding(16.dp)
            ) {
                HorizontalDivider(color = Color(0xFF2A2A2A), modifier = Modifier.padding(bottom = 12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Кнопка Шикимори (Оранжевая)
                    Button(
                        onClick = { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(news.newsUrl))) },
                        modifier = Modifier.weight(1f).height(46.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Шикимори", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    // Кнопка Источник (Бирюзовая)
                    Button(
                        onClick = {
                            if (news.sourceUrl != null) {
                                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(news.sourceUrl)))
                            }
                        },
                        enabled = news.sourceUrl != null, // Неактивна, если источника нет
                        modifier = Modifier.weight(1f).height(46.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00BFA5),
                            disabledContainerColor = Color(0xFF2A2A2A)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (news.sourceUrl != null) "Источник" else "Нет источника",
                            color = if (news.sourceUrl != null) Color.White else Color(0xFF757575),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}