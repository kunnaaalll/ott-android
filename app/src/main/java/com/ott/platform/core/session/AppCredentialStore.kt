package com.ott.platform.core.session

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Holds the opaque app credential the Chumbak backend issues at sign-in (`appSession.token`).
 *
 * The credential only identifies this app session to the Chumbak backend (checkout handoff,
 * subscription read, sign-out). It is not a payment credential and is never a website cookie.
 *
 * It is encrypted with an AES-256-GCM key held in the Android Keystore, so the value at rest in
 * SharedPreferences is useless if the app data is backed up or copied to another device.
 */
@Singleton
class AppCredentialStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Stores [token] with the server-provided expiry. Overwrites any previous credential. */
    @Synchronized
    fun save(token: String, expiresAtMs: Long) {
        if (token.isBlank()) return
        val encrypted = try {
            encrypt(token)
        } catch (e: Exception) {
            // If the Keystore is unavailable we refuse to persist rather than store plaintext.
            clear()
            return
        }
        prefs.edit()
            .putString(KEY_TOKEN, encrypted)
            .putLong(KEY_EXPIRES_AT, expiresAtMs)
            .apply()
    }

    /** Returns the credential, or null if absent, expired, or no longer decryptable. */
    @Synchronized
    fun getToken(nowMs: Long = System.currentTimeMillis()): String? {
        val encrypted = prefs.getString(KEY_TOKEN, null) ?: return null
        val expiresAt = prefs.getLong(KEY_EXPIRES_AT, 0L)
        if (expiresAt in 1..nowMs) {
            clear()
            return null
        }
        return try {
            decrypt(encrypted)
        } catch (e: Exception) {
            clear()
            null
        }
    }

    fun hasToken(): Boolean = getToken() != null

    @Synchronized
    fun clear() {
        prefs.edit().remove(KEY_TOKEN).remove(KEY_EXPIRES_AT).apply()
    }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return generator.generateKey()
    }

    private fun encrypt(plain: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val iv = cipher.iv
        val sealed = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(iv + sealed, Base64.NO_WRAP)
    }

    private fun decrypt(encoded: String): String {
        val bytes = Base64.decode(encoded, Base64.NO_WRAP)
        require(bytes.size > IV_BYTES) { "ciphertext too short" }
        val iv = bytes.copyOfRange(0, IV_BYTES)
        val sealed = bytes.copyOfRange(IV_BYTES, bytes.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(TAG_BITS, iv))
        return String(cipher.doFinal(sealed), Charsets.UTF_8)
    }

    private companion object {
        const val PREFS_NAME = "ott_app_credentials"
        const val KEY_TOKEN = "app_token_enc"
        const val KEY_EXPIRES_AT = "app_token_expires_at"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "ott_app_credential_key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_BYTES = 12
        const val TAG_BITS = 128
    }
}
