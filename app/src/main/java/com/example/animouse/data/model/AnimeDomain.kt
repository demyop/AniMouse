data class AnimeDomain(
    val id: Int,
    val title: String, // Всегда русское (или оригинал, если русское пустое)
    val posterUrl: String?
)