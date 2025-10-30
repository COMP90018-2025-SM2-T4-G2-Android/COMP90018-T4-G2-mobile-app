// utils/LocationHelper.kt
package com.cashpal.app.utils

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.google.android.gms.tasks.CancellationTokenSource


object LocationHelper {
    private const val TAG = "LocationHelper"

    /** Return true if either fine or coarse is granted. */
    private fun hasLocationPermission(activity: Activity): Boolean {
        val fine = ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    /**
     * Gets a fresh one-shot location. If permission is missing or any error happens,
     * callback receives null.
     */
    @SuppressLint("MissingPermission")
    fun getOneShotLocation(activity: Activity, callback: (Location?) -> Unit) {
        val client = LocationServices.getFusedLocationProviderClient(activity)
        client.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            CancellationTokenSource().token
        ).addOnSuccessListener { loc ->
            if (loc != null) callback(loc)
            else {
                val req = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 0)
                    .setMaxUpdates(1)
                    .build()
                client.requestLocationUpdates(req, object : LocationCallback() {
                    override fun onLocationResult(result: LocationResult) {
                        client.removeLocationUpdates(this)
                        callback(result.lastLocation)
                    }
                }, Looper.getMainLooper())
            }
        }
    }

    @SuppressLint("MissingPermission") // guarded by hasLocationPermission before call sites
    private fun requestSingleUpdate(
        activity: Activity,
        client: FusedLocationProviderClient,
        onResult: (Location?) -> Unit
    ) {
        if (!hasLocationPermission(activity)) {
            onResult(null)
            return
        }

        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            1_000L
        )
            .setWaitForAccurateLocation(true)
            .setMaxUpdates(1)
            .build()

        val cb = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                client.removeLocationUpdates(this)
                onResult(result.lastLocation)
            }
        }

        try {
            client.requestLocationUpdates(request, cb, Looper.getMainLooper())
                .addOnFailureListener {
                    Log.w(TAG, "requestLocationUpdates failed: ${it.message}")
                    onResult(null)
                }
        } catch (se: SecurityException) {
            Log.e(TAG, "SecurityException requesting updates", se)
            onResult(null)
        }
    }
}
