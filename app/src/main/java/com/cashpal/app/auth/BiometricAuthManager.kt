package com.cashpal.app.auth

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Shows BiometricPrompt. If [onUsePassword] is provided, the prompt will show a
 * "Use Password" button and invoke that callback when tapped.
 */
class BiometricAuthManager(
    private val context: Context,
    private val activity: FragmentActivity
) {
    interface BiometricCallback {
        fun onSuccess()
        fun onError(errorCode: Int, errorMessage: String)
        fun onFailed()
    }

    /** True if strong biometrics are available & enrolled. */
    fun isBiometricAvailable(): Boolean {
        val bm = BiometricManager.from(context)
        return bm.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
                BiometricManager.BIOMETRIC_SUCCESS
    }

    /**
     * Show the system biometric prompt.
     *
     * If [onUsePassword] is non-null, the sheet shows a "Use Password" negative button
     * and we invoke it when the user taps that button.
     */
    fun showBiometricPrompt(
        title: String,
        subtitle: String? = null,
        crypto: BiometricPrompt.CryptoObject? = null,
        callback: BiometricCallback,
        negativeLabel: String = "Use Password",
        onUsePassword: (() -> Unit)? = null
    ) {
        val builder = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle ?: "")

        if (onUsePassword != null) {
            // We must NOT include DEVICE_CREDENTIAL when using a custom negative button.
            builder.setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            builder.setNegativeButtonText(negativeLabel)
        } else {
            // No password fallback requested; allow device credential as system fallback if you want.
            builder.setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG
                // If you still want device PIN/pattern fallback when no custom password is provided,
                // uncomment the next line:
                // or BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
        }

        val promptInfo = builder.build()
        val executor = ContextCompat.getMainExecutor(context)

        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    callback.onSuccess()
                }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON && onUsePassword != null) {
                        // User tapped "Use Password"
                        onUsePassword.invoke()
                    } else {
                        callback.onError(errorCode, errString.toString())
                    }
                }
                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    callback.onFailed()
                }
            }
        )

        if (crypto != null) {
            prompt.authenticate(promptInfo, crypto)
        } else {
            prompt.authenticate(promptInfo)
        }
    }
}
