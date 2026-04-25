package com.setka.network.internet

import com.setka.network.transport.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket
import javax.net.ssl.SSLSocketFactory

/**
 * InternetTransport — TCP/TLS соединение с Setka-сервером.
 * Протокол: каждое сообщение = одна строка JSON + \n
 *
 * Когда поднимем сервер — меняем SERVER_HOST на реальный адрес.
 */
class InternetTransport : ITransport {

    companion object {
        var SERVER_HOST = "setka.app"   // заменить на реальный хост
        var SERVER_PORT = 8443          // TLS порт
        const val RECONNECT_DELAY_MS = 5_000L
    }

    private val _state = MutableStateFlow<TransportState>(TransportState.Idle)
    override val state: StateFlow<TransportState> = _state.asStateFlow()

    private val _incoming = MutableSharedFlow<NetworkPacket>(extraBufferCapacity = 128)
    override val incomingPackets: Flow<NetworkPacket> = _incoming.asSharedFlow()

    private val _peers = MutableStateFlow<List<DiscoveredPeer>>(emptyList())
    override val discoveredPeers: StateFlow<List<DiscoveredPeer>> = _peers.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var socket: Socket? = null
    private var writer: PrintWriter? = null
    private var readJob: Job? = null
    private var myUserId: String = ""

    // ── ITransport ───────────────────────────────────────────────────

    override fun startDiscovery() { /* через сервер — не нужно */ }
    override fun stopDiscovery()  { /* через сервер — не нужно */ }
    override fun startListening() { connect() }

    override suspend fun connect(peer: DiscoveredPeer): Boolean {
        connect()
        return _state.value is TransportState.Connected
    }

    override suspend fun send(packet: NetworkPacket): Boolean {
        val w = writer ?: return false
        return withContext(Dispatchers.IO) {
            try {
                val json = PacketSerializer.toJson(packet)
                w.println(json)
                w.flush()
                !w.checkError()
            } catch (e: Exception) { false }
        }
    }

    override fun disconnect() {
        readJob?.cancel()
        try { socket?.close() } catch (_: Exception) {}
        socket = null
        writer = null
        _state.value = TransportState.Disconnected
    }

    override fun release() {
        disconnect()
        scope.cancel()
    }

    override fun isAvailable(): Boolean = true  // интернет считаем всегда потенциально доступным

    // ── Подключение к серверу ────────────────────────────────────────

    fun connect(userId: String = myUserId) {
        if (userId.isNotBlank()) myUserId = userId
        if (_state.value is TransportState.Connected ||
            _state.value is TransportState.Connecting) return

        scope.launch {
            _state.value = TransportState.Connecting("Setka Server")
            try {
                val factory = SSLSocketFactory.getDefault()
                val sock    = factory.createSocket(SERVER_HOST, SERVER_PORT) as Socket
                socket = sock
                writer = PrintWriter(sock.outputStream, true)

                // Авторизуемся на сервере
                if (myUserId.isNotBlank()) {
                    writer?.println(PacketSerializer.toJson(
                        NetworkPacket(
                            type           = PacketType.HANDSHAKE,
                            senderId       = myUserId,
                            senderNickname = myUserId,
                            payload        = """{"userId":"$myUserId"}"""
                        )
                    ))
                }

                _state.value = TransportState.Connected(
                    peerId       = "server",
                    peerNickname = "Setka Server",
                    deviceName   = SERVER_HOST
                )
                startReading(sock)
            } catch (e: Exception) {
                _state.value = TransportState.Error("Нет связи с сервером: ${e.message}")
                // Автоматический переподключение через 5 секунд
                delay(RECONNECT_DELAY_MS)
                if (_state.value is TransportState.Error) {
                    _state.value = TransportState.Idle
                }
            }
        }
    }

    private fun startReading(sock: Socket) {
        readJob?.cancel()
        readJob = scope.launch {
            try {
                val reader = BufferedReader(InputStreamReader(sock.inputStream))
                while (isActive) {
                    val line = reader.readLine() ?: break
                    if (line.isBlank()) continue
                    try {
                        val packet = PacketSerializer.fromJson(line, NetworkPacket::class.java)
                        _incoming.emit(packet)
                    } catch (_: Exception) {}
                }
            } catch (_: Exception) {}

            if (_state.value is TransportState.Connected) {
                _state.value = TransportState.Disconnected
                // Переподключаемся автоматически
                delay(RECONNECT_DELAY_MS)
                connect()
            }
        }
    }
}
