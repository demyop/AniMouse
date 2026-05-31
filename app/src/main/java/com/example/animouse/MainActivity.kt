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

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        lifecycleScope.launch {

            try {

                val response =
                    AnimeRepository().getAnimeList()

                if (response.isSuccessful) {

                    val animeList =
                        response.body()
                            ?.data
                            ?.Page
                            ?.media
                            ?: emptyList()

                    binding.recyclerAnime.layoutManager =
                        LinearLayoutManager(this@MainActivity)

                    binding.recyclerAnime.adapter =
                        AnimeAdapter(animeList)
                }

            } catch (e: Exception) {

                Log.e(
                    "ANIME",
                    e.message ?: "Unknown error"
                )
            }
        }
    }
}

