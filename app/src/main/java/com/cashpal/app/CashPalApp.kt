package com.cashpal.app

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.color.DynamicColors
import com.google.firebase.FirebaseApp
import com.cashpal.app.di.ServiceLocator
import com.cashpal.app.utils.UserProfileCache
import java.io.File
import com.cashpal.app.utils.AppPreferences

class CashPalApp : Application() {
    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)

        AppPreferences.init(this)
        AppCompatDelegate.setDefaultNightMode(
            if (AppPreferences.isDarkModeEnabled()) {
                AppCompatDelegate.MODE_NIGHT_YES
            } else {
                AppCompatDelegate.MODE_NIGHT_NO
            }
        )
        
        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        
        // Initialize Service Locator
        ServiceLocator.initialize(this)
    }
}


