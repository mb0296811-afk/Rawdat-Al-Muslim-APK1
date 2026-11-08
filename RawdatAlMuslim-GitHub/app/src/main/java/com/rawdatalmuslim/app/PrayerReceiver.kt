package com.rawdatalmuslim.app

import android.app.*
import android.content.*
import android.media.MediaPlayer
import android.os.Build
import android.net.Uri

class PrayerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val name = intent.getStringExtra("prayerName") ?: "الصلاة"
        val audio = intent.getStringExtra("audio") ?: ""
        val muted = intent.getBooleanExtra("muted", false)
        val id = intent.getIntExtra("id", 2000)
        PrayerScheduler.createChannel(context)
        val open = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = if (Build.VERSION.SDK_INT >= 26) {
            Notification.Builder(context, "prayer_adhan")
        } else {
            @Suppress("DEPRECATION") Notification.Builder(context)
        }.setContentTitle("حان وقت $name").setContentText("حي على الصلاة").setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentIntent(open).setAutoCancel(true).build()
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(id, notification)
        if (!muted && audio.isNotBlank()) {
            try {
                val afd = context.assets.openFd(audio)
                val player = MediaPlayer()
                player.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
                player.setOnCompletionListener { it.release() }
                player.setOnErrorListener { mp, _, _ -> mp.release(); true }
                player.prepare()
                player.start()
            } catch (_: Exception) { }
        }
        // Schedule this prayer for the next day after it fires.
        val prefs = context.getSharedPreferences("rawdatalmuslim_native", Context.MODE_PRIVATE)
        val raw = prefs.getString("prayers", null) ?: return
        PrayerScheduler.scheduleAll(context, org.json.JSONArray(raw), prefs.getBoolean("muted", false))
    }
}
