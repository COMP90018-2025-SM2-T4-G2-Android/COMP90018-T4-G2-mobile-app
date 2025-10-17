package com.cashpal.app.utils

import android.content.Context
import android.util.Log
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability

object GooglePlayServicesUtils {
    
    private const val TAG = "GooglePlayServicesUtils"
    
    fun checkGooglePlayServices(context: Context): Boolean {
        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(context)
        
        return when (resultCode) {
            ConnectionResult.SUCCESS -> {
                Log.d(TAG, "Google Play Services is available")
                true
            }
            ConnectionResult.SERVICE_MISSING -> {
                Log.e(TAG, "Google Play Services is missing")
                false
            }
            ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED -> {
                Log.e(TAG, "Google Play Services needs to be updated")
                false
            }
            ConnectionResult.SERVICE_DISABLED -> {
                Log.e(TAG, "Google Play Services is disabled")
                false
            }
            ConnectionResult.SERVICE_INVALID -> {
                Log.e(TAG, "Google Play Services is invalid")
                false
            }
            else -> {
                Log.e(TAG, "Google Play Services error: $resultCode")
                false
            }
        }
    }
    
    fun logGooglePlayServicesStatus(context: Context) {
        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(context)
        
        Log.d(TAG, "Google Play Services status: $resultCode")
        
        when (resultCode) {
            ConnectionResult.SUCCESS -> Log.d(TAG, "Google Play Services is working correctly")
            ConnectionResult.SERVICE_MISSING -> Log.e(TAG, "Google Play Services is missing")
            ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED -> Log.e(TAG, "Google Play Services needs update")
            ConnectionResult.SERVICE_DISABLED -> Log.e(TAG, "Google Play Services is disabled")
            ConnectionResult.SERVICE_INVALID -> Log.e(TAG, "Google Play Services is invalid")
            else -> Log.e(TAG, "Google Play Services error: $resultCode")
        }
    }
}
