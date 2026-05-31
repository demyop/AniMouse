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

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        lifecycleScope.launch {

            try {

                val response =
                    AnimeRepository().getAnimeList()

                if (response.isSuccessful) {

                    response.body()
                        ?.data
                        ?.Page
                        ?.media
                        ?.forEach {

                            Log.d(
                                "ANIME",
                                "${it.title.romaji} | ${it.id}"
                            )
                        }
                }

            } catch (e: Exception) {

                Log.e(
                    "ANIME",
                    e.message ?: "Unknown error"
                )
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}

