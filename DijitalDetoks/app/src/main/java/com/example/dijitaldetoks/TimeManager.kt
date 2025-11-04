package com.example.dijitaldetoks

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

object TimeManager {
    private const val PREFS_NAME = "DetoxPrefs"
    private const val KEY_USAGE_MILLIS = "usage_millis"
    private const val KEY_LIMIT_MILLIS = "limit_millis"
    private const val KEY_IS_LOCKED = "is_locked"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, 0)
    }

    fun setLimitTime(context: Context, timeMinutes: Int) {
        val timeMillis = timeMinutes.toLong() * 60L * 1000L
        getPrefs(context).edit { putLong(KEY_LIMIT_MILLIS, timeMillis) }
    }

    fun getUsedTime(context: Context): Long {
        return getPrefs(context).getLong(KEY_USAGE_MILLIS, 0L)
    }

    fun setUsedTime(context: Context, timeMillis: Long) {
        getPrefs(context).edit { putLong(KEY_USAGE_MILLIS, timeMillis) }
    }

    fun getLimitTime(context: Context): Long {
        return getPrefs(context).getLong(KEY_LIMIT_MILLIS, 30L * 60L * 1000L)
    }

    fun resetUsedTime(context: Context) {
        setUsedTime(context, 0L)
    }

    fun isAppLocked(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_IS_LOCKED, false)
    }

    fun setAppLocked(context: Context, isLocked: Boolean) {
        getPrefs(context).edit { putBoolean(KEY_IS_LOCKED, isLocked) }
    }
}
