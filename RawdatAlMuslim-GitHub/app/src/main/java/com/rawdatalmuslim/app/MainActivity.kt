package com.rawdatalmuslim.app

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.core.content.FileProvider
import org.json.JSONArray
import java.io.File

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

        val settings = web.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.allowFileAccess = true
        settings.allowContentAccess = true
        settings.allowFileAccessFromFileURLs = true
        settings.allowUniversalAccessFromFileURLs = true
        settings.mediaPlaybackRequiresUserGesture = false
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

        // اعتراض الروابط لفتح ملف المصحف PDF بصورة خارجية صحيحة
        web.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: android.webkit.WebResourceRequest?): Boolean {
                val url = request?.url.toString()
                // إذا كان الرابط يخص ملف المصحف أو صيغة pdf
                if (url.contains("quran") || url.endsWith(".pdf")) {
                    try {
                        val fileName = "standard1-quran.pdf"
                        val outFile = File(cacheDir, fileName)
                        if (!outFile.exists()) {
                            assets.open(fileName).use { input ->
                                outFile.outputStream().use { output ->
                                    input.copyTo(output)
                                }
                            }
                        }
                        val uri = FileProvider.getUriForFile(
                            this@MainActivity,
                            "${packageName}.fileprovider",
                            outFile
                        )
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(uri, "application/pdf")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(this@MainActivity, "تعذر فتح المصحف: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                    return true
                }
                return false
            }
        }

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
