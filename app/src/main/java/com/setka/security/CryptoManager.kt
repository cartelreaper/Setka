package com.setka.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.*
import java.security.spec.ECGenParameterSpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * End-to-end шифрование:
 * 1. Каждый пользователь генерирует пару ключей ECDH (P-256)
 * 2. При хэндшейке обмениваются публичными ключами
 * 3. Общий секрет выводится через ECDH → AES-256-GCM для шифрования сообщений
 */
object CryptoManager {

    private const val KEY_ALGORITHM   = "EC"
    private const val CURVE           = "secp256r1"
    private const val AGREEMENT_ALG   = "ECDH"
    private const val CIPHER_ALG      = "AES/GCM/NoPadding"
    private const val KEY_SIZE        = 256
    private const val GCM_TAG_BITS    = 128
    private const val GCM_IV_BYTES    = 12

    // ── Генерация ключевой пары ──────────────────────────────────────

    fun generateKeyPair(): KeyPair {
        val gen = KeyPairGenerator.getInstance(KEY_ALGORITHM)
        gen.initialize(ECGenParameterSpec(CURVE), SecureRandom())
        return gen.generateKeyPair()
    }

    fun publicKeyToBase64(key: PublicKey): String =
        Base64.encodeToString(key.encoded, Base64.NO_WRAP)

    fun publicKeyFromBase64(b64: String): PublicKey {
        val bytes   = Base64.decode(b64, Base64.NO_WRAP)
        val keySpec = X509EncodedKeySpec(bytes)
        return KeyFactory.getInstance(KEY_ALGORITHM).generatePublic(keySpec)
    }

    fun privateKeyToBase64(key: PrivateKey): String =
        Base64.encodeToString(key.encoded, Base64.NO_WRAP)

    fun privateKeyFromBase64(b64: String): PrivateKey {
        val bytes   = Base64.decode(b64, Base64.NO_WRAP)
        val keySpec = java.security.spec.PKCS8EncodedKeySpec(bytes)
        return KeyFactory.getInstance(KEY_ALGORITHM).generatePrivate(keySpec)
    }

    // ── Общий секрет ECDH ────────────────────────────────────────────

    fun deriveSharedSecret(myPrivateKey: PrivateKey, theirPublicKey: PublicKey): SecretKey {
        val agreement = KeyAgreement.getInstance(AGREEMENT_ALG)
        agreement.init(myPrivateKey)
        agreement.doPhase(theirPublicKey, true)
        val sharedBytes = agreement.generateSecret()
        // Хэшируем через SHA-256 чтобы получить ровно 32 байта для AES-256
        val digest  = MessageDigest.getInstance("SHA-256")
        val keyBytes = digest.digest(sharedBytes)
        return SecretKeySpec(keyBytes, "AES")
    }

    // ── Шифрование / дешифрование ────────────────────────────────────

    /**
     * Шифрует текст. Возвращает Base64(IV + ciphertext).
     */
    fun encrypt(plaintext: String, secretKey: SecretKey): String {
        val iv     = ByteArray(GCM_IV_BYTES).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance(CIPHER_ALG)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_BITS, iv))
        val cipherBytes = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        val result = iv + cipherBytes
        return Base64.encodeToString(result, Base64.NO_WRAP)
    }

    /**
     * Дешифрует Base64(IV + ciphertext). Возвращает null при ошибке.
     */
    fun decrypt(encrypted: String, secretKey: SecretKey): String? {
        return try {
            val bytes      = Base64.decode(encrypted, Base64.NO_WRAP)
            val iv         = bytes.sliceArray(0 until GCM_IV_BYTES)
            val cipherBytes = bytes.sliceArray(GCM_IV_BYTES until bytes.size)
            val cipher     = Cipher.getInstance(CIPHER_ALG)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_BITS, iv))
            String(cipher.doFinal(cipherBytes), Charsets.UTF_8)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Шифрует бинарные данные (голос, фото, файл).
     */
    fun encryptBytes(data: ByteArray, secretKey: SecretKey): ByteArray {
        val iv     = ByteArray(GCM_IV_BYTES).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance(CIPHER_ALG)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_BITS, iv))
        val cipherBytes = cipher.doFinal(data)
        return iv + cipherBytes
    }

    /**
     * Дешифрует бинарные данные. Возвращает null при ошибке.
     */
    fun decryptBytes(data: ByteArray, secretKey: SecretKey): ByteArray? {
        return try {
            val iv          = data.sliceArray(0 until GCM_IV_BYTES)
            val cipherBytes = data.sliceArray(GCM_IV_BYTES until data.size)
            val cipher      = Cipher.getInstance(CIPHER_ALG)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_BITS, iv))
            cipher.doFinal(cipherBytes)
        } catch (e: Exception) {
            null
        }
    }
}
