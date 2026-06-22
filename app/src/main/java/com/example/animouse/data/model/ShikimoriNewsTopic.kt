package com.example.animouse.data.model

data class ShikimoriNewsTopic(
    val id: Int,
    val topic_title: String,
    val body: String?,
    val html_body: String?, // 👈 ДОБАВЛЯЕМ ЭТО ПОЛЕ: Шикимори сам отрендерит нам HTML!
    val created_at: String,
    val tags: List<ShikiTag>?,
    val linked: ShikiLinkedEntity?
)

data class ShikiTag(
    val name: String
)

data class ShikiLinkedEntity(
    val id: Int, // 👈 ДОБАВЛЯЕМ ЭТУ СТРОЧКУ!
    val image: ShikiLinkedImage?
)

data class ShikiLinkedImage(
    val original: String?
)

// Модель для UI
data class AnimeNews(
    val id: Int,
    val title: String,
    val imageUrl: String?,
    val date: String, // Для главной карусели ("Вчера", "2 ч. назад")
    val fullDate: String, // 👇 Для экрана деталей ("00:00 | ЧЧ.ММ.ГГГГ")
    val tags: List<String>,
    val newsUrl: String,
    val bodyHtml: String, // 👇 Содержимое из "body-inner"
    val sourceUrl: String?, // 👇 Ссылка на внешний источник из класса "source"
    val linkedAnimeIdMal: Int?, // 👇 ID связанного тайтла (idMal)
    val mediaUrls: List<String> // 👇 Все картинки/видео из b-shiki_wall для раздела Медиа
)