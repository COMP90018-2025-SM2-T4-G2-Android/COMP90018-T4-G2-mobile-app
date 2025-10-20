package com.cashpal.app.auth

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Handles showing the BiometricPrompt and verifying availability.
 * Supports fingerprint and face (if device supports & enrolled).
 */
class BiometricAuthManager(
    private val context: Context,
    private val activity: FragmentActivity   // <- MUST be FragmentActivity
) {

    interface BiometricCallback {
        fun onSuccess()
        fun onError(errorCode: Int, errorMessage: String)
        fun onFailed()
    }

    /** True if biometric hardware exists and at least one biometric is enrolled */
    fun isBiometricAvailable(): Boolean {
        val bm = BiometricManager.from(context)
        return when (bm.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )) {
            BiometricManager.BIOMETRIC_SUCCESS -> true
            else -> false
        }
    }

    /**
     * Show the system biometric prompt.
     *
     * @param title      Title displayed on the sheet
     * @param subtitle   Optional subtitle
     * @param crypto     Optional CryptoObject for encrypt/decrypt flows
     * @param callback   Result callbacks
     */
    fun showBiometricPrompt(
        title: String,
        subtitle: String? = null,
        crypto: BiometricPrompt.CryptoObject? = null,
        callback: BiometricCallback
    ) {
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle ?: "")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()

        val executor = ContextCompat.getMainExecutor(context)
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult
                ) {
                    super.onAuthenticationSucceeded(result)
                    callback.onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    callback.onError(errorCode, errString.toString())
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    callback.onFailed()
                }
            }
        )

        if (crypto != null) {
            prompt.authenticate(promptInfo, crypto)  // <- valid overload
        } else {
            prompt.authenticate(promptInfo)          // <- valid overload
        }
    }
}
