package com.setka.network.transport

data class HandshakePayload(
    val userId: String,
    val nickname: String,
    val avatarColor: Int,
    val publicKey: String  = "",   // публичный ключ ECDH для E2E шифрования
    val appVersion: Int    = 2
)

data class MessagePayload(
    val messageId: String,
    val chatId: String,
    val text: String,
    val replyToMessageId: String? = null,
    val sentAt: Long = System.currentTimeMillis()
)

data class MessageStatusPayload(
    val messageId: String,
    val status: String
)

data class FileInfoPayload(
    val fileId: String,
    val fileName: String,
    val fileSizeBytes: Long,
    val mimeType: String,
    val totalChunks: Int
)

data class FileChunkPayload(
    val fileId: String,
    val chunkIndex: Int,
    val totalChunks: Int
)

data class TypingPayload(
    val chatId: String,
    val isTyping: Boolean
)
