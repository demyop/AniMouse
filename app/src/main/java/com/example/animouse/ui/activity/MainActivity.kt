package com.example.animouse.ui.activity

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.animouse.MainViewModel
import com.example.animouse.R
import com.example.animouse.data.model.Anime
import com.example.animouse.databinding.ActivityMainBinding
import com.example.animouse.ui.adapter.AnimeAdapter
import com.example.animouse.ui.adapter.FolderItem
import com.example.animouse.ui.adapter.ListsFolderAdapter
import com.example.animouse.ui.adapter.SchedulePagerAdapter
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.tabs.TabLayoutMediator
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel
    private var tabLayoutMediator: TabLayoutMediator? = null

    private var ongoingSortMethod = 0

    // Переменная для хранения открытой папки (SPA подход)
    private var openedFolderStatus: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[MainViewModel::class.java]

        binding.toolbar.title = "AniMouse | Календарь аниме"
        binding.recyclerAnime.layoutManager = GridLayoutManager(this, 2)

        viewModel.allAnime.observe(this) { refreshUI() }
        viewModel.animeStatuses.observe(this) { refreshUI() }

        binding.toolbar.setOnClickListener {
            Toast.makeText(this, "AniMouse v1.0", Toast.LENGTH_SHORT).show()
        }

        binding.switchFavorites.setOnCheckedChangeListener { _, _ -> refreshUI() }

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            refreshUI(item.itemId)
            true
        }

        binding.toolbar.setOnClickListener {
            Toast.makeText(this, "AniMouse v1.0", Toast.LENGTH_SHORT).show()
        }

        binding.switchFavorites.setOnCheckedChangeListener { _, _ -> refreshUI() }

        // === ВСТАВЛЯЕМ КЛИК ПО ЛУПЕ СЮДА ===
        binding.btnSearch.setOnClickListener {
            val intent = android.content.Intent(this, com.example.animouse.ui.activity.SearchActivity::class.java)
            startActivity(intent)
        }
        // ===================================

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            refreshUI(item.itemId)
            true
        }

        // --- ОБРАБОТКА КНОПКИ "НАЗАД" (Для выхода из папок) ---
        // Системный жест "Назад"
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (openedFolderStatus != null) {
                    closeFolder()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })

        // Стрелочка в Тулбаре
        binding.toolbar.setNavigationOnClickListener {
            if (openedFolderStatus != null) {
                closeFolder()
            }
        }
    }



    override fun onResume() {
        super.onResume()
        viewModel.refreshFavorites()
    }

    private fun refreshUI(itemId: Int = binding.bottomNavigation.selectedItemId) {
        val currentAnime = viewModel.allAnime.value ?: emptyList()
        val statuses = viewModel.animeStatuses.value ?: emptyMap()
        val savedIds = statuses.keys

        // Если мы переключаем вкладку, сбрасываем открытую папку и стрелочку
        if (itemId != R.id.menu_lists) {
            openedFolderStatus = null
            binding.toolbar.navigationIcon = null
        }

        when (itemId) {
            R.id.menu_home -> {
                binding.toolbar.title = "AniMouse | Онгоинги"
                showAnime(currentAnime, savedIds)
            }
            R.id.menu_schedule -> {
                binding.toolbar.title = "AniMouse | Календарь аниме"
                showSchedule(currentAnime, savedIds)
            }
            R.id.menu_lists -> {
                if (openedFolderStatus != null) {
                    // Если папка открыта, перерисовываем её (например, если статус поменялся)
                    val title = when (openedFolderStatus) {
                        "WATCHING" -> "Смотрю"
                        "PLANNED" -> "В планах"
                        "COMPLETED" -> "Просмотрено"
                        "DROPPED" -> "Брошено"
                        else -> "Списки"
                    }
                    openFolder(openedFolderStatus!!, title)
                } else {
                    binding.toolbar.title = "AniMouse | Списки аниме"
                    binding.toolbar.navigationIcon = null // Прячем стрелочку
                    showListsScreen(currentAnime, statuses)
                }
            }
        }
    }

    // --- ЛОГИКА ВЛОЖЕННЫХ ПАПОК ---
    private fun openFolder(statusId: String, title: String) {
        openedFolderStatus = statusId
        binding.toolbar.title = "Списки | $title"
        binding.toolbar.setNavigationIcon(R.drawable.ic_arrow_reg) // Показываем стрелочку

        binding.layoutListsHeader.visibility = View.GONE // Прячем график
        binding.recyclerAnime.layoutManager = GridLayoutManager(this, 2) // Включаем сетку

        val currentAnime = viewModel.allAnime.value ?: emptyList()
        val statuses = viewModel.animeStatuses.value ?: emptyMap()

        // Фильтруем аниме только для этой папки
        val folderAnime = currentAnime.filter { statuses[it.id] == statusId }

        // Используем наш стильный адаптер с бейджами и долгим нажатием
        binding.recyclerAnime.adapter = AnimeAdapter(folderAnime, statuses) { anime, _ ->
            showBottomSheetDialog(anime)
        }
    }

    private fun closeFolder() {
        openedFolderStatus = null
        binding.toolbar.navigationIcon = null
        refreshUI(R.id.menu_lists) // Перерисовываем главный экран списков
    }

    // --- ВЫНЕСЕННОЕ МЕНЮ BOTTOM SHEET ---
    private fun showBottomSheetDialog(anime: Anime) {
        try {
            val bottomSheetDialog = BottomSheetDialog(this@MainActivity)
            val sheetView = layoutInflater.inflate(R.layout.bottom_sheet_status, null)
            bottomSheetDialog.setContentView(sheetView)

            val titleView = sheetView.findViewById<android.widget.TextView>(R.id.textSheetTitle)
            titleView.text = anime.title.romaji

            sheetView.findViewById<View>(R.id.btnWatching).setOnClickListener {
                viewModel.setAnimeStatus(anime.id, "WATCHING")
                bottomSheetDialog.dismiss()
            }
            sheetView.findViewById<View>(R.id.btnPlanned).setOnClickListener {
                viewModel.setAnimeStatus(anime.id, "PLANNED")
                bottomSheetDialog.dismiss()
            }
            sheetView.findViewById<View>(R.id.btnCompleted).setOnClickListener {
                viewModel.setAnimeStatus(anime.id, "COMPLETED")
                bottomSheetDialog.dismiss()
            }
            sheetView.findViewById<View>(R.id.btnDropped).setOnClickListener {
                viewModel.setAnimeStatus(anime.id, "DROPPED")
                bottomSheetDialog.dismiss()
            }
            sheetView.findViewById<View>(R.id.btnRemove).setOnClickListener {
                viewModel.removeAnimeFromLists(anime.id)
                bottomSheetDialog.dismiss()
            }

            bottomSheetDialog.show()
        } catch (e: Exception) {
            Toast.makeText(this@MainActivity, "Ошибка меню: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // ... ОСТАЛЬНЫЕ МЕТОДЫ (showSchedule, showAnime, showListsScreen, getTodayIndex, getDayOfWeekFromTimestamp) ...

    private fun showSchedule(animeList: List<Anime>, savedIds: Set<Int>) {
        // (Твой текущий рабочий код showSchedule)
        binding.recyclerAnime.visibility = View.GONE
        binding.tabLayout.visibility = View.VISIBLE
        binding.layoutFilters.visibility = View.VISIBLE
        binding.viewPager.visibility = View.VISIBLE
        binding.layoutListsHeader.visibility = View.GONE

        binding.layoutFilters.setOnClickListener(null)
        binding.iconSort.setImageResource(R.drawable.ic_filter_funnel_reg)
        binding.textSortTitle.text = "Все тайтлы"

        val daysTitles = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
        val calendar = Calendar.getInstance()
        calendar.firstDayOfWeek = Calendar.MONDAY
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)

        val datesNumbers = mutableListOf<String>()
        for (i in 0..6) {
            datesNumbers.add(calendar.get(Calendar.DAY_OF_MONTH).toString())
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }

        val isFavoritesOnly = binding.switchFavorites.isChecked

        val groupedAnime = List(7) { dayIndex ->
            animeList.filter { anime ->
                val airingAt = anime.nextAiringEpisode?.airingAt
                val isCorrectDay = airingAt != null && getDayOfWeekFromTimestamp(airingAt) == dayIndex
                val passFilter = if (isFavoritesOnly) savedIds.contains(anime.id) else true
                isCorrectDay && passFilter
            }.sortedBy { it.nextAiringEpisode?.airingAt }
        }

        val currentItem = binding.viewPager.currentItem

        binding.viewPager.adapter = SchedulePagerAdapter(groupedAnime, savedIds.toMutableSet()) { animeId, isAdding ->
            if (isAdding) viewModel.setAnimeStatus(animeId, "PLANNED")
            else viewModel.removeAnimeFromLists(animeId)
        }

        tabLayoutMediator?.detach()
        tabLayoutMediator = TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.setCustomView(R.layout.item_tab_day)
            val customView = tab.customView
            val textDayName = customView?.findViewById<android.widget.TextView>(R.id.textDayName)
            val textDayNumber = customView?.findViewById<android.widget.TextView>(R.id.textDayNumber)
            textDayName?.text = daysTitles[position]
            textDayNumber?.text = datesNumbers[position]
        }
        tabLayoutMediator?.attach()

        if (currentItem == 0 && !isFavoritesOnly) {
            binding.viewPager.setCurrentItem(getTodayIndex(), false)
        } else {
            binding.viewPager.setCurrentItem(currentItem, false)
        }
    }

    private fun showAnime(animeList: List<Anime>, savedIds: Set<Int>) {
        binding.tabLayout.visibility = View.GONE
        binding.viewPager.visibility = View.GONE
        binding.recyclerAnime.visibility = View.VISIBLE
        binding.layoutListsHeader.visibility = View.GONE

        binding.recyclerAnime.layoutManager = GridLayoutManager(this, 2)

        binding.layoutFilters.visibility = View.VISIBLE
        binding.iconSort.setImageResource(R.drawable.ic_sort_reg)

        val sortText = when (ongoingSortMethod) {
            0 -> "По популярности ↓"
            1 -> "По популярности ↑"
            2 -> "По эпизодам ↓"
            3 -> "По эпизодам ↑"
            else -> "По популярности ↓"
        }
        binding.textSortTitle.text = sortText

        binding.layoutFilters.setOnClickListener {
            ongoingSortMethod = (ongoingSortMethod + 1) % 4
            refreshUI()
        }

        val isFavoritesOnly = binding.switchFavorites.isChecked

        val processedList = animeList.filter { anime ->
            if (isFavoritesOnly) savedIds.contains(anime.id) else true
        }.let { filteredList ->
            when (ongoingSortMethod) {
                0 -> filteredList.sortedByDescending { it.averageScore ?: 0 }
                1 -> filteredList.sortedBy { it.averageScore ?: 0 }
                2 -> filteredList.sortedByDescending { it.nextAiringEpisode?.episode ?: 0 }
                3 -> filteredList.sortedBy { it.nextAiringEpisode?.episode ?: 0 }
                else -> filteredList.sortedByDescending { it.averageScore ?: 0 }
            }
        }

        val allStatuses = viewModel.animeStatuses.value ?: emptyMap()
        binding.recyclerAnime.adapter = AnimeAdapter(processedList, allStatuses) { anime, _ ->
            // Используем наш вынесенный метод
            showBottomSheetDialog(anime)
        }
    }

    private fun showListsScreen(animeList: List<Anime>, statuses: Map<Int, String>) {
        binding.tabLayout.visibility = View.GONE
        binding.viewPager.visibility = View.GONE
        binding.layoutFilters.visibility = View.GONE
        binding.recyclerAnime.visibility = View.VISIBLE

        binding.layoutListsHeader.visibility = View.VISIBLE

        var countWatching = 0
        var countPlanned = 0
        var countCompleted = 0
        var countDropped = 0

        statuses.values.forEach { status ->
            when (status) {
                "WATCHING" -> countWatching++
                "PLANNED" -> countPlanned++
                "COMPLETED" -> countCompleted++
                "DROPPED" -> countDropped++
            }
        }
        val totalCount = countWatching + countPlanned + countCompleted + countDropped

        binding.textTotalAnime.text = "Всего в списках: $totalCount"
        binding.legendWatching.text = "Смотрю: $countWatching"
        binding.legendPlanned.text = "В планах: $countPlanned"
        binding.legendCompleted.text = "Просмотрено: $countCompleted"
        binding.legendDropped.text = "Брошено: $countDropped"

        binding.layoutChartBar.weightSum = if (totalCount > 0) totalCount.toFloat() else 1f

        fun updateBarWeight(view: View, weight: Int) {
            val params = view.layoutParams as android.widget.LinearLayout.LayoutParams
            params.weight = weight.toFloat()
            view.layoutParams = params
        }

        updateBarWeight(binding.barWatching, countWatching)
        updateBarWeight(binding.barPlanned, countPlanned)
        updateBarWeight(binding.barCompleted, countCompleted)
        updateBarWeight(binding.barDropped, countDropped)

        val watchingAnime = animeList.filter { statuses[it.id] == "WATCHING" }
        val plannedAnime = animeList.filter { statuses[it.id] == "PLANNED" }
        val completedAnime = animeList.filter { statuses[it.id] == "COMPLETED" }
        val droppedAnime = animeList.filter { statuses[it.id] == "DROPPED" }

        val folders = listOf(
            FolderItem(
                "WATCHING", "Смотрю", countWatching,
                watchingAnime.randomOrNull()?.coverImage?.large, R.color.turquoise_secondary
            ),
            FolderItem(
                "PLANNED", "В планах", countPlanned,
                plannedAnime.randomOrNull()?.coverImage?.large, R.color.orange_accent
            ),
            FolderItem(
                "COMPLETED", "Просмотрено", countCompleted,
                completedAnime.randomOrNull()?.coverImage?.large, R.color.green_accent
            ),
            FolderItem(
                "DROPPED", "Брошено", countDropped,
                droppedAnime.randomOrNull()?.coverImage?.large, R.color.bg_dark_deep
            )
        )

        binding.recyclerAnime.layoutManager = LinearLayoutManager(this)

        binding.recyclerAnime.adapter = ListsFolderAdapter(folders) { statusId ->
            val title = folders.find { it.statusId == statusId }?.title ?: "Список"
            openFolder(statusId, title) // ВЫЗЫВАЕМ ОТКРЫТИЕ ПАПКИ
        }
    }

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
            else -> 0
        }
    }

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