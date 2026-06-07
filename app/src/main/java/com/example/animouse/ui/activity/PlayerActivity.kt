package com.example.animouse.ui.activity

import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import com.example.animouse.R

class PlayerActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        val webView = findViewById<WebView>(R.id.webViewPlayer)
        val idMal = intent.getIntExtra("EXTRA_ID_MAL", -1)

        if (idMal != -1) {
            setupWebView(webView)
            // Магическая ссылка Kodik. Автоматически найдет плеер по Shikimori ID
            val playerUrl = "https://kodik.cc/find-player?shikimoriID=$idMal"
            webView.loadUrl(playerUrl)
        }
    }

    private fun setupWebView(webView: WebView) {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true // Обязательно для плееров
            mediaPlaybackRequiresUserGesture = false // Автовоспроизведение
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }

        webView.webViewClient = WebViewClient() // Чтобы ссылки не выкидывали в системный браузер
        webView.webChromeClient = WebChromeClient() // Для поддержки полноэкранного режима
    }

    // Защита: при нажатии "Назад" возвращаемся по истории плеера, если можно
    override fun onBackPressed() {
        val webView = findViewById<WebView>(R.id.webViewPlayer)
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}