package com.cashpal.app.utils

import android.content.Context
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import com.cashpal.app.R

/**
 * Wires the three header action buttons so that they stay in sync with
 * persisted preferences and provide consistent behaviour across screens.
 */
class HeaderActionsController(
    headerRoot: View,
    private val context: Context,
    private val onProfileClick: () -> Unit
) : SharedPreferences.OnSharedPreferenceChangeListener {

    private val darkModeButton: ImageButton =
        headerRoot.findViewById(R.id.button_dark_mode)
    private val notificationsButton: ImageButton =
        headerRoot.findViewById(R.id.button_notifications)
    private val profileButton: ImageButton =
        headerRoot.findViewById(R.id.button_profile)

    init {
        require(AppPreferences.isInitialized()) {
            "AppPreferences must be initialised before binding header actions."
        }

        updateDarkModeButton(AppPreferences.isDarkModeEnabled())
        updateNotificationsButton(AppPreferences.areNotificationsEnabled())
        tintProfileButton()

        darkModeButton.setOnClickListener {
            val enabled = !AppPreferences.isDarkModeEnabled()
            AppPreferences.setDarkModeEnabled(enabled)
            applyDarkMode(enabled)
            showShortToast(
                if (enabled) R.string.dark_mode_enabled_message
                else R.string.dark_mode_disabled_message
            )
        }

        notificationsButton.setOnClickListener {
            val enabled = !AppPreferences.areNotificationsEnabled()
            AppPreferences.setNotificationsEnabled(enabled)
            showShortToast(
                if (enabled) R.string.notifications_enabled_message
                else R.string.notifications_disabled_message
            )
        }

        profileButton.setOnClickListener { onProfileClick() }

        AppPreferences.registerListener(this)
    }

    override fun onSharedPreferenceChanged(
        sharedPreferences: SharedPreferences,
        key: String?
    ) {
        when (key) {
            AppPreferences.KEY_DARK_MODE -> {
                val enabled = AppPreferences.isDarkModeEnabled()
                updateDarkModeButton(enabled)
                applyDarkMode(enabled)
            }

            AppPreferences.KEY_NOTIFICATIONS -> {
                updateNotificationsButton(AppPreferences.areNotificationsEnabled())
            }
        }
    }

    fun detach() {
        AppPreferences.unregisterListener(this)
        darkModeButton.setOnClickListener(null)
        notificationsButton.setOnClickListener(null)
        profileButton.setOnClickListener(null)
    }

    private fun applyDarkMode(enabled: Boolean) {
        val mode = if (enabled) {
            AppCompatDelegate.MODE_NIGHT_YES
        } else {
            AppCompatDelegate.MODE_NIGHT_NO
        }
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    private fun updateDarkModeButton(enabled: Boolean) {
        val tintColor = if (enabled) {
            ContextCompat.getColor(context, android.R.color.white)
        } else {
            ContextCompat.getColor(context, R.color.primary)
        }
        ImageViewCompat.setImageTintList(
            darkModeButton,
            ColorStateList.valueOf(tintColor)
        )
        darkModeButton.alpha = if (enabled) 1f else 0.95f
        darkModeButton.contentDescription = context.getString(
            if (enabled) R.string.accessibility_disable_dark_mode
            else R.string.accessibility_enable_dark_mode
        )
    }

    private fun updateNotificationsButton(enabled: Boolean) {
        val tintColor = if (enabled) {
            ContextCompat.getColor(context, android.R.color.white)
        } else {
            ContextCompat.getColor(context, R.color.muted_foreground)
        }
        ImageViewCompat.setImageTintList(
            notificationsButton,
            ColorStateList.valueOf(tintColor)
        )
        notificationsButton.alpha = if (enabled) 1f else 0.55f
        notificationsButton.contentDescription = context.getString(
            if (enabled) R.string.accessibility_disable_notifications
            else R.string.accessibility_enable_notifications
        )
    }

    private fun tintProfileButton() {
        val tintColor = ContextCompat.getColor(context, android.R.color.white)
        ImageViewCompat.setImageTintList(
            profileButton,
            ColorStateList.valueOf(tintColor)
        )
        profileButton.alpha = 1f
    }

    private fun showShortToast(messageRes: Int) {
        Toast.makeText(context, context.getString(messageRes), Toast.LENGTH_SHORT).show()
    }
}
