package com.cashpal.app

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.color.DynamicColors
import com.google.firebase.FirebaseApp
import com.cashpal.app.di.ServiceLocator
import com.cashpal.app.utils.UserProfileCache
import android.content.ComponentCallbacks2
import java.io.File
import kotlin.jvm.Volatile
import com.cashpal.app.utils.AppPreferences

class CashPalApp : Application() {

    @Volatile
    private var isClearingCache = false

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

        UserProfileCache.clear()

        clearAppCacheAsync()
    }

    private fun clearAppCacheAsync() {
        Thread {
            runCatching {
                clearDirectory(cacheDir)
                externalCacheDirs?.forEach { dir ->
                    if (dir != null) {
                        clearDirectory(dir)
                    }
                }
            }
        }.apply { isDaemon = true }.start()
    }

    private fun clearDirectory(directory: File?) {
        if (directory == null || !directory.exists()) return

        directory.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                clearDirectory(file)
            }
            file.delete()
        }
    }
}
