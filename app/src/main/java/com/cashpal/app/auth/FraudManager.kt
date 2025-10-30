package com.cashpal.app.auth

import android.app.Activity
import android.location.Location
import android.util.Log
import com.cashpal.app.utils.LocationHelper
import com.cashpal.app.utils.LocationFix

/**
 * Checks current device location against the last saved TX location.
 * If the distance exceeds the threshold, caller should require MFA.
 */
object FraudManager {

    // 20 km threshold for “suspicious”
    private const val THRESHOLD_METERS = 20_000.0

    /**
     * Fetches a one-shot location, compares with last known TX location,
     * and calls back either [onAllowed] or [onRequireMfa].
     *
     * @param onRequireMfa  invoked when movement exceeds threshold.
     *                      You can trigger MFA there; once MFA succeeds,
     *                      remember to save the new location via FraudPrefs.saveTxLocation(...)
     */
    fun checkLocationAndMaybeRequireMfa(
        activity: Activity,
        onAllowed: () -> Unit,
        onRequireMfa: (saved: LocationFix, current: Location) -> Unit
    ) {
        LocationHelper.getOneShotLocation(activity) { current: Location? ->
            if (current == null) {
                Log.d("FraudManager", "No current location -> allow (fail-open)")
                onAllowed()
                return@getOneShotLocation
            }

            val saved: LocationFix? = FraudPrefs.getLastTxLocation(activity)
            val currentFix = LocationFix(
                lat = current.latitude,
                lon = current.longitude,
                accuracyM = current.accuracy
            )

            if (saved == null) {
                // First-time: save and allow
                Log.d("FraudManager", "No saved location -> saving first fix and allowing")
                FraudPrefs.saveTxLocation(activity, currentFix)
                FraudPrefs.setLastEventMillis(activity, System.currentTimeMillis())
                onAllowed()
                return@getOneShotLocation
            }

            val distanceMeters = distanceMeters(
                saved.lat, saved.lon,
                current.latitude, current.longitude
            )

            Log.d("FraudManager", "Distance from last TX = ${"%.1f".format(distanceMeters)} m")

            if (distanceMeters > THRESHOLD_METERS) {
                // Suspicious movement: caller should MFA
                onRequireMfa(saved, current)
            } else {
                // Close enough: refresh saved location and allow
                FraudPrefs.saveTxLocation(activity, currentFix)
                FraudPrefs.setLastEventMillis(activity, System.currentTimeMillis())
                onAllowed()
            }
        }
    }

    /** Haversine using Android's Location API for accuracy & simplicity */
    private fun distanceMeters(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0].toDouble()
    }
}
