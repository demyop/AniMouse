package com.example.animouse.ui.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.animouse.R
import com.example.animouse.ui.compose.SearchAnimeCard
import com.example.animouse.ui.viewmodel.SearchViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SearchActivity : AppCompatActivity() {

    private val viewModel: SearchViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SearchScreen()
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun SearchScreen() {
        var searchText by remember { mutableStateOf("") }
        val searchResults by viewModel.searchResults.observeAsState(emptyList())
        val isLoading by viewModel.isLoading.observeAsState(false)

        Column(modifier = Modifier.fillMaxSize().background(Color(0xFF121212))) {
            // Тулбар с полем поиска
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF121212))
                    .statusBarsPadding() // 👈 ВОТ ЭТОТ СПАСАТЕЛЬНЫЙ КРУГ
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { finish() }) {
                    Icon(painterResource(R.drawable.ic_arrow_reg), contentDescription = "Назад", tint = Color.White)
                }

                TextField(
                    value = searchText,
                    onValueChange = {
                        searchText = it
                        viewModel.searchAnime(it)
                    },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Название аниме...", color = Color(0xFFAAAAAA)) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    singleLine = true
                )

                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp).padding(end = 12.dp),
                        color = Color(0xFF00BFA5)
                    )
                }
            }

            // Список результатов (Заменил SearchAnimeAdapter!)
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(searchResults) { anime ->
                    // Вычисляем сезон и год
                    var seasonYearText = ""
                    val airedOn = anime.aired_on
                    if (!airedOn.isNullOrEmpty() && airedOn.length >= 7) {
                        val year = airedOn.substring(0, 4)
                        val season = when (airedOn.substring(5, 7).toIntOrNull() ?: 0) {
                            in 1..3 -> "Зима"; in 4..6 -> "Весна"; in 7..9 -> "Лето"; in 10..12 -> "Осень"; else -> ""
                        }
                        seasonYearText = if (season.isNotEmpty()) "$year • $season • " else "$year • "
                    }

                    SearchAnimeCard(
                        anime = anime,
                        seasonYearText = seasonYearText,
                        onClick = {
                            val intent = Intent(this@SearchActivity, DetailsActivity::class.java).apply {
                                putExtra("EXTRA_ID", -1)
                                putExtra("EXTRA_ID_MAL", anime.id)
                                putExtra("EXTRA_TITLE", anime.russian ?: anime.name)
                                putExtra("EXTRA_POSTER", "https://shikimori.one${anime.image?.original}")
                                putExtra("EXTRA_SCORE", ((anime.score?.toFloatOrNull() ?: 0f) * 10).toInt())
                            }
                            startActivity(intent)
                        }
                    )
                }
            }
        }
    }
}