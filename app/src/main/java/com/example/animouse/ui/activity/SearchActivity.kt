package com.example.animouse.ui.activity

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.animouse.databinding.ActivitySearchBinding
import com.example.animouse.ui.adapter.SearchAnimeAdapter
import com.example.animouse.ui.viewmodel.SearchViewModel

class SearchActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySearchBinding
    private lateinit var viewModel: SearchViewModel
    private val searchAdapter = SearchAnimeAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[SearchViewModel::class.java]
        binding.recyclerSearch.adapter = searchAdapter

        // Кнопка назад
        binding.btnBack.setOnClickListener { finish() }

        // Фокус на поле ввода и показ клавиатуры
        binding.inputSearch.requestFocus()
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(binding.inputSearch, InputMethodManager.SHOW_IMPLICIT)

        // Слушаем изменения текста
        binding.inputSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.searchAnime(s.toString())
            }
        })

        // Подписываемся на результаты
        viewModel.searchResults.observe(this) { results ->
            searchAdapter.submitList(results)
        }

        // Показываем крутилку загрузки
        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressSearch.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }
}