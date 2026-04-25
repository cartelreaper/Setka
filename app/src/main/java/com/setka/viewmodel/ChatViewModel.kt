package com.setka.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.setka.data.db.SetkaDatabase
import com.setka.data.model.*
import com.setka.data.repository.*
import com.setka.network.transport.*
import com.setka.notifications.NotificationHelper
import com.setka.security.KeyStorage
import com.setka.ui.components.MediaHelper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

class ChatViewModel(app: Application) : AndroidViewModel(app) {

    companion object {
        const val APP_VERSION = "0.2.0"
        const val BUILD_DATE  = "2024"
    }

    private val db = SetkaDatabase.getInstance(app)
    val messageRepo  = MessageRepository(db)
    val chatRepo     = ChatRepository(db)
    val contactRepo  = ContactRepository(db)
    val userRepo     = UserRepository(app, db)
    val statsRepo    = StatsRepository(db)
    val keyStorage   = KeyStorage(app)

    val transportManager = TransportManager(app)
    val networkService   = NetworkService(
        app, transportManager, messageRepo, chatRepo, contactRepo, userRepo
    )

    val allChats        = chatRepo.getAllChats()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val allContacts     = contactRepo.getAllContacts()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val discoveredPeers = transportManager.discoveredPeers
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val btState         = transportManager.bluetoothState
        .stateIn(viewModelScope, SharingStarted.Lazily, TransportState.Idle)
    val wifiState       = transportManager.wifiDirectState
        .stateIn(viewModelScope, SharingStarted.Lazily, TransportState.Idle)
    val internetState   = transportManager.internetState
        .stateIn(viewModelScope, SharingStarted.Lazily, TransportState.Idle)
    val typingStates    = networkService.typingStates
    val myStats         = statsRepo.getMyStats()
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    private val _myUser = MutableStateFlow<User?>(null)
    val myUser: StateFlow<User?> = _myUser.asStateFlow()

    private val _isFirstLaunch = MutableStateFlow(false)
    val isFirstLaunch: StateFlow<Boolean> = _isFirstLaunch.asStateFlow()

    // Статус подключения для UI
    private val _connectionStatus = MutableStateFlow("")
    val connectionStatus: StateFlow<String> = _connectionStatus.asStateFlow()

    init {
        NotificationHelper.createChannel(app)
        viewModelScope.launch {
            val user = userRepo.getMyUser()
            if (user.nickname.startsWith("User_")) _isFirstLaunch.value = true
            _myUser.value = user
            transportManager.initMesh(user.id)
            keyStorage.getOrCreateMyKeyPair()
            networkService.start()
            listenForNotifications()
            listenConnectionStatus()
            transportManager.connectInternet(user.id)
        }
    }

    // ── Статус подключения ───────────────────────────────────────────

    private fun listenConnectionStatus() {
        viewModelScope.launch {
            combine(btState, wifiState) { bt, wifi -> Pair(bt, wifi) }.collect { (bt, wifi) ->
                _connectionStatus.value = when {
                    wifi is TransportState.Connected -> "WiFi Direct"
                    bt   is TransportState.Connected -> "Bluetooth"
                    wifi is TransportState.Listening ||
                    bt   is TransportState.Listening -> "Ожидание подключения..."
                    wifi is TransportState.Scanning  ||
                    bt   is TransportState.Scanning  -> "Поиск устройств..."
                    wifi is TransportState.Error     -> wifi.message
                    bt   is TransportState.Error     -> bt.message
                    else -> ""
                }
            }
        }
    }

    // ── Профиль ──────────────────────────────────────────────────────

    fun createProfile(nickname: String, onDone: () -> Unit) {
        viewModelScope.launch {
            userRepo.updateNickname(nickname)
            _myUser.value = userRepo.getMyUser()
            _isFirstLaunch.value = false
            onDone()
        }
    }

    fun updateNickname(nickname: String) {
        viewModelScope.launch {
            userRepo.updateNickname(nickname)
            _myUser.value = userRepo.getMyUser()
        }
    }

    // ── Сообщения ────────────────────────────────────────────────────

    fun getMessages(chatId: String) = messageRepo.getMessages(chatId)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun getUnreadCount(chatId: String) = messageRepo
        .getUnreadCount(chatId, _myUser.value?.id ?: "")
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    fun sendTextMessage(chatId: String, text: String) {
        val me = _myUser.value ?: return
        if (text.isBlank()) return
        viewModelScope.launch {
            val message = Message(
                id             = UUID.randomUUID().toString(),
                chatId         = chatId,
                senderId       = me.id,
                senderNickname = me.nickname,
                type           = MessageType.TEXT,
                text           = text.trim(),
                status         = MessageStatus.SENDING,
                transport      = currentTransport()
            )
            messageRepo.sendMessage(message)
            networkService.sendTextMessage(message)
            statsRepo.onMessageSent()
            statsRepo.checkNightOwl()
        }
    }

