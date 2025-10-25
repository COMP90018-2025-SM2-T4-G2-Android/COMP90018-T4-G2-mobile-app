package com.cashpal.app.auth

import androidx.fragment.app.FragmentActivity

/**
 * MFA guard = biometric first, then PIN (if set).
 * Call MfaGuard.requireAuth(activity) { /* secure action */ }
 */
object MfaGuard {

    fun requireAuth(activity: FragmentActivity, onPassed: () -> Unit) {
        val biometricManager = BiometricAuthManager(activity, activity)

        biometricManager.showBiometricPrompt(
            title = "Confirm your identity",
            subtitle = "Use fingerprint or device PIN",
            // NOTE: No cryptoObject here — your BiometricAuthManager doesn't accept it
            callback = object : BiometricAuthManager.BiometricCallback {

                override fun onSuccess() {
                    // If a PIN is configured, require it after biometric success
                    if (BiometricPasswordStore.isPinSet(activity)) {
                        // PinVerifyDialogFragment should accept a no-arg onPassed() callback
                        PinVerifyDialogFragment {
                            onPassed()
                        }.show(activity.supportFragmentManager, "PinVerify")
                    } else {
                        onPassed()
                    }
                }

                override fun onError(errorCode: Int, errorMessage: String) {
                    // Optional: Toast or log
                }

                override fun onFailed() {
                    // Optional: Toast or log
                }
            }
        )
    }
}
