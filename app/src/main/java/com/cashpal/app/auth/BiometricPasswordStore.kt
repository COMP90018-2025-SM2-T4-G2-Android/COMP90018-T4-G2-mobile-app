package com.cashpal.app.auth

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import androidx.biometric.BiometricPrompt
import androidx.core.content.edit
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import android.util.Base64

object BiometricPasswordStore {
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "cashpal_bio_key_v1"
    private const val PREFS = "bio_store"
    private const val PREF_EMAIL = "email"
    private const val PREF_PW_ENC = "pw_enc"
    private const val PREF_PW_IV  = "pw_iv"
    private const val PREF_ENABLED = "enabled"

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Call once (first enable) */
    private fun ensureKey() {
        val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (!ks.containsAlias(KEY_ALIAS)) {
            val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
            val spec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setUserAuthenticationRequired(true) // must authenticate to use key
                .build()
            gen.init(spec)
            gen.generateKey()
        }
    }

    private fun getKey(): SecretKey {
        val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        return (ks.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry).secretKey
    }

    /** Prep a cipher for ENCRYPT and show in BiometricPrompt as CryptoObject */
    fun createEncryptCipher(): Cipher {
        ensureKey()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getKey())
        return cipher
    }

    /** Prep a cipher for DECRYPT using saved IV */
    fun createDecryptCipher(ctx: Context): Cipher? {
        val iv = prefs(ctx).getString(PREF_PW_IV, null)?.let { Base64.decode(it, Base64.DEFAULT) }
            ?: return null
        
        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, getKey(), GCMParameterSpec(128, iv))
            cipher
        } catch (e: KeyPermanentlyInvalidatedException) {
            // Key was invalidated (e.g., user changed biometrics or device credentials)
            // Clear stored credentials and disable biometric login
            disable(ctx)
            null
        } catch (e: Exception) {
            // Handle other KeyStore exceptions (e.g., key not found, initialization failed)
            // Clear stored credentials to prevent repeated failures
            disable(ctx)
            null
        }
    }

    /** Persist encrypted password + email */
    fun saveEncrypted(ctx: Context, email: String, cipher: Cipher, plainPassword: String) {
        val enc = cipher.doFinal(plainPassword.toByteArray(Charsets.UTF_8))
        prefs(ctx).edit {
            putString(PREF_EMAIL, email)
            putString(PREF_PW_ENC, Base64.encodeToString(enc, Base64.DEFAULT))
            putString(PREF_PW_IV,  Base64.encodeToString(cipher.iv, Base64.DEFAULT))
            putBoolean(PREF_ENABLED, true)
        }
    }

    fun isEnabled(ctx: Context) = prefs(ctx).getBoolean(PREF_ENABLED, false)
    fun getEmail(ctx: Context) = prefs(ctx).getString(PREF_EMAIL, null)

    /** Read encrypted bytes (for decrypt) */
    fun getEncryptedPassword(ctx: Context): ByteArray? =
        prefs(ctx).getString(PREF_PW_ENC, null)?.let { Base64.decode(it, Base64.DEFAULT) }

    fun disable(ctx: Context) = prefs(ctx).edit { clear() }

}
