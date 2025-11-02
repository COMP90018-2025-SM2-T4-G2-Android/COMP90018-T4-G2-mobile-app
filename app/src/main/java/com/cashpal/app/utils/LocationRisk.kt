package com.cashpal.app.utils

import android.content.Context
import android.location.Location
import kotlin.math.abs

object LocationRisk {
    // ===== Login baseline (already have) =====
    private const val PREFS_LOGIN = "location_risk_login_prefs"
    private const val KEY_LOGIN_LAT = "login_last_lat"
    private const val KEY_LOGIN_LNG = "login_last_lng"
    private const val KEY_LOGIN_TIME = "login_last_time"
    private const val LOGIN_JUMP_THRESHOLD_METERS = 50f
    private const val ZERO_EPS = 1e-5

    fun isLoginLocationSuspicious(ctx: Context, current: Location): Boolean {
        val last = loadFix(ctx, PREFS_LOGIN, KEY_LOGIN_LAT, KEY_LOGIN_LNG, KEY_LOGIN_TIME) ?: return false
        val dist = last.distanceTo(current)
        return dist >= LOGIN_JUMP_THRESHOLD_METERS
    }

    fun saveLastLoginFix(ctx: Context, loc: Location) {
        saveFix(ctx, PREFS_LOGIN, KEY_LOGIN_LAT, KEY_LOGIN_LNG, KEY_LOGIN_TIME, loc)
    }

    // ===== Payment baseline (NEW) =====
    private const val PREFS_PAY = "location_risk_payment_prefs"
    private const val KEY_PAY_LAT = "pay_last_lat"
    private const val KEY_PAY_LNG = "pay_last_lng"
    private const val KEY_PAY_TIME = "pay_last_time"
    // Higher threshold for payment (tune as you like)
    private const val PAYMENT_JUMP_THRESHOLD_METERS = 300f

    /** Compare current vs last payment fix. */
    fun isPaymentLocationSuspicious(ctx: Context, current: Location): Boolean {
        val last = loadFix(ctx, PREFS_PAY, KEY_PAY_LAT, KEY_PAY_LNG, KEY_PAY_TIME) ?: return false
        val dist = last.distanceTo(current)
        return dist >= PAYMENT_JUMP_THRESHOLD_METERS
    }

    /** Overwrite single stored payment fix. */
    fun saveLastPaymentFix(ctx: Context, loc: Location) {
        saveFix(ctx, PREFS_PAY, KEY_PAY_LAT, KEY_PAY_LNG, KEY_PAY_TIME, loc)
    }

    // ===== Shared helpers =====
    private fun saveFix(
        ctx: Context,
        prefsName: String,
        kLat: String,
        kLng: String,
        kTime: String,
        loc: Location
    ) {
        ctx.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            .edit()
            .putFloat(kLat, loc.latitude.toFloat())
            .putFloat(kLng, loc.longitude.toFloat())
            .putLong(kTime, loc.time)
            .apply()
    }

    private fun loadFix(
        ctx: Context,
        prefsName: String,
        kLat: String,
        kLng: String,
        kTime: String
    ): Location? {
        val sp = ctx.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        if (!sp.contains(kLat) || !sp.contains(kLng)) return null
        return Location("saved").apply {
            latitude = sp.getFloat(kLat, 0f).toDouble()
            longitude = sp.getFloat(kLng, 0f).toDouble()
            time = sp.getLong(kTime, 0L)
        }
    }
}
