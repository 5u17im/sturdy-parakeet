package com.nothingsense.ns.data.backup

import android.util.Base64
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class BackupChatItem(
    val id: String,
    val name: String,
    val type: String,
    val lastMessage: String?,
    val lastMessageTimestamp: Long?
)

@Serializable
data class BackupMessageItem(
    val id: String,
    val chatId: String,
    val senderId: String,
    val text: String,
    val timestamp: Long,
    val type: String,
    val fileUri: String? = null,
    val fileType: String? = null,
    val fileName: String? = null,
    val status: String
)

@Serializable
data class BackupDataPayload(
    val userId: String,
    val username: String,
    val bio: String,
    val chats: List<BackupChatItem>,
    val messages: List<BackupMessageItem>,
    val timestamp: Long = System.currentTimeMillis()
)

@Singleton
class NoSenseBackupEngine @Inject constructor() {

    companion object {
        private val MAGIC_HEADER = byteArrayOf(0x4E.toByte(), 0x53.toByte(), 0x42.toByte(), 0x4B.toByte()) // "NSBK"
        private const val FORMAT_VERSION: Short = 1
        private const val SALT_LENGTH = 16
        private const val IV_LENGTH = 12
        private const val TAG_LENGTH_BITS = 128
        private const val PBKDF2_ITERATIONS = 100000
        private const val KEY_LENGTH_BITS = 256
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
    }

    fun packAndEncrypt(payload: BackupDataPayload, passphrase: String): ByteArray {
        val jsonString = Json.encodeToString(payload)
        val plainBytes = jsonString.toByteArray(Charsets.UTF_8)

        val random = SecureRandom()
        val salt = ByteArray(SALT_LENGTH)
        random.nextBytes(salt)

        val iv = ByteArray(IV_LENGTH)
        random.nextBytes(iv)

        val secretKey = deriveKey(passphrase, salt)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(TAG_LENGTH_BITS, iv))
        val encryptedBytes = cipher.doFinal(plainBytes)

        val baos = ByteArrayOutputStream()
        val dos = DataOutputStream(baos)

        dos.write(MAGIC_HEADER)
        dos.writeShort(FORMAT_VERSION.toInt())
        dos.write(salt)
        dos.write(iv)
        dos.writeInt(encryptedBytes.size)
        dos.write(encryptedBytes)
        dos.flush()

        return baos.toByteArray()
    }

    fun unpackAndDecrypt(containerBytes: ByteArray, passphrase: String): BackupDataPayload {
        val dis = DataInputStream(ByteArrayInputStream(containerBytes))

        val header = ByteArray(4)
        dis.readFully(header)
        if (!header.contentEquals(MAGIC_HEADER)) {
            throw IllegalArgumentException("Formato no válido: Encabezado mágico NSBK no coincide.")
        }

        val version = dis.readShort()
        if (version.toInt() != FORMAT_VERSION.toInt()) {
            throw IllegalArgumentException("Versión de respaldo no compatible: $version")
        }

        val salt = ByteArray(SALT_LENGTH)
        dis.readFully(salt)

        val iv = ByteArray(IV_LENGTH)
        dis.readFully(iv)

        val encryptedSize = dis.readInt()
        val encryptedBytes = ByteArray(encryptedSize)
        dis.readFully(encryptedBytes)

        val secretKey = deriveKey(passphrase, salt)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(TAG_LENGTH_BITS, iv))
        val decryptedBytes = cipher.doFinal(encryptedBytes)

        val jsonString = String(decryptedBytes, Charsets.UTF_8)
        return Json.decodeFromString<BackupDataPayload>(jsonString)
    }

    private fun deriveKey(passphrase: String, salt: ByteArray): SecretKeySpec {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(passphrase.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS)
        val tmpKey = factory.generateSecret(spec)
        return SecretKeySpec(tmpKey.encoded, "AES")
    }
}
