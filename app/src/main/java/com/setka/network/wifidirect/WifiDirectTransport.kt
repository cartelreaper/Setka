package com.setka.network.wifidirect

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.p2p.*
import androidx.core.content.ContextCompat
import com.setka.network.transport.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.IOException
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket

class WifiDirectTransport(private val context: Context) : ITransport {

    companion object {
        const val PORT = 8989
        const val TIMEOUT_MS = 10_000
    }

    private val wifiP2pManager: WifiP2pManager? =
        context.getSystemService(Context.WIFI_P2P_SERVICE) as? WifiP2pManager
    private var channel: WifiP2pManager.Channel? = null

    private val _state = MutableStateFlow<TransportState>(TransportState.Idle)
    override val state: StateFlow<TransportState> = _state.asStateFlow()

    private val _incoming = MutableSharedFlow<NetworkPacket>(extraBufferCapacity = 64)
    override val incomingPackets: Flow<NetworkPacket> = _incoming.asSharedFlow()

    private val _peers = MutableStateFlow<List<DiscoveredPeer>>(emptyList())
    override val discoveredPeers: StateFlow<List<DiscoveredPeer>> = _peers.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var serverSocket: ServerSocket? = null
    private var clientSocket: Socket? = null
    private var readJob: Job? = null

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            when (intent.action) {
                WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> requestPeers()
                WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                    val info = intent.getParcelableExtra<WifiP2pInfo>(WifiP2pManager.EXTRA_WIFI_P2P_INFO)
                    info?.let { handleConnectionInfo(it) }
                }
                WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                    val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
                    if (state != WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                        _state.value = TransportState.Error("WiFi Direct недоступен")
                    }
                }
            }
        }
    }

    init {
        channel = wifiP2pManager?.initialize(context, context.mainLooper, null)
        val filter = IntentFilter().apply {
            addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        }
        context.registerReceiver(receiver, filter)
    }

    override fun startDiscovery() {
        if (!hasPermission()) return
        _state.value = TransportState.Scanning
        wifiP2pManager?.discoverPeers(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {}
            override fun onFailure(reason: Int) {
                _state.value = TransportState.Error("Ошибка поиска WiFi Direct: $reason")
            }
        })
    }

    override fun stopDiscovery() {
        wifiP2pManager?.stopPeerDiscovery(channel, null)
        if (_state.value is TransportState.Scanning) {
            _state.value = TransportState.Idle
        }
    }

    override fun startListening() {
        _state.value = TransportState.Listening
        scope.launch {
            try {
                serverSocket = ServerSocket(PORT)
                val socket = serverSocket?.accept() ?: return@launch
                clientSocket = socket
                startReading(socket)
            } catch (e: IOException) {
                if (_state.value is TransportState.Listening) {
                    _state.value = TransportState.Error("Ошибка сервера WiFi: ${e.message}")
                }
            }
        }
    }

    override suspend fun connect(peer: DiscoveredPeer): Boolean {
        if (!hasPermission()) return false
        _state.value = TransportState.Connecting(peer.deviceName)

        // Ищем WifiP2pDevice по имени
        return suspendCancellableCoroutine { cont ->
            wifiP2pManager?.requestPeers(channel) { peerList ->
                val device = peerList.deviceList.find { it.deviceAddress == peer.deviceId }
                if (device == null) {
                    cont.resume(false) {}
                    return@requestPeers
                }
                val config = WifiP2pConfig().apply { deviceAddress = device.deviceAddress }
                wifiP2pManager.connect(channel, config, object : WifiP2pManager.ActionListener {
                    override fun onSuccess() { cont.resume(true) {} }
                    override fun onFailure(reason: Int) {
                        _state.value = TransportState.Error("Не удалось подключиться: $reason")
                        cont.resume(false) {}
                    }
                })
            }
        }
    }

    override suspend fun send(packet: NetworkPacket): Boolean {
        val socket = clientSocket ?: return false
        return withContext(Dispatchers.IO) {
            try {
                val data = PacketSerializer.serialize(packet)
                socket.outputStream.write(data)
                socket.outputStream.flush()
                true
            } catch (e: IOException) {
                false
            }
        }
    }

    override fun disconnect() {
        readJob?.cancel()
        try { clientSocket?.close() } catch (_: IOException) {}
        try { serverSocket?.close() } catch (_: IOException) {}
        wifiP2pManager?.removeGroup(channel, null)
        clientSocket = null
        serverSocket = null
        _state.value = TransportState.Disconnected
    }

    override fun release() {
        disconnect()
        scope.cancel()
        try { context.unregisterReceiver(receiver) } catch (_: Exception) {}
    }

    override fun isAvailable(): Boolean {
        return wifiP2pManager != null
    }

    // ── Приватные методы ──────────────────────────────────────────────

    private fun requestPeers() {
        if (!hasPermission()) return
        wifiP2pManager?.requestPeers(channel) { peerList ->
            val peers = peerList.deviceList.map { device ->
                DiscoveredPeer(
                    deviceId = device.deviceAddress,
                    deviceName = device.deviceName
                )
            }
            _peers.value = peers
        }
    }

    private fun handleConnectionInfo(info: WifiP2pInfo) {
        if (!info.groupFormed) return
        if (info.isGroupOwner) {
            // Мы хост — слушаем входящее подключение
            startListening()
        } else {
            // Мы клиент — подключаемся к хосту
            val hostAddress = info.groupOwnerAddress.hostAddress ?: return
            scope.launch {
                try {
                    val socket = Socket()
                    socket.connect(InetSocketAddress(hostAddress, PORT), TIMEOUT_MS)
                    clientSocket = socket
                    _state.value = TransportState.Connected(
                        peerId = hostAddress,
                        peerNickname = hostAddress,
                        deviceName = hostAddress
                    )
                    startReading(socket)
                } catch (e: IOException) {
                    _state.value = TransportState.Error("Ошибка подключения к хосту: ${e.message}")
                }
            }
        }
    }

    private fun startReading(socket: Socket) {
        readJob?.cancel()
        readJob = scope.launch {
            val buffer = ByteArray(4096)
            val accumulator = mutableListOf<Byte>()

            while (isActive) {
                try {
                    val bytesRead = socket.inputStream.read(buffer)
                    if (bytesRead < 0) break
                    accumulator.addAll(buffer.take(bytesRead).toTypedArray())

                    while (accumulator.size >= 4) {
                        val packetLen = bytesToInt(accumulator.take(4).toByteArray())
                        val totalLen = 4 + packetLen
                        if (accumulator.size < totalLen) break
                        val bytes = accumulator.take(totalLen).toByteArray()
                        repeat(totalLen) { accumulator.removeAt(0) }
                        val packet = PacketSerializer.deserialize(bytes)
                        if (packet != null) _incoming.emit(packet)
                    }
                } catch (e: IOException) { break }
            }
            if (_state.value is TransportState.Connected) {
                _state.value = TransportState.Disconnected
            }
        }
    }

    private fun hasPermission() =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED

    private fun bytesToInt(bytes: ByteArray): Int =
        (bytes[0].toInt() and 0xFF shl 24) or
        (bytes[1].toInt() and 0xFF shl 16) or
        (bytes[2].toInt() and 0xFF shl 8) or
        (bytes[3].toInt() and 0xFF)
}