    fun sendVoiceMsg(chatId: String, file: File, durationMs: Long) {
        val me = _myUser.value ?: return
        viewModelScope.launch {
            val message = Message(
                id              = UUID.randomUUID().toString(),
                chatId          = chatId,
                senderId        = me.id,
                senderNickname  = me.nickname,
                type            = MessageType.VOICE,
                mediaPath       = file.absolutePath,
                voiceDurationMs = durationMs,
                status          = MessageStatus.SENDING,
                transport       = currentTransport()
            )
            messageRepo.sendMessage(message)
            networkService.sendVoiceMessage(message)
            statsRepo.onMessageSent()
        }
    }

    fun sendImageMessage(chatId: String, uri: Uri, context: Context) {
        val me = _myUser.value ?: return
        viewModelScope.launch {
            val file = MediaHelper.copyToInternal(context, uri, "images") ?: return@launch
            val message = Message(
                id             = UUID.randomUUID().toString(),
                chatId         = chatId,
                senderId       = me.id,
                senderNickname = me.nickname,
                type           = MessageType.IMAGE,
                mediaPath      = file.absolutePath,
                mediaName      = file.name,
                mediaSizeBytes = file.length(),
                status         = MessageStatus.SENDING,
                transport      = currentTransport()
            )
            messageRepo.sendMessage(message)
            networkService.sendImageMessage(message)
            statsRepo.onMessageSent()
        }
    }

    fun sendFileMessage(chatId: String, uri: Uri, context: Context) {
        val me = _myUser.value ?: return
        viewModelScope.launch {
            val file = MediaHelper.copyToInternal(context, uri, "files") ?: return@launch
            val name = MediaHelper.getFileName(context, uri) ?: file.name
            val message = Message(
                id             = UUID.randomUUID().toString(),
                chatId         = chatId,
                senderId       = me.id,
                senderNickname = me.nickname,
                type           = MessageType.FILE,
                mediaPath      = file.absolutePath,
                mediaName      = name,
                mediaSizeBytes = file.length(),
                status         = MessageStatus.SENDING,
                transport      = currentTransport()
            )
            messageRepo.sendMessage(message)
            networkService.sendFileMessage(message)
            statsRepo.onMessageSent()
        }
    }

    fun markAllRead(chatId: String) {
        viewModelScope.launch {
            messageRepo.markAllRead(chatId, _myUser.value?.id ?: "")
            NotificationHelper.cancelNotification(getApplication(), chatId)
        }
    }

    fun sendTyping(chatId: String, isTyping: Boolean) {
        viewModelScope.launch { networkService.sendTyping(chatId, isTyping) }
    }

    fun retryMessage(message: Message) {
        viewModelScope.launch {
            messageRepo.updateStatus(message.id, MessageStatus.SENDING)
            networkService.retryMessage(message)
        }
    }

    fun deleteMessage(messageId: String) {
        viewModelScope.launch { messageRepo.deleteMessage(messageId) }
    }

    // ── Чаты ─────────────────────────────────────────────────────────

    fun setPinned(chatId: String, pinned: Boolean) {
        viewModelScope.launch { chatRepo.setPinned(chatId, pinned) }
    }

    fun setMuted(chatId: String, muted: Boolean) {
        viewModelScope.launch { chatRepo.setMuted(chatId, muted) }
    }

    fun deleteChat(chatId: String) {
        viewModelScope.launch { chatRepo.deleteChat(chatId) }
    }

    // ── Транспорт ────────────────────────────────────────────────────

    fun startDiscovery()  = transportManager.startDiscovery()
    fun stopDiscovery()   = transportManager.stopDiscovery()
    fun startListening()  = transportManager.startListening()

    fun connectToPeer(peer: DiscoveredPeer) {
        viewModelScope.launch {
            val ok = transportManager.connect(peer)
            if (ok) {
                networkService.sendHandshake()
                statsRepo.onNewConnection()
            }
        }
    }

    fun disconnect() = transportManager.disconnect()

    // ── Уведомления ──────────────────────────────────────────────────

    private fun listenForNotifications() {
        viewModelScope.launch {
            networkService.lastReceivedMessage.collect { msg ->
                val me = _myUser.value ?: return@collect
                if (msg.senderId == me.id) return@collect
                val preview = when (msg.type) {
                    MessageType.TEXT  -> msg.text
                    MessageType.IMAGE -> "Фото"
                    MessageType.VOICE -> "Голосовое сообщение"
                    MessageType.FILE  -> msg.mediaName ?: "Файл"
                    else -> ""
                }
                NotificationHelper.showMessageNotification(
                    context     = getApplication(),
                    chatId      = msg.chatId,
                    senderName  = msg.senderNickname,
                    messageText = preview
                )
            }
        }
    }

    private fun currentTransport(): TransportType = when (transportManager.activeTransport.value) {
        TransportType.WIFI_DIRECT -> TransportType.WIFI_DIRECT
        TransportType.INTERNET    -> TransportType.INTERNET
        else                      -> TransportType.BLUETOOTH
    }

    override fun onCleared() {
        super.onCleared()
        networkService.stop()
        transportManager.release()
    }
}
