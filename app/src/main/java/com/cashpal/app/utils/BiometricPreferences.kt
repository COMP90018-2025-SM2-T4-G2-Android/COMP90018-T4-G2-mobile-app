package com.cashpal.app.utils

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Securely stores biometric-related preferences and credentials.
 * Used for biometric login (fingerprint or face authentication).
 */
class BiometricPreferences(context: Context) {

    // Master key for AES encryption
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    // Encrypted shared preferences
    private val prefs = EncryptedSharedPreferences.create(
        context,
        "biometric_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    /** Save the last login email for quick autofill */
    fun setLastLoginEmail(email: String) =
        prefs.edit().putString("last_email", email).apply()

    fun getLastLoginEmail(): String? =
        prefs.getString("last_email", null)

    /** Enable/disable biometric login flag */
    fun setBiometricEnabled(enabled: Boolean) =
        prefs.edit().putBoolean("biometric_enabled", enabled).apply()

    fun isBiometricEnabled(): Boolean =
        prefs.getBoolean("biometric_enabled", false)

    /** Save encrypted credentials for biometric sign-in */
    fun saveCredentials(email: String, password: String) {
        prefs.edit()
            .putString("cred_email", email)
            .putString("cred_password", password)
            .apply()
    }

    /** Retrieve saved credentials if both exist */
    fun getSavedCredentials(): Pair<String, String>? {
        val email = prefs.getString("cred_email", null)
        val password = prefs.getString("cred_password", null)
        return if (email.isNullOrBlank() || password.isNullOrBlank()) null else email to password
    }

    /** Remove saved credentials (for logout or security reasons) */
    fun clearCredentials() {
        prefs.edit()
            .remove("cred_email")
            .remove("cred_password")
            .apply()
    }

    /** Optional: toggle demo mode */
    fun setDemoMode(enabled: Boolean) =
        prefs.edit().putBoolean("demo_mode", enabled).apply()

    fun isDemoMode(): Boolean =
        prefs.getBoolean("demo_mode", false)
}
