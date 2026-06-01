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

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

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

        binding.navigationView.setNavigationItemSelectedListener {

            when (it.itemId) {

                R.id.menu_home -> {
                    Toast.makeText(
                        this,
                        "Онгоинги",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                R.id.menu_favorites -> {
                    Toast.makeText(
                        this,
                        "Избранное",
                        Toast.LENGTH_SHORT
                    ).show()
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
}

