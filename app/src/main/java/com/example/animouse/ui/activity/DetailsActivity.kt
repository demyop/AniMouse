package com.example.animouse.ui.activity

import android.os.Bundle
import android.text.Html
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.example.animouse.R
import com.example.animouse.databinding.ActivityDetailsBinding
import com.example.animouse.ui.viewmodel.DetailsViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog

class DetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetailsBinding
    private lateinit var viewModel: DetailsViewModel

    private var isDescriptionExpanded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Скрываем блоки до загрузки
        binding.cardTrailer.visibility = android.view.View.GONE
        binding.textTrailerHeader.visibility = android.view.View.GONE
        binding.textRelatedHeader.visibility = android.view.View.GONE
        binding.recyclerRelated.visibility = android.view.View.GONE

        viewModel = ViewModelProvider(this)[DetailsViewModel::class.java]

        // 1. Извлекаем данные из Интента (включая бэкап-информацию с AniList)

        // Делаем переменную изменяемой (var)
        var currentAnimeId = intent.getIntExtra("EXTRA_ID", -1)
        val idMal = intent.getIntExtra("EXTRA_ID_MAL", -1)
        val animeId = intent.getIntExtra("EXTRA_ID", -1)
        val title = intent.getStringExtra("EXTRA_TITLE") ?: "Без названия"
        val posterUrl = intent.getStringExtra("EXTRA_POSTER")
        val score = intent.getIntExtra("EXTRA_SCORE", 0)

        val descEnglish = intent.getStringExtra("EXTRA_DESC_ENG") ?: "Описание отсутствует"
        val totalEpisodesAniList = intent.getIntExtra("EXTRA_EPISODES_TOTAL", 0)
        val genres = intent.getStringArrayListExtra("EXTRA_GENRES") ?: arrayListOf()

        // 2. Инициализируем стартовый UI данными с AniList
        binding.textTitleLarge.text = title
        binding.textDetailScore.text = if (score > 0) "$score%" else "—"
        binding.textDetailEpisodes.text = "0 / ${if (totalEpisodesAniList > 0) totalEpisodesAniList else "?"} эп."

        Glide.with(this).load(posterUrl).into(binding.imagePosterBig)

        // --- НОВЫЕ КНОПКИ ВЕРХНЕЙ ПАНЕЛИ ---
        binding.btnBack.setOnClickListener { finish() }

        binding.btnMenuOptions.setOnClickListener {
            Toast.makeText(this, "Настройки уведомлений и Поделиться", Toast.LENGTH_SHORT).show()
        }

        // --- НОВАЯ ЛЕНТА ТЕГОВ ---
        setupTags(genres, animeId)

        // 3. Стартовая загрузка
        if (currentAnimeId != -1) {
            viewModel.loadStatus(currentAnimeId)
            viewModel.loadNotes(currentAnimeId)
        }
        viewModel.loadAnimeDetails(idMal)
        viewModel.loadAniListExtra(currentAnimeId, idMal)

        // 4. Умный обработчик ответа от Шикимори
        viewModel.animeDetails.observe(this) { details ->
            if (details != null) {
                // Подменяем название на русское, если оно есть
                val finalTitle = if (!details.russian.isNullOrBlank()) details.russian else title
                binding.textTitleLarge.text = finalTitle

                // Логика эпизодов: Вышло (Шикимори) / Всего (AniList)
                val totalEp = if (totalEpisodesAniList > 0) totalEpisodesAniList else details.episodes
                binding.textDetailEpisodes.text = "${details.episodes_aired} / ${if (totalEp > 0) totalEp else "?"} эп."

                // Проверка синопсиса на валидность
                val rawDescription = details.description
                if (!rawDescription.isNullOrBlank() && rawDescription != "Описание отсутствует.") {
                    // Очищаем BB-коды Шикимори
                    binding.textDescription.text = rawDescription.replace(Regex("\\[.*?\\]"), "")
                } else {
                    // Спасательный круг: ставим английский текст, декорируя HTML-теги AniList
                    setEnglishFallbackDescription(descEnglish)
                }
            }
        }

        // --- Подписка на заметки ---
        viewModel.notes.observe(this) { notesList ->
            if (notesList.isNotEmpty()) {
                // Если есть заметки — делаем иконку закрашенной
                binding.btnNote.setIconResource(R.drawable.ic_pencil_square_sol)
            } else {
                // Если пусто — оставляем контурную
                binding.btnNote.setIconResource(R.drawable.ic_pencil_square_reg)
            }
        }

        // --- Обработчик Трейлеров и Связанных тайтлов ---
        viewModel.aniListExtra.observe(this) { extraData ->
            if (extraData != null) {
                // === МАГИЯ ЗДЕСЬ: Если пришли из поиска, вытаскиваем AniList ID и активируем БД! ===
                val fetchedId = extraData.id
                if (currentAnimeId == -1 && fetchedId != null) {
                    currentAnimeId = fetchedId
                    // Разблокируем локальную базу данных!
                    viewModel.loadStatus(currentAnimeId)
                    viewModel.loadNotes(currentAnimeId)
                }
                // Обрабатываем Трейлер
                val trailer = extraData.trailer
                if (trailer != null && trailer.site == "youtube" && trailer.id != null) {
                    binding.cardTrailer.visibility = android.view.View.VISIBLE
                    binding.textTrailerHeader.visibility = android.view.View.VISIBLE

                    // Грузим превьюшку с серверов YouTube
                    val thumbnailUrl = "https://img.youtube.com/vi/${trailer.id}/maxresdefault.jpg"
                    Glide.with(this).load(thumbnailUrl).into(binding.imageTrailerThumb)

                    // Клик открывает приложение YouTube
                    binding.cardTrailer.setOnClickListener {
                        val intent = android.content.Intent(
                            android.content.Intent.ACTION_VIEW,
                            android.net.Uri.parse("https://www.youtube.com/watch?v=${trailer.id}")
                        )
                        startActivity(intent)
                    }
                }

                // Обрабатываем Связанные тайтлы
                val relations = extraData.relations?.edges?.filter { it.node.title.romaji != null }
                if (!relations.isNullOrEmpty()) {
                    binding.textRelatedHeader.visibility = android.view.View.VISIBLE
                    binding.recyclerRelated.visibility = android.view.View.VISIBLE

                    val adapter = com.example.animouse.ui.adapter.RelatedAnimeAdapter(relations)
                    binding.recyclerRelated.adapter = adapter
                }
            }
        }

        // Если сетевой запрос упал — плавно подставляем бэкап-данные
        viewModel.error.observe(this) { errorMessage ->
            if (errorMessage != null) {
                setEnglishFallbackDescription(descEnglish)
                binding.textDetailEpisodes.text = "? / ${if (totalEpisodesAniList > 0) totalEpisodesAniList else "?"} эп."
            }
        }

        viewModel.currentStatus.observe(this) { status ->
            updateStatusButtonUI(status)
        }

        // 5. Логика кнопки разворачивания синопсиса
        binding.textReadMore.setOnClickListener {
            isDescriptionExpanded = !isDescriptionExpanded
            if (isDescriptionExpanded) {
                binding.textDescription.maxLines = Integer.MAX_VALUE
                binding.textReadMore.text = "Свернуть"
            } else {
                binding.textDescription.maxLines = 5 // В новой верстке мы поставили 5 строк
                binding.textReadMore.text = "Читать далее..."
            }
        }

        binding.btnStatus.setOnClickListener {
            if (currentAnimeId != -1) showBottomSheetDialog(currentAnimeId, title)
            else Toast.makeText(this, "Синхронизация с базой...", Toast.LENGTH_SHORT).show()
        }

        binding.btnNote.setOnClickListener {
            if (currentAnimeId != -1) showNotesBottomSheet(currentAnimeId)
            else Toast.makeText(this, "Синхронизация с базой...", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setEnglishFallbackDescription(htmlDescription: String) {
        val cleanText = Html.fromHtml(htmlDescription, Html.FROM_HTML_MODE_LEGACY).toString().trim()
        binding.textDescription.text = if (cleanText.isNotEmpty()) cleanText else "Описание отсутствует"
    }

    private fun updateStatusButtonUI(status: String?) {
        val colorRes: Int
        val textRes: String
        val textColorRes: Int

        when (status) {
            "WATCHING" -> { textRes = "Смотрю"; colorRes = R.color.turquoise_secondary; textColorRes = R.color.bg_dark_deep }
            "PLANNED" -> { textRes = "В планах"; colorRes = R.color.orange_accent; textColorRes = R.color.bg_dark_deep }
            "COMPLETED" -> { textRes = "Просмотрено"; colorRes = R.color.green_accent; textColorRes = R.color.bg_dark_deep }
            "DROPPED" -> { textRes = "Брошено"; colorRes = R.color.bg_dark_card; textColorRes = R.color.text_primary }
            else -> { textRes = "Добавить в списки"; colorRes = R.color.bg_dark_card; textColorRes = R.color.text_primary }
        }

        binding.btnStatus.text = textRes
        binding.btnStatus.backgroundTintList = android.content.res.ColorStateList.valueOf(
            androidx.core.content.ContextCompat.getColor(this, colorRes)
        )
        binding.btnStatus.setTextColor(androidx.core.content.ContextCompat.getColor(this, textColorRes))
    }

    // Динамически создаем красивые плашки-теги
    private fun setupTags(genres: List<String>, animeId: Int) {
        binding.chipGroupTags.removeAllViews()

        // Сначала добавляем серенький ID
        val idChip = com.google.android.material.chip.Chip(this).apply {
            text = "ID: $animeId"
            setChipBackgroundColorResource(R.color.bg_dark_card)
            setTextColor(getColor(R.color.text_secondary))
            isClickable = false
            chipStrokeWidth = 0f
        }
        binding.chipGroupTags.addView(idChip)

        // Затем добавляем каждый жанр с иконкой
        for (genre in genres) {
            val chip = com.google.android.material.chip.Chip(this).apply {
                text = genre
                setChipBackgroundColorResource(R.color.bg_dark_card)
                setTextColor(getColor(R.color.text_primary))
                setChipIconResource(R.drawable.ic_tag_reg) // Иконка тега!
                chipIconTint = android.content.res.ColorStateList.valueOf(getColor(R.color.turquoise_secondary))
                iconStartPadding = 8f
                chipIconSize = 40f // Размер иконки
                isClickable = false
                chipStrokeWidth = 0f
            }
            binding.chipGroupTags.addView(chip)
        }
    }

    private fun showBottomSheetDialog(animeId: Int, animeTitle: String) {
        try {
            val bottomSheetDialog = BottomSheetDialog(this)
            val sheetView = layoutInflater.inflate(R.layout.bottom_sheet_status, null)
            bottomSheetDialog.setContentView(sheetView)

            val titleView = sheetView.findViewById<android.widget.TextView>(R.id.textSheetTitle)
            titleView.text = animeTitle

            sheetView.findViewById<View>(R.id.btnWatching).setOnClickListener { viewModel.updateStatus(animeId, "WATCHING"); bottomSheetDialog.dismiss() }
            sheetView.findViewById<View>(R.id.btnPlanned).setOnClickListener { viewModel.updateStatus(animeId, "PLANNED"); bottomSheetDialog.dismiss() }
            sheetView.findViewById<View>(R.id.btnCompleted).setOnClickListener { viewModel.updateStatus(animeId, "COMPLETED"); bottomSheetDialog.dismiss() }
            sheetView.findViewById<View>(R.id.btnDropped).setOnClickListener { viewModel.updateStatus(animeId, "DROPPED"); bottomSheetDialog.dismiss() }
            sheetView.findViewById<View>(R.id.btnRemove).setOnClickListener { viewModel.updateStatus(animeId, null); bottomSheetDialog.dismiss() }

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

        // Переменная для хранения заметки, которую мы сейчас редактируем
        var editingNote: com.example.animouse.data.database.NoteEntity? = null

        val adapter = com.example.animouse.ui.adapter.NotesAdapter(
            onEditClick = { note ->
                // При клике на карандашик закидываем текст в поле ввода
                inputNote.setText(note.content)
                editingNote = note
                inputNote.requestFocus()
            },
            onDeleteClick = { note ->
                viewModel.deleteNote(note)
            }
        )
        recyclerNotes.adapter = adapter

        // Подписываемся на обновления именно для этого диалога
        viewModel.notes.observe(this) { notesList ->
            adapter.submitList(notesList)
            textEmptyNotes.visibility = if (notesList.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
            // Если добавили новую заметку — скроллим наверх
            if (notesList.isNotEmpty()) recyclerNotes.scrollToPosition(0)
        }

        btnSendNote.setOnClickListener {
            val text = inputNote.text.toString().trim()
            if (text.isNotEmpty()) {
                if (editingNote != null) {
                    // Обновляем существующую
                    viewModel.addOrUpdateNote(editingNote!!.copy(content = text))
                    editingNote = null
                } else {
                    // Создаем новую
                    viewModel.addOrUpdateNote(
                        com.example.animouse.data.database.NoteEntity(animeId = animeId, content = text)
                    )
                }
                inputNote.text?.clear()
            }
        }

        bottomSheetDialog.show()
    }

}