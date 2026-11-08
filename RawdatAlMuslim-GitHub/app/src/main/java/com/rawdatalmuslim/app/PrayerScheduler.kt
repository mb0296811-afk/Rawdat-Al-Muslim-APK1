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

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "تنبيهات الأذان"
            val descriptionText = "قناة خاصة بتشغيل أذان الصلاة في وقتها"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel("adhan_channel", name, importance).apply {
                description = descriptionText
                enableVibration(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun scheduleAll(context: Context, jsonArray: JSONArray, muted: Boolean) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        for (i in 0 until jsonArray.length()) {
            try {
                val prayerObj = jsonArray.getJSONObject(i)
                val name = prayerObj.getString("name")
                val hour = prayerObj.getInt("hour")
                val minute = prayerObj.getInt("minute")
                val period = prayerObj.getString("period")
                
                var targetHour = hour
                if (period.equals("PM", ignoreCase = true) && targetHour < 12) targetHour += 12
                if (period.equals("AM", ignoreCase = true) && targetHour == 12) targetHour = 0

                val calendar = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, targetHour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                if (calendar.timeInMillis <= System.currentTimeMillis()) {
                    calendar.add(Calendar.DAY_OF_YEAR, 1)
                }

                val intent = Intent(context, AlarmReceiver::class.java).apply {
                    putExtra("prayer_name", name)
                    putExtra("is_muted", muted)
                }

                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    i,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
                } else {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
