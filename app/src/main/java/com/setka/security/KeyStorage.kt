package com.setka.security

import android.content.Context
import androidx.core.content.edit
import java.security.KeyPair
import java.security.PrivateKey
import java.security.PublicKey
import javax.crypto.SecretKey

/**
 * Хранит ключевую пару пользователя в SharedPreferences (зашифровано Android Keystore).
 * Публичные ключи собеседников — в памяти (сессионно) и в БД (долгосрочно).
 */
class KeyStorage(context: Context) {

    private val prefs = context.getSharedPreferences("setka_keys", Context.MODE_PRIVATE)

    // Кэш общих секретов: userId → SecretKey
    private val sessionKeys = mutableMapOf<String, SecretKey>()

    // ── Своя ключевая пара ───────────────────────────────────────────

    fun getOrCreateMyKeyPair(): KeyPair {
        val pubB64  = prefs.getString("my_pub_key", null)
        val privB64 = prefs.getString("my_priv_key", null)

        if (pubB64 != null && privB64 != null) {
            return KeyPair(
                CryptoManager.publicKeyFromBase64(pubB64),
                CryptoManager.privateKeyFromBase64(privB64)
            )
        }

        val kp = CryptoManager.generateKeyPair()
        prefs.edit {
            putString("my_pub_key",  CryptoManager.publicKeyToBase64(kp.public))
            putString("my_priv_key", CryptoManager.privateKeyToBase64(kp.private))
        }
        return kp
    }

    fun getMyPublicKeyBase64(): String {
        return prefs.getString("my_pub_key", null)
            ?: CryptoManager.publicKeyToBase64(getOrCreateMyKeyPair().public)
    }

    // ── Сессионные ключи (общий секрет с собеседником) ───────────────

    fun getOrDeriveSessionKey(
        userId: String,
        theirPublicKeyBase64: String,
        myPrivateKey: PrivateKey
    ): SecretKey {
        sessionKeys[userId]?.let { return it }
        val theirPubKey = CryptoManager.publicKeyFromBase64(theirPublicKeyBase64)
        val secret      = CryptoManager.deriveSharedSecret(myPrivateKey, theirPubKey)
        sessionKeys[userId] = secret
        return secret
    }

    fun getSessionKey(userId: String): SecretKey? = sessionKeys[userId]

    fun storeSessionKey(userId: String, key: SecretKey) {
        sessionKeys[userId] = key
    }

    fun clearSessionKey(userId: String) {
        sessionKeys.remove(userId)
    }

    fun clearAll() {
        sessionKeys.clear()
    }
}
