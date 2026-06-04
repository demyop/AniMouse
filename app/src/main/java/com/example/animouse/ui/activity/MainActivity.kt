package com.example.animouse.ui.activity

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.animouse.ui.activity.MainViewModel
import com.example.animouse.R
import com.example.animouse.data.model.Anime
import com.example.animouse.databinding.ActivityMainBinding
import com.example.animouse.ui.adapter.AnimeAdapter
import com.example.animouse.ui.adapter.FolderItem
import com.example.animouse.ui.adapter.ListsFolderAdapter
import com.example.animouse.ui.adapter.SchedulePagerAdapter
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.launch
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
        binding.btnNotesHub.setOnClickListener {
            val intent = android.content.Intent(this, com.example.animouse.ui.activity.NotesHubActivity::class.java)
            startActivity(intent)
        }

        viewModel = ViewModelProvider(this)[MainViewModel::class.java]

        binding.toolbar.title = "AniMouse | Календарь аниме"
        binding.recyclerAnime.layoutManager = GridLayoutManager(this, 2)
// Перерисовываем экран, как только база посчитает кастомные папки и плашки
        viewModel.customFolderPreviews.observe(this) { refreshUI() }
        viewModel.animeCustomBadges.observe(this) { refreshUI() }
        viewModel.allAnime.observe(this) { refreshUI() }
        viewModel.animeStatuses.observe(this) { refreshUI() }
        viewModel.localAnime.observe(this) { refreshUI() } // Перерисовываем UI при обновлении кэша базы

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
                binding.btnSearch.visibility = View.VISIBLE    // Показываем лупу
                binding.btnNotesHub.visibility = View.GONE     // Прячем Хаб
                binding.toolbar.title = "AniMouse | Онгоинги"
                showAnime(currentAnime, savedIds)
            }
            R.id.menu_schedule -> {
                binding.btnSearch.visibility = View.VISIBLE
                binding.btnNotesHub.visibility = View.GONE
                binding.toolbar.title = "AniMouse | Календарь аниме"
                showSchedule(currentAnime, savedIds)
            }
            R.id.menu_lists -> {
                binding.btnSearch.visibility = View.GONE       // Прячем лупу
                binding.btnNotesHub.visibility = View.VISIBLE  // Показываем закладку Хаба

                if (openedFolderStatus != null) {
                    // (Твой текущий код для открытой папки...)
                    val title = when (openedFolderStatus) {
                        "WATCHING" -> "Смотрю"
// ... остальное не трогай
                        "PLANNED" -> "В планах"
                        "COMPLETED" -> "Просмотрено"
                        "DROPPED" -> "Брошено"
                        else -> {
                            // Если это кастомная папка, вытаскиваем её имя из списка
                            val listId = openedFolderStatus!!.removePrefix("custom_").toIntOrNull()
                            viewModel.customFolderPreviews.value?.find { it.id == listId }?.name ?: "Список"
                        }
                    }
                    openFolder(openedFolderStatus!!, title)
                } else {
                    binding.toolbar.title = "AniMouse | Списки аниме"
                    binding.toolbar.navigationIcon = null
                    showListsScreen(currentAnime, statuses)
                }
            }
        }
    }

    // --- ЛОГИКА ВЛОЖЕННЫХ ПАПОК ---
    private fun openFolder(statusId: String, title: String) {
        if (statusId == "separator") return // Защита от клика по разделителю

        openedFolderStatus = statusId
        binding.toolbar.title = "Списки | $title"
        binding.toolbar.setNavigationIcon(R.drawable.ic_arrow_reg)

        binding.layoutListsHeader.visibility = View.GONE
        binding.recyclerAnime.layoutManager = GridLayoutManager(this, 2)

        val localAnimeList = viewModel.localAnime.value ?: emptyList()
        val statuses = viewModel.animeStatuses.value ?: emptyMap()

        // НОВОЕ: Читаем из шпаргалки для бейджиков
        val customBadges = viewModel.animeCustomBadges.value ?: emptyMap()

        // Умная фильтрация
        val folderAnime = if (statusId.startsWith("custom_")) {
            val listId = statusId.removePrefix("custom_").toInt()
            val customMap = viewModel.customFolderAnime.value ?: emptyMap()
            customMap[listId]?.map { mapEntityToAnime(it) } ?: emptyList()
        } else {
            localAnimeList.filter { it.status == statusId }.map { mapEntityToAnime(it) }
        }

// Прячем кастомные плашки внутри папок, передавая emptyMap()
        binding.recyclerAnime.adapter = AnimeAdapter(folderAnime, statuses, emptyMap()) { anime, _ ->
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

                // === НОВЫЙ ФИЛЬТР ПО КОЛОКОЛЬЧИКАМ ===
                val passFilter = if (isFavoritesOnly) {
                    com.example.animouse.data.NotificationHelper.isNotificationEnabled(this, anime.id)
                } else true

                isCorrectDay && passFilter
            }.sortedBy { it.nextAiringEpisode?.airingAt }
        }

        val currentItem = binding.viewPager.currentItem

