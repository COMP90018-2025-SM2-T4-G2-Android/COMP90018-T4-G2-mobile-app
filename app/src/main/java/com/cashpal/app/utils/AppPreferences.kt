package com.cashpal.app.utils

import android.content.Context
import android.content.SharedPreferences
import kotlin.jvm.Volatile

class AppPreferences private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isDarkModeEnabled(): Boolean = prefs.getBoolean(KEY_DARK_MODE_ENABLED, false)

    fun setDarkModeEnabled(enabled: Boolean) {
        if (enabled == isDarkModeEnabled()) return
        prefs.edit().putBoolean(KEY_DARK_MODE_ENABLED, enabled).apply()
    }

    fun isNotificationsEnabled(): Boolean = prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true)

    fun setNotificationsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled).apply()
    }

    fun registerListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregisterListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.unregisterOnSharedPreferenceChangeListener(listener)
    }

    companion object {
        private const val PREFS_NAME = "cashpal_app_prefs"
        const val KEY_DARK_MODE_ENABLED = "pref_dark_mode_enabled"
        const val KEY_NOTIFICATIONS_ENABLED = "pref_notifications_enabled"

        @Volatile
        private var INSTANCE: AppPreferences? = null

        fun init(context: Context) {
            if (INSTANCE == null) {
                synchronized(this) {
                    if (INSTANCE == null) {
                        INSTANCE = AppPreferences(context.applicationContext)
                    }
                }
            }
        }

        fun getInstance(context: Context? = null): AppPreferences {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: context?.let {
                    AppPreferences(it.applicationContext).also { prefs -> INSTANCE = prefs }
                } ?: throw IllegalStateException("AppPreferences must be initialized")
            }
        }
    }
}
