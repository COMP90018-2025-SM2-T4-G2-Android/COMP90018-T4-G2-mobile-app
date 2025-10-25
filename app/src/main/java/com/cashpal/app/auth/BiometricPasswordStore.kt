package com.cashpal.app.auth

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
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
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, getKey(), GCMParameterSpec(128, iv))
        return cipher
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
    // --- Multi-Factor Extension (PIN support) ---

    private const val PREF_PIN_HASH = "pin_hash"
    private const val PREF_PIN_SALT = "pin_salt"

    /**
     * Sets a secure numeric PIN (4–8 digits) hashed and salted.
     */
    fun setPin(ctx: Context, pin: String) {
        require(pin.length in 4..8 && pin.all { it.isDigit() }) { "PIN must be 4–8 digits" }

        val salt = ByteArray(16).also { java.security.SecureRandom().nextBytes(it) }
        val hash = hashPin(pin, salt)

        prefs(ctx).edit {
            putString(PREF_PIN_SALT, android.util.Base64.encodeToString(salt, android.util.Base64.NO_WRAP))
            putString(PREF_PIN_HASH, android.util.Base64.encodeToString(hash, android.util.Base64.NO_WRAP))
        }
    }

    /**
     * Verifies a user-entered PIN against stored hash.
     */
    fun verifyPin(ctx: Context, pin: String): Boolean {
        val saltB64 = prefs(ctx).getString(PREF_PIN_SALT, null) ?: return false
        val hashB64 = prefs(ctx).getString(PREF_PIN_HASH, null) ?: return false

        val salt = android.util.Base64.decode(saltB64, android.util.Base64.NO_WRAP)
        val storedHash = android.util.Base64.decode(hashB64, android.util.Base64.NO_WRAP)
        val computed = hashPin(pin, salt)

        return constantTimeEquals(storedHash, computed)
    }

    /**
     * Checks if a PIN has been configured.
     */
    fun isPinSet(ctx: Context): Boolean =
        prefs(ctx).contains(PREF_PIN_HASH) && prefs(ctx).contains(PREF_PIN_SALT)

    private fun hashPin(pin: String, salt: ByteArray): ByteArray {
        val spec = javax.crypto.spec.PBEKeySpec(pin.toCharArray(), salt, 100_000, 256)
        val skf = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return skf.generateSecret(spec).encoded
    }

    private fun constantTimeEquals(a: ByteArray, b: ByteArray): Boolean {
        if (a.size != b.size) return false
        var r = 0
        for (i in a.indices) r = r or (a[i].toInt() xor b[i].toInt())
        return r == 0
    }

}
