package com.setka.network.transport

import android.content.Context
import com.setka.data.model.*
import com.setka.data.repository.*
import com.setka.security.CryptoManager
import com.setka.security.KeyStorage
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class NetworkService(
    private val context: Context,
    private val transportManager: TransportManager,
    private val messageRepo: MessageRepository,
    private val chatRepo: ChatRepository,
    private val contactRepo: ContactRepository,
    private val userRepo: UserRepository
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var myUser: User? = null
    private lateinit var keyStorage: KeyStorage

    private val _typingStates = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val typingStates: StateFlow<Map<String, Boolean>> = _typingStates.asStateFlow()

    private val _lastReceivedMessage = MutableSharedFlow<Message>(extraBufferCapacity = 16)
    val lastReceivedMessage: Flow<Message> = _lastReceivedMessage.asSharedFlow()

    fun start() {
        scope.launch {
            myUser = userRepo.getMyUser()
            keyStorage = KeyStorage(context)
            keyStorage.getOrCreateMyKeyPair()
            listenIncomingPackets()
        }
    }

    fun stop() { scope.cancel() }

    // ── Входящие ─────────────────────────────────────────────────────

    private fun listenIncomingPackets() {
        scope.launch {
            transportManager.incomingPackets.collect { packet ->
                when (packet.type) {
                    PacketType.HANDSHAKE      -> handleHandshake(packet)
                    PacketType.MESSAGE        -> handleMessage(packet)
                    PacketType.MESSAGE_STATUS -> handleMessageStatus(packet)
                    PacketType.VOICE          -> handleVoice(packet)
                    PacketType.IMAGE          -> handleImage(packet)
                    PacketType.FILE_INFO      -> handleFile(packet)
                    PacketType.TYPING         -> handleTyping(packet)
                    PacketType.PING           -> sendHandshake()
                    else -> {}
                }
            }
        }
    }

    private suspend fun handleHandshake(packet: NetworkPacket) {
        val payload = PacketSerializer.fromJson(packet.payload, HandshakePayload::class.java)
        contactRepo.addOrUpdate(
            Contact(
                userId      = payload.userId,
                nickname    = payload.nickname,
                avatarColor = payload.avatarColor,
                publicKey   = payload.publicKey
            )
        )
        // Деривируем общий ключ если есть публичный ключ собеседника
        if (payload.publicKey.isNotBlank()) {
            try {
                val myKeyPair = keyStorage.getOrCreateMyKeyPair()
                keyStorage.getOrDeriveSessionKey(
                    userId              = payload.userId,
                    theirPublicKeyBase64 = payload.publicKey,
                    myPrivateKey        = myKeyPair.private
                )
            } catch (_: Exception) {}
        }
        sendHandshake()
    }

    private suspend fun handleMessage(packet: NetworkPacket) {
        val me = myUser ?: return
        // Расшифровываем если есть сессионный ключ
        val decryptedPayload = tryDecrypt(packet.payload, packet.senderId) ?: packet.payload
        val payload = try {
            PacketSerializer.fromJson(decryptedPayload, MessagePayload::class.java)
        } catch (_: Exception) { return }

        val chatId = chatRepo.getOrCreateDirectChat(
            me.id, me.nickname, packet.senderId, packet.senderNickname)
        val message = Message(
            id               = payload.messageId,
            chatId           = chatId,
            senderId         = packet.senderId,
            senderNickname   = packet.senderNickname,
            type             = MessageType.TEXT,
            text             = payload.text,
            status           = MessageStatus.DELIVERED,
            transport        = packet.transport,
            sentAt           = payload.sentAt,
            replyToMessageId = payload.replyToMessageId
        )
        messageRepo.sendMessage(message)
        _lastReceivedMessage.emit(message)
        sendStatusUpdate(payload.messageId, "DELIVERED")
    }

    private suspend fun handleMessageStatus(packet: NetworkPacket) {
        val payload = PacketSerializer.fromJson(packet.payload, MessageStatusPayload::class.java)
        val status = when (payload.status) {
            "DELIVERED" -> MessageStatus.DELIVERED
            "READ"      -> MessageStatus.READ
            else        -> return
        }
        messageRepo.updateStatus(payload.messageId, status)
    }

    private suspend fun handleVoice(packet: NetworkPacket) {
        val data = packet.binaryData ?: return
        val me   = myUser ?: return
        // Расшифровываем бинарные данные
        val decryptedData = tryDecryptBytes(data, packet.senderId) ?: data
        val file = context.filesDir.resolve("voices/voice_${packet.id}.ogg")
            .also { it.parentFile?.mkdirs() }
        file.writeBytes(decryptedData)
        val chatId = chatRepo.getOrCreateDirectChat(
            me.id, me.nickname, packet.senderId, packet.senderNickname)
        val message = Message(
            id             = packet.id,
            chatId         = chatId,
            senderId       = packet.senderId,
            senderNickname = packet.senderNickname,
            type           = MessageType.VOICE,
            mediaPath      = file.absolutePath,
            status         = MessageStatus.DELIVERED,
            transport      = packet.transport,
            sentAt         = packet.timestamp
        )
        messageRepo.sendMessage(message)
        _lastReceivedMessage.emit(message)
        sendStatusUpdate(packet.id, "DELIVERED")
    }

    private suspend fun handleImage(packet: NetworkPacket) {
        val data = packet.binaryData ?: return
        val me   = myUser ?: return
        val decryptedData = tryDecryptBytes(data, packet.senderId) ?: data
        val file = context.filesDir.resolve("images/img_${packet.id}.jpg")
            .also { it.parentFile?.mkdirs() }
        file.writeBytes(decryptedData)
        val chatId = chatRepo.getOrCreateDirectChat(
            me.id, me.nickname, packet.senderId, packet.senderNickname)
        val message = Message(
            id             = packet.id,
            chatId         = chatId,
            senderId       = packet.senderId,
            senderNickname = packet.senderNickname,
            type           = MessageType.IMAGE,
            mediaPath      = file.absolutePath,
            status         = MessageStatus.DELIVERED,
            transport      = packet.transport,
            sentAt         = packet.timestamp
        )
        messageRepo.sendMessage(message)
        _lastReceivedMessage.emit(message)
        sendStatusUpdate(packet.id, "DELIVERED")
    }

    private suspend fun handleFile(packet: NetworkPacket) {
        val data    = packet.binaryData ?: return
        val payload = try {
            PacketSerializer.fromJson(packet.payload, FileInfoPayload::class.java)
        } catch (_: Exception) { return }
        val me = myUser ?: return
        val decryptedData = tryDecryptBytes(data, packet.senderId) ?: data
        val file = context.filesDir.resolve("files/${payload.fileName}")
            .also { it.parentFile?.mkdirs() }
        file.writeBytes(decryptedData)
        val chatId = chatRepo.getOrCreateDirectChat(
            me.id, me.nickname, packet.senderId, packet.senderNickname)
        val message = Message(
            id             = packet.id,
            chatId         = chatId,
            senderId       = packet.senderId,
            senderNickname = packet.senderNickname,
            type           = MessageType.FILE,
            mediaPath      = file.absolutePath,
            mediaName      = payload.fileName,
            mediaSizeBytes = payload.fileSizeBytes,
            status         = MessageStatus.DELIVERED,
            transport      = packet.transport,
            sentAt         = packet.timestamp
        )
        messageRepo.sendMessage(message)
        _lastReceivedMessage.emit(message)
        sendStatusUpdate(packet.id, "DELIVERED")
    }

    private fun handleTyping(packet: NetworkPacket) {
        val payload = try {
            PacketSerializer.fromJson(packet.payload, TypingPayload::class.java)
        } catch (_: Exception) { return }
        val cur = _typingStates.value.toMutableMap()
        cur[payload.chatId] = payload.isTyping
        _typingStates.value = cur
        if (payload.isTyping) scope.launch {
            delay(3000)
            val map = _typingStates.value.toMutableMap()
            map[payload.chatId] = false
            _typingStates.value = map
        }
    }

    // ── Исходящие ────────────────────────────────────────────────────

    suspend fun sendHandshake() {
        val me = myUser ?: return
        val pubKey = keyStorage.getMyPublicKeyBase64()
        transportManager.send(NetworkPacket(
            type           = PacketType.HANDSHAKE,
            senderId       = me.id,
            senderNickname = me.nickname,
            payload        = PacketSerializer.toJson(
                HandshakePayload(me.id, me.nickname, me.avatarColor, pubKey)
            )
        ))
    }

    suspend fun sendTextMessage(message: Message) {
        val me = myUser ?: return
        val payload = MessagePayload(
            message.id, message.chatId, message.text,
            message.replyToMessageId, message.sentAt
        )
        // Шифруем payload если есть ключ
        val payloadJson    = PacketSerializer.toJson(payload)
        val encryptedPayload = tryEncrypt(payloadJson, getRecipientId(message.chatId)) ?: payloadJson

        val sent = transportManager.send(NetworkPacket(
            type           = PacketType.MESSAGE,
            senderId       = me.id,
            senderNickname = me.nickname,
            chatId         = message.chatId,
            payload        = encryptedPayload,
            transport      = message.transport
        ))
        messageRepo.updateStatus(message.id,
            if (sent) MessageStatus.SENT else MessageStatus.FAILED)
    }

    suspend fun retryMessage(message: Message) {
        when (message.type) {
            MessageType.TEXT  -> sendTextMessage(message)
            MessageType.VOICE -> sendVoiceMessage(message)
            MessageType.IMAGE -> sendImageMessage(message)
            MessageType.FILE  -> sendFileMessage(message)
            else -> {}
        }
    }

    suspend fun sendVoiceMessage(message: Message) {
        val me   = myUser ?: return
        val file = message.mediaPath?.let { java.io.File(it) }?.takeIf { it.exists() } ?: return
        val data = file.readBytes()
        val encData = tryEncryptBytes(data, getRecipientId(message.chatId)) ?: data
        val sent = transportManager.send(NetworkPacket(
            type           = PacketType.VOICE,
            senderId       = me.id,
            senderNickname = me.nickname,
            chatId         = message.chatId,
            binaryData     = encData,
            transport      = message.transport
        ))
        messageRepo.updateStatus(message.id,
            if (sent) MessageStatus.SENT else MessageStatus.FAILED)
    }

    suspend fun sendImageMessage(message: Message) {
        val me   = myUser ?: return
        val file = message.mediaPath?.let { java.io.File(it) }?.takeIf { it.exists() } ?: return
        val data = file.readBytes()
        val encData = tryEncryptBytes(data, getRecipientId(message.chatId)) ?: data
        val sent = transportManager.send(NetworkPacket(
            type           = PacketType.IMAGE,
            senderId       = me.id,
            senderNickname = me.nickname,
            chatId         = message.chatId,
            binaryData     = encData,
            transport      = message.transport
        ))
        messageRepo.updateStatus(message.id,
            if (sent) MessageStatus.SENT else MessageStatus.FAILED)
    }

    suspend fun sendFileMessage(message: Message) {
        val me   = myUser ?: return
        val file = message.mediaPath?.let { java.io.File(it) }?.takeIf { it.exists() } ?: return
        val data = file.readBytes()
        val encData = tryEncryptBytes(data, getRecipientId(message.chatId)) ?: data
        val payload = FileInfoPayload(
            message.id, message.mediaName ?: file.name,
            file.length(), "application/octet-stream", 1
        )
        val sent = transportManager.send(NetworkPacket(
            type           = PacketType.FILE_INFO,
            senderId       = me.id,
            senderNickname = me.nickname,
            chatId         = message.chatId,
            payload        = PacketSerializer.toJson(payload),
            binaryData     = encData,
            transport      = message.transport
        ))
        messageRepo.updateStatus(message.id,
            if (sent) MessageStatus.SENT else MessageStatus.FAILED)
    }

    suspend fun sendTyping(chatId: String, isTyping: Boolean) {
        val me = myUser ?: return
        transportManager.send(NetworkPacket(
            type           = PacketType.TYPING,
            senderId       = me.id,
            senderNickname = me.nickname,
            chatId         = chatId,
            payload        = PacketSerializer.toJson(TypingPayload(chatId, isTyping))
        ))
    }

    private suspend fun sendStatusUpdate(messageId: String, status: String) {
        val me = myUser ?: return
        transportManager.send(NetworkPacket(
            type           = PacketType.MESSAGE_STATUS,
            senderId       = me.id,
            senderNickname = me.nickname,
            payload        = PacketSerializer.toJson(MessageStatusPayload(messageId, status))
        ))
    }

    // ── Шифрование ───────────────────────────────────────────────────

    private fun tryEncrypt(text: String, recipientId: String?): String? {
        if (recipientId == null) return null
        val key = keyStorage.getSessionKey(recipientId) ?: return null
        return try { CryptoManager.encrypt(text, key) } catch (_: Exception) { null }
    }

    private fun tryDecrypt(text: String, senderId: String): String? {
        val key = keyStorage.getSessionKey(senderId) ?: return null
        return try { CryptoManager.decrypt(text, key) } catch (_: Exception) { null }
    }

    private fun tryEncryptBytes(data: ByteArray, recipientId: String?): ByteArray? {
        if (recipientId == null) return null
        val key = keyStorage.getSessionKey(recipientId) ?: return null
        return try { CryptoManager.encryptBytes(data, key) } catch (_: Exception) { null }
    }

    private fun tryDecryptBytes(data: ByteArray, senderId: String): ByteArray? {
        val key = keyStorage.getSessionKey(senderId) ?: return null
        return try { CryptoManager.decryptBytes(data, key) } catch (_: Exception) { null }
    }

    private suspend fun getRecipientId(chatId: String): String? {
        val me = myUser ?: return null
        val members = chatRepo.getMembersOnce(chatId)
        return members.firstOrNull { it.userId != me.id }?.userId
    }
}
