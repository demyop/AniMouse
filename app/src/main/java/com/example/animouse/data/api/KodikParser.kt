package com.example.animouse.data.api

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.regex.Pattern

object KodikParser {

    suspend fun extractDirectLink(playerUrl: String): String? = withContext(Dispatchers.IO) {
        try {
            val validUrl = if (playerUrl.startsWith("//")) "https:$playerUrl" else playerUrl
            val connection = URL(validUrl).openConnection() as HttpURLConnection
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")

            val html = connection.inputStream.bufferedReader().readText()

            // Ищем зашифрованную строку в JS-блоке
            val pattern = Pattern.compile("video_url: '([^']+)'")
            val matcher = pattern.matcher(html)

            if (matcher.find()) {
                val encodedLink = matcher.group(1) ?: return@withContext null

                // Базовая дешифровка (Кодик часто использует Base64 + rot13 или просто Base64)
                return@withContext decodeKodikLink(encodedLink)
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    private fun decodeKodikLink(encoded: String): String {
        // Простой Base64 декодер. Если ссылка все еще выглядит как мусор,
        // значит, тут нужна дополнительная математика замены символов.
        return try {
            val decoded = String(Base64.decode(encoded, Base64.DEFAULT))
            // Если в строке есть путь к m3u8 — возвращаем
            if (decoded.contains("http")) decoded else ""
        } catch (e: Exception) {
            ""
        }
    }
}