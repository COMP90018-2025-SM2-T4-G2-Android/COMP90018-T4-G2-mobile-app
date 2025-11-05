package com.cashpal.app.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.VisibleForTesting

/**
 * Central storage for user-facing preferences that should stay in sync
 * across headers and the More tab toggles.
 */
object AppPreferences {

    private const val PREF_NAME = "cashpal_app_prefs"
    const val KEY_DARK_MODE = "pref_dark_mode_enabled"
    const val KEY_NOTIFICATIONS = "pref_notifications_enabled"
    const val KEY_LAST_SELECTED_TAB = "pref_last_selected_tab"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        if (::prefs.isInitialized) return
        prefs = context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun isInitialized(): Boolean = ::prefs.isInitialized

    fun isDarkModeEnabled(): Boolean = prefs.getBoolean(KEY_DARK_MODE, false)

    fun setDarkModeEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DARK_MODE, enabled).apply()
    }

    fun areNotificationsEnabled(): Boolean = prefs.getBoolean(KEY_NOTIFICATIONS, true)

    fun setNotificationsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIFICATIONS, enabled).apply()
    }

    fun getLastSelectedTab(defaultTab: Int): Int =
        prefs.getInt(KEY_LAST_SELECTED_TAB, defaultTab)

    fun setLastSelectedTab(tabId: Int) {
        prefs.edit().putInt(KEY_LAST_SELECTED_TAB, tabId).apply()
    }

    fun registerListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregisterListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.unregisterOnSharedPreferenceChangeListener(listener)
    }

    @VisibleForTesting
    fun clear() {
        if (::prefs.isInitialized) {
            prefs.edit().clear().apply()
        }
    }
}
