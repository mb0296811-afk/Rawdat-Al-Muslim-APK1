package com.rawdatalmuslim.app

import android.Manifest
import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.content.pm.PackageManager
import android.widget.Toast
import org.json.JSONArray

class MainActivity : Activity() {
    private lateinit var web: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PrayerScheduler.createChannel(this)
        
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 501)
        }

        web = WebView(this)
        
        // --- إعدادات الـ WebView لضمان عمل الصوت والمصحف والملفات المحلية ---
        val settings = web.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.allowFileAccess = true
        settings.allowContentAccess = true
        settings.allowFileAccessFromFileURLs = true
        settings.allowUniversalAccessFromFileURLs = true
        settings.mediaPlaybackRequiresUserGesture = false // السماح بتشغيل الأذان والصوت تلقائياً بدون لمس الشاشة
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        // -------------------------------------------------------------

        web.webViewClient = WebViewClient()
        web.webChromeClient = WebChromeClient()
        web.addJavascriptInterface(AndroidPrayerBridge(), "AndroidPrayer")
        
        setContentView(web)
        web.loadUrl("file:///android_asset/index.html")
    }

    inner class AndroidPrayerBridge {
        @JavascriptInterface
        fun syncPrayerData(json: String, ignored: Boolean, muted: Boolean) {
            runOnUiThread {
                try {
                    PrayerScheduler.scheduleAll(this@MainActivity, JSONArray(json), muted)
                    Toast.makeText(this@MainActivity, "تم تحديث تنبيهات الصلاة بنجاح", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this@MainActivity, "تعذر ضبط التنبيهات: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onBackPressed() {
        if (::web.isInitialized && web.canGoBack()) {
            web.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
