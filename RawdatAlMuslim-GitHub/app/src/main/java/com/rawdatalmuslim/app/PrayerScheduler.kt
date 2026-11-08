package com.rawdatalmuslim.app

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import org.json.JSONArray
import java.util.Calendar

object PrayerScheduler {
    private const val CHANNEL = "prayer_adhan"
    private const val PREFS = "rawdatalmuslim_native"
    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= 26) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(NotificationChannel(CHANNEL, "تنبيهات الصلاة", NotificationManager.IMPORTANCE_HIGH))
        }
    }
    fun scheduleAll(context: Context, prayers: JSONArray, muted: Boolean) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().putBoolean("muted", muted).putString("prayers", prayers.toString()).apply()
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        for (i in 0 until prayers.length()) {
            val p = prayers.getJSONObject(i)
            val id = 2000 + i
            val intent = Intent(context, PrayerReceiver::class.java).apply {
                putExtra("prayerName", p.optString("name", "الصلاة"))
                putExtra("audio", p.optString("audio", ""))
                putExtra("muted", muted)
                putExtra("id", id)
            }
            val pi = PendingIntent.getBroadcast(context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            am.cancel(pi)
            if (muted) continue
            val hour12 = p.optInt("hour", 12)
            val minute = p.optInt("minute", 0)
            val period = p.optString("period", "AM")
            val hour24 = (hour12 % 12) + if (period == "PM") 12 else 0
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour24); set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
            }
            if (Build.VERSION.SDK_INT >= 23) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
            else am.setExact(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
        }
    }
    fun rescheduleAfterBoot(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val raw = prefs.getString("prayers", null) ?: return
        scheduleAll(context, JSONArray(raw), prefs.getBoolean("muted", false))
    }
}
