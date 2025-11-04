package com.example.dijitaldetoks

import android.app.*
import android.content.Context
import android.content.Intent
import android.app.usage.UsageStatsManager
import android.app.usage.UsageEvents
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import java.util.concurrent.TimeUnit

class TrackingService : Service() {

    private val TAG = "DetoxTrackerService"
    private val TARGET_PACKAGE = "com.instagram.android"
    private val INTERVAL_CHECK_MS = 10 * 1000L

    private lateinit var usageStatsManager: UsageStatsManager
    private lateinit var handler: Handler
    private lateinit var runnable: Runnable

    override fun onCreate() {
        super.onCreate()
        usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        handler = Handler(Looper.getMainLooper())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(1, createForegroundNotification())
        startTracking()
        return START_STICKY
    }

    private fun startTracking() {
        runnable = object : Runnable {
            override fun run() {
                checkUsageAndNotify()
                handler.postDelayed(this, INTERVAL_CHECK_MS)
            }
        }
        handler.post(runnable)
    }

    private fun sendExitNotification(usedTimeMillis: Long) {
        val usedMinutes = (usedTimeMillis / (60 * 1000L)).toInt()
        val limitMinutes = (TimeManager.getLimitTime(applicationContext) / (60 * 1000L)).toInt()
        val remainingMinutes = limitMinutes - usedMinutes
        val title = "Ayrıldığınız İçin Teşekkürler!"
        val message = "Günlük limitinizin ${usedMinutes} dakikasını kullandınız. Kalan: ${remainingMinutes} dakika."
        sendGeneralNotification(title, message, 1000)
    }

    private fun sendExitNotificationOnce(usedTimeMillis: Long) {
        val prefs = applicationContext.getSharedPreferences("DetoxPrefs", 0)
        val lastSentTime = prefs.getLong("last_exit_notification", 0L)
        val currentTime = System.currentTimeMillis()

        if (currentTime - lastSentTime > 60000) {
            val usedMinutes = (usedTimeMillis / (60 * 1000L)).toInt()
            val limitMinutes = (TimeManager.getLimitTime(applicationContext) / (60 * 1000L)).toInt()
            val remainingMinutes = limitMinutes - usedMinutes
            val title = "Ayrıldığınız İçin Teşekkürler!"
            val message = "Günlük limitinizin ${usedMinutes} dakikasını kullandınız. Kalan: ${remainingMinutes} dakika."
            sendGeneralNotification(title, message, 1000)
            prefs.edit().putLong("last_exit_notification", currentTime).apply()
        }
    }

    private fun checkUsageAndNotify() {
        val currentTime = System.currentTimeMillis()
        val lookBackTime = currentTime - INTERVAL_CHECK_MS
        val usageEvents = usageStatsManager.queryEvents(lookBackTime, currentTime)
        val event = UsageEvents.Event()

        var instagramWasResumed = false
        var instagramWasPaused = false

        while (usageEvents.getNextEvent(event)) {
            if (event.packageName == TARGET_PACKAGE) {
                when (event.eventType) {
                    UsageEvents.Event.ACTIVITY_RESUMED -> instagramWasResumed = true
                    UsageEvents.Event.ACTIVITY_PAUSED -> instagramWasPaused = true
                }
            }
        }

        if (instagramWasResumed) {
            TimeManager.setAppLocked(applicationContext, false)
            updateUsageTime(INTERVAL_CHECK_MS)
        } else if (instagramWasPaused) {
            sendExitNotificationOnce(TimeManager.getUsedTime(applicationContext))
        }

        checkLimitAndNotify()
    }

    private fun updateUsageTime(timeToAdd: Long) {
        val currentUsedTime = TimeManager.getUsedTime(applicationContext)
        val newUsedTime = currentUsedTime + timeToAdd
        TimeManager.setUsedTime(applicationContext, newUsedTime)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(newUsedTime)
        Log.d(TAG, "Yeni Toplam Kullanım Süresi: $minutes dakika.")
        val intent = Intent(Constants.ACTION_USAGE_UPDATE)
        sendBroadcast(intent)
    }

    private fun checkLimitAndNotify() {
        val usedTime = TimeManager.getUsedTime(applicationContext)
        val limitTime = TimeManager.getLimitTime(applicationContext)
        val usedMinutes = (usedTime / (60 * 1000L)).toInt()
        val limitMinutes = (limitTime / (60 * 1000L)).toInt()

        if (usedTime >= limitTime) {
            val isAlreadyLocked = TimeManager.isAppLocked(applicationContext)
            if (!isAlreadyLocked) {
                sendGeneralNotification("SÜRE BİTTİ", "Limitinizi aştınız. Instagram şimdi kilitli.", 999)
                TimeManager.setAppLocked(applicationContext, true)
            }
            Log.w(TAG, "LIMIT AŞILDI. KİLİT DEVREDE.")
            stopSelf()
        } else if (usedMinutes > 0 && usedMinutes % 5 == 0 && (usedTime % (5 * 60 * 1000L)) < INTERVAL_CHECK_MS) {
            sendGeneralNotification("Uyarı!", "Kalan: ${limitMinutes - usedMinutes} dakika.", usedMinutes)
        }
    }

    private fun createForegroundNotification(): Notification {
        val channelId = "foreground_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "Arka Plan İzleme", NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Dijital Detoks Aktif")
            .setContentText("Kullanım süreniz gerçek zamanlı takip ediliyor.")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun sendGeneralNotification(title: String, message: String, id: Int) {
        val channelId = "detox_channel_id"
        val context = applicationContext
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "Uyarı Bildirimleri", NotificationManager.IMPORTANCE_HIGH
            )
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

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        handler.removeCallbacks(runnable)
        super.onDestroy()
        Log.d(TAG, "Tracking Service Durduruldu.")
    }
}