// Теперь адаптер сам разбирается с колокольчиками, передаем ему только список аниме!
        binding.viewPager.adapter = SchedulePagerAdapter(groupedAnime)

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
        // Вытаскиваем шпаргалку по плашкам из вьюмодели:
        val customBadges = viewModel.animeCustomBadges.value ?: emptyMap()

        // Передаем customBadges третьим параметром
        binding.recyclerAnime.adapter = AnimeAdapter(processedList, allStatuses, customBadges) { anime, _ ->
            showBottomSheetDialog(anime)
        }

    }

    private fun showListsScreen(animeList: List<Anime>, statuses: Map<Int, String>) {
        binding.tabLayout.visibility = View.GONE
        binding.viewPager.visibility = View.GONE
        binding.layoutFilters.visibility = View.GONE
        binding.recyclerAnime.visibility = View.VISIBLE

        binding.layoutListsHeader.visibility = View.VISIBLE

        binding.layoutListsHeader.visibility = View.VISIBLE

        // Клик по кнопке на плашке хаба вызывает наш RGB колорпикер
        binding.layoutListsHeader.findViewById<View>(R.id.btnCreateListFromHub).setOnClickListener {
            showCustomListManageDialog()
        }

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

        // Конвертируем ВСЕ локальные данные для превьюшек папок
        val allLocalAsAnime = viewModel.localAnime.value?.map { mapEntityToAnime(it) } ?: emptyList()

        val watchingAnime = allLocalAsAnime.filter { statuses[it.id] == "WATCHING" }
        val plannedAnime = allLocalAsAnime.filter { statuses[it.id] == "PLANNED" }
        val completedAnime = allLocalAsAnime.filter { statuses[it.id] == "COMPLETED" }
        val droppedAnime = allLocalAsAnime.filter { statuses[it.id] == "DROPPED" }

        // 1. Создаем изменяемый список и кладем туда системные папки
        val allFolders = mutableListOf(
            FolderItem("WATCHING", "Смотрю", countWatching, watchingAnime.randomOrNull()?.coverImage?.large, R.color.turquoise_secondary),
            FolderItem("PLANNED", "В планах", countPlanned, plannedAnime.randomOrNull()?.coverImage?.large, R.color.orange_accent),
            FolderItem("COMPLETED", "Просмотрено", countCompleted, completedAnime.randomOrNull()?.coverImage?.large, R.color.green_accent),
            FolderItem("DROPPED", "Брошено", countDropped, droppedAnime.randomOrNull()?.coverImage?.large, R.color.bg_dark_deep)
        )

        // 2. Достаем кастомные папки из ViewModel и добавляем их следом
        val customPreviews = viewModel.customFolderPreviews.value ?: emptyList()

        if (customPreviews.isNotEmpty()) {
            allFolders.add(FolderItem("separator", "Пользовательские списки", 0, null, isSeparator = true))
        }
        for (preview in customPreviews) {
            allFolders.add(
                FolderItem(
                    id = "custom_${preview.id}", // Маркируем кастомные папки префиксом
                    title = preview.name,
                    count = preview.count,
                    posterUrl = preview.randomPosterUrl,
                    indicatorColorHex = preview.colorHex // Передаем HEX цвет
                )
            )
        }

        binding.recyclerAnime.layoutManager = LinearLayoutManager(this)

        // Передаем список, обработчик клика и НОВЫЙ обработчик лонг-клика
        binding.recyclerAnime.adapter = ListsFolderAdapter(allFolders,
            onFolderClick = { folderId ->
                val title = allFolders.find { it.id == folderId }?.title ?: "Список"
                openFolder(folderId, title)
            },
            onFolderLongClick = { folderId ->
                // Системные папки трогать нельзя
                if (folderId == "WATCHING" || folderId == "PLANNED" || folderId == "COMPLETED" || folderId == "DROPPED" || folderId == "separator") return@ListsFolderAdapter

                val listId = folderId.removePrefix("custom_").toIntOrNull() ?: return@ListsFolderAdapter
                val currentFolder = customPreviews.find { it.id == listId } ?: return@ListsFolderAdapter

                // Показываем микро-меню выбора действий
                com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                    .setTitle("Управление списком")
                    .setMessage("Что вы хотите сделать со списком «${currentFolder.name}»?")
                    .setPositiveButton("Редактировать") { _, _ ->
                        showCustomListManageDialog(currentFolder.id, currentFolder.name, currentFolder.colorHex)
                    }
                    .setNegativeButton("Удалить") { _, _ ->
                        // Подтверждение удаления
                        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                            .setTitle("Удалить полностью?")
                            .setMessage("Вы уверены? Сам список удалится, но тайтлы останутся в общей базе.")
                            .setPositiveButton("Да, удалить") { _, _ ->
                                viewModel.deleteCustomList(currentFolder.id)
                                Toast.makeText(this, "Список удален", Toast.LENGTH_SHORT).show()
                            }
                            .setNegativeButton("Отмена", null)
                            .show()
                    }
                    .setNeutralButton("Отмена", null)
                    .show()
            }
        )
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

    private fun mapEntityToAnime(entity: com.example.animouse.data.database.UserAnimeEntity): Anime {
        return Anime(
            id = entity.animeId,
            idMal = entity.idMal,
            title = com.example.animouse.data.model.Title(romaji = entity.title),
            coverImage = com.example.animouse.data.model.CoverImage(large = entity.posterUrl ?: ""),
            averageScore = entity.score,

            // 1. Берем общее количество из Шикимори (которое мы сохранили в БД)
            episodes = entity.episodesTotal,
            description = "Данные из оффлайн-списка",
            genres = emptyList(),
            status = entity.animeStatus,

            // 2. Имитируем ответ AniList, чтобы твой адаптер смог прочитать вышедшие серии
            // (Адаптеры обычно берут nextAiringEpisode.episode - 1 для расчета текущей серии)
            nextAiringEpisode = com.example.animouse.data.model.NextAiringEpisode(
                airingAt = 0,
                timeUntilAiring = 0,
                episode = entity.episodesAired + 1
            )
        )
    }

    private fun showCustomListManageDialog(listId: Int? = null, currentName: String? = null, currentColorHex: String? = null) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_create_list, null)
        val alertDialog = com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setBackground(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
            .create()

        val textTitle = dialogView.findViewById<android.widget.TextView>(R.id.textDialogTitle)
        val inputName = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.inputListName)
        val viewPreview = dialogView.findViewById<View>(R.id.viewColorPreview)
        val textHex = dialogView.findViewById<android.widget.TextView>(R.id.textColorHex)

        val seekRed = dialogView.findViewById<android.widget.SeekBar>(R.id.seekRed)
        val seekGreen = dialogView.findViewById<android.widget.SeekBar>(R.id.seekGreen)
        val seekBlue = dialogView.findViewById<android.widget.SeekBar>(R.id.seekBlue)

        // Если прилетели данные — значит мы в режиме РЕАКТИРОВАНИЯ
        if (listId != null) {
            textTitle.text = "Настройка списка"
            inputName.setText(currentName)
            textHex.text = currentColorHex
            try {
                val parsed = android.graphics.Color.parseColor(currentColorHex)
                seekRed.progress = android.graphics.Color.red(parsed)
                seekGreen.progress = android.graphics.Color.green(parsed)
                seekBlue.progress = android.graphics.Color.blue(parsed)
                viewPreview.backgroundTintList = android.content.res.ColorStateList.valueOf(parsed)
            } catch (e: Exception) { /* На случай кривого HEX */ }
        }

        // Логика динамического изменения цвета ползунками
        val rgbListener = object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                val r = seekRed.progress
                val g = seekGreen.progress
                val b = seekBlue.progress
                val computedColor = android.graphics.Color.rgb(r, g, b)

                viewPreview.backgroundTintList = android.content.res.ColorStateList.valueOf(computedColor)
                textHex.text = String.format("#%02X%02X%02X", r, g, b)
            }
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
        }

        seekRed.setOnSeekBarChangeListener(rgbListener)
        seekGreen.setOnSeekBarChangeListener(rgbListener)
        seekBlue.setOnSeekBarChangeListener(rgbListener)

        dialogView.findViewById<View>(R.id.btnCancelList).setOnClickListener { alertDialog.dismiss() }

        dialogView.findViewById<View>(R.id.btnSaveList).setOnClickListener {
            val name = inputName.text.toString().trim()
            val hex = textHex.text.toString()

            if (name.isNotEmpty()) {
                if (listId != null) {
                    // Обновляем старый
                    viewModel.updateCustomList(listId, name, hex)
                    Toast.makeText(this, "Список обновлен", Toast.LENGTH_SHORT).show()
                } else {
                    // Создаем новый (для этого вызовем старый метод из БД через CrossRef)
                    // Но чтобы не дублировать код, выполним прямую вставку:
                    val database = androidx.room.Room.databaseBuilder(applicationContext, com.example.animouse.data.database.AppDatabase::class.java, "animouse_db").build()
                    kotlinx.coroutines.MainScope().launch(kotlinx.coroutines.Dispatchers.IO) {
                        database.customListDao().insertList(com.example.animouse.data.database.CustomListEntity(name = name, colorHex = hex))
                        viewModel.updateLocalStatuses()
                    }
                    Toast.makeText(this, "Список '$name' создан", Toast.LENGTH_SHORT).show()
                }
                alertDialog.dismiss()
            } else {
                inputName.error = "Введите название"
            }
        }
        alertDialog.show()
    }

}