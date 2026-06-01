package com.example.animouse

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.util.Log
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.example.animouse.data.repository.AnimeRepository
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.animouse.ui.adapter.AnimeAdapter
import com.example.animouse.databinding.ActivityMainBinding
import androidx.appcompat.app.ActionBarDrawerToggle
import android.widget.Toast
import androidx.room.Room
import com.example.animouse.data.database.AppDatabase
import com.example.animouse.data.database.FavoriteEntity
import com.example.animouse.data.model.Anime

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var database: AppDatabase
    private var favoriteIds = mutableSetOf<Int>()
    private var allAnime = listOf<Anime>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "animouse_db"
        ).build()

        val toggle = ActionBarDrawerToggle(
            this,
            binding.drawerLayout,
            binding.toolbar,
            R.string.app_name,
            R.string.app_name
        )

        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        binding.toolbar.title = "AniMouse | Календарь аниме"

        lifecycleScope.launch {
            try {
                // Сначала загружаем избранное из базы данных
                val savedFavorites = database.favoriteDao().getAll()
                favoriteIds = savedFavorites.map { it.animeId }.toMutableSet()

                val response = AnimeRepository().getAnimeList()
                if (response.isSuccessful) {
                    allAnime = response.body()?.data?.Page?.media ?: emptyList()
                    binding.recyclerAnime.layoutManager = LinearLayoutManager(this@MainActivity)

                    showAnime(allAnime)
                }
            } catch (e: Exception) {
                Log.e("ANIME", e.message ?: "Unknown error")
            }
        }

        binding.navigationView.setNavigationItemSelectedListener {

            when (it.itemId) {

                R.id.menu_home -> {
                    showAnime(allAnime)
                }

                R.id.menu_favorites -> {
                    val favorites = allAnime.filter { favoriteIds.contains(it.id) }
                    showAnime(favorites)
                }

                R.id.menu_about -> {
                    Toast.makeText(
                        this,
                        "AniMouse v1.0",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            binding.drawerLayout.closeDrawers()
            true
        }

    }

    private fun showAnime(animeList: List<Anime>) {
        binding.recyclerAnime.adapter = AnimeAdapter(animeList, favoriteIds) { animeId, isAdding ->
            lifecycleScope.launch {
                if (isAdding) {
                    database.favoriteDao().insert(FavoriteEntity(animeId))
                } else {
                    database.favoriteDao().delete(FavoriteEntity(animeId))
                }
            }
        }
    }
}