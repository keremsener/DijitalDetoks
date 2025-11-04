package com.example.dijitaldetoks

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.util.Log

class AppBlockerService : AccessibilityService() {

    private val TARGET_PACKAGE = "com.instagram.android"
    private val BLOCK_ACTIVITY_CLASS = BlockActivity::class.java

    override fun onServiceConnected() {
        super.onServiceConnected()
        val info = AccessibilityServiceInfo()
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or AccessibilityEvent.TYPE_WINDOWS_CHANGED
        info.packageNames = null
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
        info.flags = AccessibilityServiceInfo.DEFAULT
        serviceInfo = info
        Log.d("AppBlocker", "Erişilebilirlik Servisi Bağlandı ve Aktif.")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: return
            if (packageName == applicationContext.packageName) return

            if (packageName == TARGET_PACKAGE) {
                Log.d("AppBlocker", "Instagram Algılandı: $packageName")

                val usedTime = TimeManager.getUsedTime(applicationContext)
                val limitTime = TimeManager.getLimitTime(applicationContext)

                if (usedTime >= limitTime) {
                    Log.w("AppBlocker", "Limit Aşıldı! Engelleme Başlatılıyor...")
                    blockApp()
                }
            }
        }
    }

    private fun blockApp() {
        val intent = Intent(this, BlockActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
        }
        startActivity(intent)
    }

    override fun onInterrupt() {
        Log.e("AppBlocker", "Erişilebilirlik Servisi Kesildi.")
    }
}
