package com.example.animouse

import android.os.Bundle
import android.text.Html
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.animouse.databinding.ActivityDetailsBinding

class DetailsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDetailsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Достаем переданные данные
        val title = intent.getStringExtra("EXTRA_TITLE") ?: ""
        val posterUrl = intent.getStringExtra("EXTRA_POSTER") ?: ""
        val score = intent.getIntExtra("EXTRA_SCORE", 0)
        val genres = intent.getStringArrayListExtra("EXTRA_GENRES") ?: arrayListOf()
        val descriptionHtml = intent.getStringExtra("EXTRA_DESC") ?: "Описание отсутствует"

        // Привязываем к UI
        binding.textDetailsTitle.text = title
        binding.textDetailsRating.text = if (score > 0) "⭐ Рейтинг: $score%" else "⭐ Нет оценки"
        binding.textDetailsGenres.text = genres.joinToString(", ")

        // Декорируем HTML-теги в обычный текст
        binding.textDetailsDescription.text = Html.fromHtml(descriptionHtml, Html.FROM_HTML_MODE_LEGACY)

        Glide.with(this)
            .load(posterUrl)
            .into(binding.imageDetailsPoster)
    }
}