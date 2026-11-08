package com.rawdatalmuslim.app

import android.Manifest
import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
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
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.settings.allowFileAccess = true
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
                    Toast.makeText(this@MainActivity, "تم تحديث تنبيهات الصلاة", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this@MainActivity, "تعذر ضبط التنبيهات: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onBackPressed() {
        if (::web.isInitialized && web.canGoBack()) web.goBack() else super.onBackPressed()
    }
}
