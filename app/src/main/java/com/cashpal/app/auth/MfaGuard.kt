package com.cashpal.app.auth

import androidx.fragment.app.FragmentActivity
import com.cashpal.app.dialogs.PasswordVerifyDialogFragment

/**
 * Central MFA: biometric first, then PASSWORD fallback (no PIN).
 * Usage: MfaGuard.requireAuth(activity) { /* secure action */ }
 */
object MfaGuard {

    fun requireAuth(activity: FragmentActivity, onPassed: () -> Unit) {
        val biometricManager = BiometricAuthManager(activity, activity)

        biometricManager.showBiometricPrompt(
            title = "Confirm your identity",
            subtitle = "Use fingerprint or password",
            crypto = null, // not doing crypto-bound flows here
            callback = object : BiometricAuthManager.BiometricCallback {
                override fun onSuccess() {
                    onPassed()
                }
                override fun onError(errorCode: Int, errorMessage: String) {
                    // Errors are handled by the prompt; no-op.
                }
                override fun onFailed() {
                    // Bad sample; prompt remains. No-op.
                }
            },
            negativeLabel = "Use Password",
            onUsePassword = {
                // Fallback: verify account password in a small dialog.
                val prefill = BiometricPasswordStore.getEmail(activity)
                PasswordVerifyDialogFragment.new(prefill) { ok ->
                    if (ok) onPassed()
                }.show(activity.supportFragmentManager, "PasswordVerify")
            }
        )
    }
}
