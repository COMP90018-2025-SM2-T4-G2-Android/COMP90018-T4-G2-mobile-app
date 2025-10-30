package com.cashpal.app.auth

import android.content.Context
import com.cashpal.app.utils.LocationFix

object FraudPrefs {
    private const val PREF = "fraud_prefs"
    private const val KEY_LAST_LAT = "fraud_last_lat"
    private const val KEY_LAST_LON = "fraud_last_lon"
    private const val KEY_LAST_ACC = "fraud_last_acc"
    private const val KEY_LAST_MS  = "fraud_last_ms"

    private fun prefs(ctx: Context) =
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    fun saveTxLocation(ctx: Context, loc: LocationFix) {
        prefs(ctx).edit()
            .putFloat(KEY_LAST_LAT, loc.lat.toFloat())
            .putFloat(KEY_LAST_LON, loc.lon.toFloat())
            .putFloat(KEY_LAST_ACC, loc.accuracyM)
            .apply()
    }

    fun getLastTxLocation(ctx: Context): LocationFix? {
        val p = prefs(ctx)
        if (!p.contains(KEY_LAST_LAT) || !p.contains(KEY_LAST_LON)) return null
        val lat = p.getFloat(KEY_LAST_LAT, Float.NaN)
        val lon = p.getFloat(KEY_LAST_LON, Float.NaN)
        if (lat.isNaN() || lon.isNaN()) return null
        val acc = p.getFloat(KEY_LAST_ACC, 0f)
        return LocationFix(lat = lat.toDouble(), lon = lon.toDouble(), accuracyM = acc)
    }

    fun setLastEventMillis(ctx: Context, millis: Long) {
        prefs(ctx).edit().putLong(KEY_LAST_MS, millis).apply()
    }

    fun getLastEventMillis(ctx: Context): Long =
        prefs(ctx).getLong(KEY_LAST_MS, 0L)
}
