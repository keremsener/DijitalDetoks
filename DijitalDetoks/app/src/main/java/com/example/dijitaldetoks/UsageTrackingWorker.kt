package com.example.dijitaldetoks

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import android.app.usage.UsageStatsManager
import android.app.usage.UsageEvents
import android.util.Log
import android.app.NotificationManager
import android.app.NotificationChannel
import android.os.Build
import androidx.core.app.NotificationCompat

class UsageTrackingWorker(appContext: Context, workerParams: WorkerParameters) :
    Worker(appContext, workerParams) {

    private val TARGET_PACKAGE = "com.instagram.android"
    private val INTERVAL_MINUTES = 1L

    override fun doWork(): Result {
        Log.d("DetoxWorker", "Arka plan izleme görevi başladı. Kontrol Aralığı: ${INTERVAL_MINUTES} dk.")

        val usageStatsManager = applicationContext.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val currentTime = System.currentTimeMillis()
        val lookBackTime = currentTime - (INTERVAL_MINUTES * 60 * 1000)
        val usageEvents = usageStatsManager.queryEvents(lookBackTime, currentTime)
        val event = UsageEvents.Event()

        var wasInstagramActiveInInterval = false

        while (usageEvents.getNextEvent(event)) {
            if (event.packageName == TARGET_PACKAGE) {
                if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                    wasInstagramActiveInInterval = true
                    break
                }
            }
        }

        if (wasInstagramActiveInInterval) {
            val timeToAdd = INTERVAL_MINUTES * 60 * 1000L
            updateUsageTime(timeToAdd)
            Log.d("DetoxWorker", "Instagram aktif bulundu. ${INTERVAL_MINUTES} dakika eklendi.")
        }

        checkAndNotify()
        return Result.success()
    }

    private fun updateUsageTime(timeToAdd: Long) {
        val currentUsedTime = TimeManager.getUsedTime(applicationContext)
        val newUsedTime = currentUsedTime + timeToAdd
        TimeManager.setUsedTime(applicationContext, newUsedTime)
        val minutes = newUsedTime / (60 * 1000)
        Log.d("DetoxWorker", "Yeni Toplam Kullanım Süresi: $minutes dakika.")
    }

    private fun checkAndNotify() {
        val usedTime = TimeManager.getUsedTime(applicationContext)
        val limitTime = TimeManager.getLimitTime(applicationContext)
        val usedMinutes = (usedTime / (60 * 1000)).toInt()
        val limitMinutes = (limitTime / (60 * 1000)).toInt()

        if (usedTime >= limitTime) {
            sendNotification("Süre Doldu!", "30 dakikalık limitinizi aştınız. Instagram şimdi kilitlenecek!", 999)
        } else if (usedMinutes > 0 && usedMinutes % 5 == 0) {
            sendNotification("Uyarı!", "Sana $limitMinutes dakika müddet tanıdık. Kalan: ${limitMinutes - usedMinutes} dakika.", usedMinutes)
        }
    }

    private fun sendNotification(title: String, message: String, id: Int) {
        val channelId = "detox_channel_id"
        val context = applicationContext
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Dijital Detoks Bildirimleri",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Kullanım süresi uyarıları ve kilit bildirimleri"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        notificationManager.notify(id, builder.build())
    }
}
