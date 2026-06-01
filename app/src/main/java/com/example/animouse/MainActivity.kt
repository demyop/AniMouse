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
import com.example.animouse.ui.adapter.SchedulePagerAdapter
import java.util.Calendar
import com.google.android.material.tabs.TabLayoutMediator

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

        // 2. Обработка нажатия на название (Toolbar) для окна "О приложении":
        binding.toolbar.setOnClickListener {
            Toast.makeText(this, "AniMouse v1.0", Toast.LENGTH_SHORT).show()
        }

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.menu_home -> {
                    showAnime(allAnime)
                    true
                }
                R.id.menu_schedule -> {
                    showSchedule()
                    true
                }

                R.id.menu_favorites -> {
                    // Убедитесь, что favoriteIds берутся из локальной базы/Room (переменная из предыдущего шага)
                    val favorites = allAnime.filter { favoriteIds.contains(it.id) }
                    showAnime(favorites)
                    true
                }
                else -> false
            }
        }

    }

    private fun showSchedule() {
        // Переключаем видимость: скрываем обычный список, показываем вкладки
        binding.recyclerAnime.visibility = android.view.View.GONE
        binding.tabLayout.visibility = android.view.View.VISIBLE
        binding.viewPager.visibility = android.view.View.VISIBLE

        val daysTitles = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")

        // Распределяем все полученные аниме по 7 спискам в зависимости от дня недели
        val groupedAnime = List(7) { dayIndex ->
            allAnime.filter { anime ->
                val airingAt = anime.nextAiringEpisode?.airingAt
                airingAt != null && getDayOfWeekFromTimestamp(airingAt) == dayIndex
            }
        }

        // Инициализируем адаптер страниц
        binding.viewPager.adapter =
            SchedulePagerAdapter(groupedAnime, favoriteIds) { animeId, isAdding ->
                lifecycleScope.launch {
                    if (isAdding) {
                        database.favoriteDao().insert(FavoriteEntity(animeId))
                    } else {
                        database.favoriteDao().delete(FavoriteEntity(animeId))
                    }
                }
            }

        // Связываем TabLayout и ViewPager2 вместе
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = daysTitles[position]
        }.attach()

        // Открываем вкладку "Сегодня" (smoothScroll = false, чтобы открылось мгновенно без анимации)
        binding.viewPager.setCurrentItem(getTodayIndex(), false)
    }

    private fun showAnime(animeList: List<Anime>) {
        // Возвращаем видимость стандартного списка
        binding.tabLayout.visibility = android.view.View.GONE
        binding.viewPager.visibility = android.view.View.GONE
        binding.recyclerAnime.visibility = android.view.View.VISIBLE

        binding.recyclerAnime.adapter = AnimeAdapter(animeList, favoriteIds) { animeId, isAdding ->
            // Здесь ваша логика сохранения/удаления из Room
        }
    }
    // Возвращает индекс текущего дня (Понедельник = 0, Вторник = 1 ... Воскресенье = 6)
    private fun getTodayIndex(): Int {
        val dayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        return when (dayOfWeek) {
            Calendar.MONDAY -> 0
            Calendar.TUESDAY -> 1
            Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY -> 3
            Calendar.FRIDAY -> 4
            Calendar.SATURDAY -> 5
            Calendar.SUNDAY -> 6
            else -> 0 // На случай непредвиденного поведения
        }
    }

    // Извлекает день недели из Unix-таймстампа (airingAt) от API
    private fun getDayOfWeekFromTimestamp(timestampSec: Long): Int {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestampSec * 1000
        return when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 0
            Calendar.TUESDAY -> 1
            Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY -> 3
            Calendar.FRIDAY -> 4
            Calendar.SATURDAY -> 5
            Calendar.SUNDAY -> 6
            else -> 0
        }
    }
}