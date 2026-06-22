package com.example.animouse.ui.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.animouse.R
import com.example.animouse.data.model.NewsDataHolder
import com.example.animouse.ui.compose.NewsCard
import com.example.animouse.ui.viewmodel.AllNewsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlin.getValue

@AndroidEntryPoint
class AllNewsActivity : AppCompatActivity() {

    private val viewModel: AllNewsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AllNewsScreen()
        }
    }

    @Composable
    fun AllNewsScreen() {
        Column(modifier = Modifier.fillMaxSize().background(Color(0xFF121212))) {
            // ШАПКА
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, top = 8.dp, end = 16.dp, bottom = 8.dp)
                    .statusBarsPadding(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { finish() }) {
                    Icon(painterResource(R.drawable.ic_arrow_reg), contentDescription = "Назад", tint = Color.White)
                }
                Text("Все новости", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }

            // СПИСОК НОВОСТЕЙ
            LazyColumn(
                contentPadding = PaddingValues(bottom = 16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(viewModel.newsList) { index, newsItem ->

                    // Рисуем твою красивую карточку
                    NewsCard(news = newsItem, onClick = {
                        NewsDataHolder.selectedNews = newsItem
                        val intent = Intent(this@AllNewsActivity, NewsDetailsActivity::class.java)
                        startActivity(intent)
                    })

                    // 🪄 МАГИЯ ПАГИНАЦИИ: Если дошли до последнего элемента — грузим еще!
                    if (index == viewModel.newsList.lastIndex && !viewModel.isLoading.value) {
                        LaunchedEffect(Unit) {
                            viewModel.loadNextPage()
                        }
                    }
                }

                // Индикатор загрузки в самом низу списка
                if (viewModel.isLoading.value) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Color(0xFFFF9800))
                        }
                    }
                }
            }
        }
    }
}