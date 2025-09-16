package com.cashpal.app

import android.app.Application
import com.google.android.material.color.DynamicColors

class CashPalApp : Application() {
    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}


