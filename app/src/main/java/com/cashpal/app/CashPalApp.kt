package com.cashpal.app

import android.app.Application
import com.google.android.material.color.DynamicColors
import com.google.firebase.FirebaseApp
import com.cashpal.app.di.ServiceLocator
import com.cashpal.app.utils.UserProfileCache
import java.io.File

class CashPalApp : Application() {
    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)
        
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
