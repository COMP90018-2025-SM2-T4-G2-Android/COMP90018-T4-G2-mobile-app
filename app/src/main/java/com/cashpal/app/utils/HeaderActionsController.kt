package com.cashpal.app.utils

import android.content.Context
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import com.cashpal.app.R

class HeaderActionsController(
    context: Context,
    private val onProfileRequested: () -> Unit = {}
) : SharedPreferences.OnSharedPreferenceChangeListener {

    private val appContext = context.applicationContext
    private val preferences = AppPreferences.getInstance(appContext)

    private var headerView: View? = null
    private var darkModeButton: ImageButton? = null
    private var notificationsButton: ImageButton? = null
    private var profileButton: ImageButton? = null
    private var isBound = false

    fun bind(headerActionsView: View) {
        headerView = headerActionsView
        darkModeButton = headerActionsView.findViewById(R.id.button_header_theme)
        notificationsButton = headerActionsView.findViewById(R.id.button_header_notifications)
        profileButton = headerActionsView.findViewById(R.id.button_header_profile)

        setupListeners()
        refreshDarkModeState()
        refreshNotificationsState()

        preferences.registerListener(this)
        isBound = true
    }

    fun unbind() {
        if (!isBound) return
        preferences.unregisterListener(this)
        headerView = null
        darkModeButton = null
        notificationsButton = null
        profileButton = null
        isBound = false
    }

    private fun setupListeners() {
        darkModeButton?.setOnClickListener {
            val enabled = !preferences.isDarkModeEnabled()
            preferences.setDarkModeEnabled(enabled)
            showToast(
                if (enabled) R.string.header_action_dark_mode_on
                else R.string.header_action_dark_mode_off
            )
        }

        notificationsButton?.setOnClickListener {
            val enabled = !preferences.isNotificationsEnabled()
            preferences.setNotificationsEnabled(enabled)
            showToast(
                if (enabled) R.string.header_action_notifications_on
                else R.string.header_action_notifications_off
            )
        }

        profileButton?.setOnClickListener {
            onProfileRequested()
            showToast(R.string.header_action_profile_open)
        }
    }

    private fun refreshDarkModeState() {
        val enabled = preferences.isDarkModeEnabled()
        darkModeButton?.let { button ->
            val iconColor = if (enabled) color(android.R.color.white)
            else color(R.color.header_action_icon_inactive)
            val backgroundColor = if (enabled) color(R.color.header_action_bg_active)
            else color(R.color.header_action_bg_default)

            button.imageTintList = ColorStateList.valueOf(iconColor)
            ViewCompat.setBackgroundTintList(button, ColorStateList.valueOf(backgroundColor))

            button.contentDescription = headerView?.resources?.getString(
                if (enabled) R.string.cd_header_dark_mode_on else R.string.cd_header_dark_mode_off
            )
        }
    }

    private fun refreshNotificationsState() {
        val enabled = preferences.isNotificationsEnabled()
        notificationsButton?.let { button ->
            val iconColor = if (enabled) color(android.R.color.white)
            else color(R.color.header_action_icon_inactive)
            val backgroundColor = if (enabled) color(R.color.header_action_bg_active)
            else color(R.color.header_action_bg_default)

            button.imageTintList = ColorStateList.valueOf(iconColor)
            ViewCompat.setBackgroundTintList(button, ColorStateList.valueOf(backgroundColor))

            button.contentDescription = headerView?.resources?.getString(
                if (enabled) R.string.cd_header_notifications_on
                else R.string.cd_header_notifications_off
            )
        }
    }

    private fun showToast(messageRes: Int) {
        headerView?.context?.let {
            Toast.makeText(it, messageRes, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            AppPreferences.KEY_DARK_MODE_ENABLED -> refreshDarkModeState()
            AppPreferences.KEY_NOTIFICATIONS_ENABLED -> refreshNotificationsState()
        }
    }

    private fun color(@ColorRes colorRes: Int): Int =
        ContextCompat.getColor(appContext, colorRes)
}
