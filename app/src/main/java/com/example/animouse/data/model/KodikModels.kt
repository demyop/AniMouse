package com.example.animouse.data.model

data class KodikResponse(
    val results: List<KodikResult>
)

data class KodikResult(
    val link: String, // Ссылка на плеер (именно её мы будем парсить на следующем этапе)
    val translation: KodikTranslation, // Информация об озвучке
    val last_season: Int?,
    val last_episode: Int?,
    val episodes_count: Int?
)

data class KodikTranslation(
    val id: Int,
    val title: String, // Название (например: "AniLibria", "Студийная Банда")
    val type: String?  // voice (озвучка) или subtitles (субтитры)
)