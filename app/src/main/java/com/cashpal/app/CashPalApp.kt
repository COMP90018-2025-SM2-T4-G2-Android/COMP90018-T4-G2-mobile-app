package com.cashpal.app

import android.app.Application
import com.google.android.material.color.DynamicColors
import com.google.firebase.FirebaseApp
import com.cashpal.app.di.ServiceLocator
import com.cashpal.app.utils.UserProfileCache
import android.content.ComponentCallbacks2
import java.io.File
import kotlin.jvm.Volatile

class CashPalApp : Application() {

    @Volatile
    private var isClearingCache = false

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

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) {
            UserProfileCache.clear()
            clearAppCacheAsync()
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        UserProfileCache.clear()
        clearAppCacheAsync()
    }

    private fun clearAppCacheAsync() {
        if (isClearingCache) return
        isClearingCache = true
        Thread {
            try {
                clearDirectory(cacheDir)
                externalCacheDirs?.forEach { dir ->
                    if (dir != null) {
                        clearDirectory(dir)
                    }
                }
            } finally {
                isClearingCache = false
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
