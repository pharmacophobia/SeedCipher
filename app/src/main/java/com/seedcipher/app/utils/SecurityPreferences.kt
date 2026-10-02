package com.seedcipher.app.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.MessageDigest

object SecurityPreferences {
    private const val PREF_NAME = "seedcipher_secure_prefs"
    private const val KEY_PIN_HASH = "key_pin_hash"
    private const val KEY_PIN_SALT = "key_pin_salt"
    private const val KEY_SAVED_SEED = "key_saved_seed"
    private const val DEFAULT_SEED = "QuantumKey-2026"

    private fun getPrefs(context: Context): SharedPreferences {
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                PREF_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        }
    }

    private fun hashPin(pin: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val combined = "$pin:$salt".toByteArray()
        val hashBytes = digest.digest(combined)
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    fun isPinSet(context: Context): Boolean {
        val prefs = getPrefs(context)
        return !prefs.getString(KEY_PIN_HASH, null).isNullOrEmpty()
    }

    fun savePin(context: Context, pin: String): Boolean {
        if (pin.length < 4) return false
        val salt = System.currentTimeMillis().toString() + "_seed_salt"
        val hash = hashPin(pin, salt)
        val prefs = getPrefs(context)
        prefs.edit()
            .putString(KEY_PIN_HASH, hash)
            .putString(KEY_PIN_SALT, salt)
            .apply()
        return true
    }

    fun verifyPin(context: Context, inputPin: String): Boolean {
        val prefs = getPrefs(context)
        val storedHash = prefs.getString(KEY_PIN_HASH, null) ?: return false
        val storedSalt = prefs.getString(KEY_PIN_SALT, "") ?: ""
        val inputHash = hashPin(inputPin, storedSalt)
        return storedHash == inputHash
    }

    fun getSavedSeedKey(context: Context): String {
        val prefs = getPrefs(context)
        return prefs.getString(KEY_SAVED_SEED, DEFAULT_SEED) ?: DEFAULT_SEED
    }

    fun saveSeedKey(context: Context, seedKey: String) {
        val prefs = getPrefs(context)
        prefs.edit().putString(KEY_SAVED_SEED, seedKey).apply()
    }
}
