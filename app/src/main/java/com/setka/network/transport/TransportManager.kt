package com.setka.network.transport

import android.content.Context
import com.setka.data.model.TransportType
import com.setka.network.bluetooth.BluetoothTransport
import com.setka.network.internet.InternetTransport
import com.setka.network.mesh.MeshRouter
import com.setka.network.wifidirect.WifiDirectTransport
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class TransportManager(context: Context) {

    private val btTransport     = BluetoothTransport(context)
    private val wifiTransport   = WifiDirectTransport(context)
    val internetTransport        = InternetTransport()

    private var meshRouter: MeshRouter? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Все пакеты из всех транспортов
    val incomingPackets: Flow<NetworkPacket> = merge(
        btTransport.incomingPackets,
        wifiTransport.incomingPackets,
        internetTransport.incomingPackets
    )

    // Найденные устройства (BT + WiFi)
    val discoveredPeers: Flow<List<DiscoveredPeer>> = combine(
        btTransport.discoveredPeers,
        wifiTransport.discoveredPeers
    ) { bt, wifi -> (bt + wifi).distinctBy { it.deviceId } }

    val bluetoothState: Flow<TransportState>  = btTransport.state
    val wifiDirectState: Flow<TransportState> = wifiTransport.state
    val internetState: Flow<TransportState>   = internetTransport.state

    private val _activeTransport = MutableStateFlow<TransportType?>(null)
    val activeTransport: StateFlow<TransportType?> = _activeTransport.asStateFlow()

    // ── Инициализация mesh ───────────────────────────────────────────

    fun initMesh(myUserId: String) {
        val router = MeshRouter(myUserId)
        meshRouter = router

        // Пересылаем mesh-пакеты через все доступные транспорты
        scope.launch {
            router.forwardPackets.collect { packet ->
                sendDirect(packet)
            }
        }
    }

    // ── Поиск / подключение ──────────────────────────────────────────

    fun startDiscovery() {
        if (btTransport.isAvailable())   btTransport.startDiscovery()
        if (wifiTransport.isAvailable()) wifiTransport.startDiscovery()
    }

    fun stopDiscovery() {
        btTransport.stopDiscovery()
        wifiTransport.stopDiscovery()
    }

    fun startListening() {
        if (wifiTransport.isAvailable()) wifiTransport.startListening()
        if (btTransport.isAvailable())   btTransport.startListening()
    }

    fun connectInternet(userId: String) {
        internetTransport.connect(userId)
    }

    suspend fun connect(peer: DiscoveredPeer): Boolean {
        if (wifiTransport.isAvailable()) {
            if (wifiTransport.connect(peer)) {
                _activeTransport.value = TransportType.WIFI_DIRECT
                return true
            }
        }
        if (btTransport.isAvailable()) {
            if (btTransport.connect(peer)) {
                _activeTransport.value = TransportType.BLUETOOTH
                return true
            }
        }
        return false
    }

    // ── Отправка ─────────────────────────────────────────────────────

    /**
     * Умная отправка:
     * 1. WiFi Direct (быстро, далеко)
     * 2. Bluetooth (fallback локально)
     * 3. Internet (глобально)
     */
    suspend fun send(packet: NetworkPacket): Boolean {
        // Через mesh если инициализирован и нужна маршрутизация
        val mesh = meshRouter
        if (mesh != null && packet.payload.contains("targetUserId")) {
            mesh.route(packet)
            return true
        }
        return sendDirect(packet)
    }

    private suspend fun sendDirect(packet: NetworkPacket): Boolean {
        val active = _activeTransport.value

        // Сначала активный транспорт
        val primary: ITransport? = when (active) {
            TransportType.WIFI_DIRECT -> wifiTransport
            TransportType.BLUETOOTH   -> btTransport
            TransportType.INTERNET    -> internetTransport
            else -> null
        }

        if (primary != null && primary.send(packet)) return true

        // Fallback порядок: WiFi → BT → Internet
        val fallbacks = listOf(wifiTransport, btTransport, internetTransport)
            .filter { it != primary }

        for (transport in fallbacks) {
            if (transport.send(packet)) {
                _activeTransport.value = when (transport) {
                    is WifiDirectTransport -> TransportType.WIFI_DIRECT
                    is BluetoothTransport  -> TransportType.BLUETOOTH
                    is InternetTransport   -> TransportType.INTERNET
                    else -> null
                }
                return true
            }
        }
        return false
    }

    // ── Состояние ────────────────────────────────────────────────────

    fun disconnect() {
        btTransport.disconnect()
        wifiTransport.disconnect()
        internetTransport.disconnect()
        _activeTransport.value = null
    }

    fun release() {
        btTransport.release()
        wifiTransport.release()
        internetTransport.release()
        scope.cancel()
    }

    fun isAnyConnected(): Boolean {
        return btTransport.state.value is TransportState.Connected ||
               wifiTransport.state.value is TransportState.Connected ||
               internetTransport.state.value is TransportState.Connected
    }
}
