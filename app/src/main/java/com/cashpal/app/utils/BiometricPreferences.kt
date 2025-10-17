package com.cashpal.app.utils

import android.content.Context
import android.content.SharedPreferences

class BiometricPreferences(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    companion object {
        private const val PREFS_NAME = "biometric_preferences"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        private const val KEY_LAST_LOGIN_EMAIL = "last_login_email"
        private const val KEY_DEMO_MODE = "demo_mode"
    }
    
    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
    }
    
    fun isBiometricEnabled(): Boolean {
        return prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false)
    }
    
    fun setLastLoginEmail(email: String) {
        prefs.edit().putString(KEY_LAST_LOGIN_EMAIL, email).apply()
    }
    
    fun getLastLoginEmail(): String? {
        return prefs.getString(KEY_LAST_LOGIN_EMAIL, null)
    }
    
    fun setDemoMode(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DEMO_MODE, enabled).apply()
    }
    
    fun isDemoMode(): Boolean {
        return prefs.getBoolean(KEY_DEMO_MODE, false)
    }
    
    fun clearBiometricData() {
        prefs.edit()
            .remove(KEY_BIOMETRIC_ENABLED)
            .remove(KEY_LAST_LOGIN_EMAIL)
            .apply()
    }
    
    fun clearAllData() {
        prefs.edit().clear().apply()
    }
}
