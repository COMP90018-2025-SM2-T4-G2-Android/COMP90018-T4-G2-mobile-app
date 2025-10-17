package com.cashpal.app

import android.app.Application
import com.google.android.material.color.DynamicColors
import com.google.firebase.FirebaseApp
import com.cashpal.app.di.ServiceLocator

class CashPalApp : Application() {
    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)
        
        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        
        // Initialize Service Locator
        ServiceLocator.initialize(this)
    }
}


