package com.rawdatalmuslim.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.PowerManager

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prayerName = intent.getStringExtra("prayer_name") ?: "الصلاة"
        val isMuted = intent.getBooleanExtra("is_muted", false)

        // إيقاظ الشاشة عند حلول الأذان
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.FULL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.ON_AFTER_RELEASE,
            "RawdatAlMuslim:AdhanWakeLock"
        )
        wakeLock.acquire(30000L)

        // تشغيل صوت الأذان إذا لم يكن التطبيق في وضع الصامت
        if (!isMuted) {
            try {
                // يمكنك ربطه بملف الصوت الافتراضي أو تشغيل رنين التنبيه
                val mediaPlayer = MediaPlayer.create(context, android.provider.Settings.System.DEFAULT_RINGTONE_URI)
                mediaPlayer.start()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
