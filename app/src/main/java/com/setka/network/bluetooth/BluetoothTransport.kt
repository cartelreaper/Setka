package com.setka.network.bluetooth

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.setka.network.transport.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.IOException
import java.util.UUID

class BluetoothTransport(private val context: Context) : ITransport {

    companion object {
        val SERVICE_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        const val SERVICE_NAME   = "Setka"
        const val HEADER_SIZE    = 4
        const val MAX_CHUNK      = 4096
        const val HEARTBEAT_MS   = 15_000L  // каждые 15 сек
    }

    private val btManager = context.getSystemService(BluetoothManager::class.java)
    private val btAdapter: BluetoothAdapter? = btManager?.adapter

    private val _state = MutableStateFlow<TransportState>(TransportState.Idle)
    override val state: StateFlow<TransportState> = _state.asStateFlow()

    private val _incoming = MutableSharedFlow<NetworkPacket>(extraBufferCapacity = 64)
    override val incomingPackets: Flow<NetworkPacket> = _incoming.asSharedFlow()

    private val _peers = MutableStateFlow<List<DiscoveredPeer>>(emptyList())
    override val discoveredPeers: StateFlow<List<DiscoveredPeer>> = _peers.asStateFlow()

    private val scope       = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var serverSocket: BluetoothServerSocket? = null
    private var clientSocket: BluetoothSocket? = null
    private var readJob: Job? = null
    private var heartbeatJob: Job? = null

    private val deviceReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= 33) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }
                    val rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, 0).toInt()
                    device?.let { addPeer(it, rssi) }
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    if (_state.value is TransportState.Scanning) _state.value = TransportState.Idle
                }
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1)
                    if (state == BluetoothAdapter.STATE_OFF) {
                        disconnect()
                        _state.value = TransportState.Error("Bluetooth выключен")
                    }
                }
            }
        }
    }

    init {
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        }
        context.registerReceiver(deviceReceiver, filter)
    }

    override fun startDiscovery() {
        if (!checkPermission(Manifest.permission.BLUETOOTH_SCAN)) return
        _peers.value = emptyList()
        btAdapter?.bondedDevices?.forEach { addPeer(it, -50) }
        btAdapter?.startDiscovery()
        _state.value = TransportState.Scanning
    }

    override fun stopDiscovery() {
        if (!checkPermission(Manifest.permission.BLUETOOTH_SCAN)) return
        btAdapter?.cancelDiscovery()
        if (_state.value is TransportState.Scanning) _state.value = TransportState.Idle
    }

    override fun startListening() {
        // ── Фикс краша: проверяем разрешение до любых BT операций ──
        if (!checkPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
            _state.value = TransportState.Error("Нет разрешения Bluetooth. Выдайте разрешение в настройках приложения.")
            return
        }
        if (btAdapter?.isEnabled != true) {
            _state.value = TransportState.Error("Bluetooth выключен. Включите Bluetooth и попробуйте снова.")
            return
        }

        _state.value = TransportState.Listening
        scope.launch {
            try {
                serverSocket = btAdapter.listenUsingRfcommWithServiceRecord(SERVICE_NAME, SERVICE_UUID)
                val socket = serverSocket?.accept() ?: return@launch
                serverSocket?.close()
                handleConnection(socket)
            } catch (e: IOException) {
                if (_state.value is TransportState.Listening) {
                    _state.value = TransportState.Error("Ошибка ожидания: ${e.message}")
                }
            } catch (e: SecurityException) {
                _state.value = TransportState.Error("Нет разрешения Bluetooth: ${e.message}")
            }
        }
    }

    override suspend fun connect(peer: DiscoveredPeer): Boolean {
        if (!checkPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
            _state.value = TransportState.Error("Нет разрешения Bluetooth")
            return false
        }
        stopDiscovery()
        _state.value = TransportState.Connecting(peer.deviceName)
        return withContext(Dispatchers.IO) {
            try {
                val device = btAdapter?.getRemoteDevice(peer.deviceId) ?: return@withContext false
                val socket = device.createRfcommSocketToServiceRecord(SERVICE_UUID)
                socket.connect()
                handleConnection(socket)
                true
            } catch (e: IOException) {
                _state.value = TransportState.Error("Не удалось подключиться: ${e.message}")
                false
            } catch (e: SecurityException) {
                _state.value = TransportState.Error("Нет разрешения: ${e.message}")
                false
            }
        }
    }

    override suspend fun send(packet: NetworkPacket): Boolean {
        val socket = clientSocket ?: return false
        if (!socket.isConnected) return false
        return withContext(Dispatchers.IO) {
            try {
                val data = PacketSerializer.serialize(packet)
                socket.outputStream.write(data)
                socket.outputStream.flush()
                true
            } catch (e: IOException) {
                _state.value = TransportState.Error("Ошибка отправки")
                false
            }
        }
    }

    override fun disconnect() {
        heartbeatJob?.cancel()
        readJob?.cancel()
        try { clientSocket?.close() } catch (_: IOException) {}
        try { serverSocket?.close() } catch (_: IOException) {}
        clientSocket = null
        _state.value = TransportState.Disconnected
    }

    override fun release() {
        disconnect()
        scope.cancel()
        try { context.unregisterReceiver(deviceReceiver) } catch (_: Exception) {}
    }

    override fun isAvailable(): Boolean =
        btAdapter?.isEnabled == true &&
        checkPermission(Manifest.permission.BLUETOOTH_CONNECT)

    // ── Приватные методы ─────────────────────────────────────────────

    private fun handleConnection(socket: BluetoothSocket) {
        clientSocket = socket
        val deviceName = try {
            if (checkPermission(Manifest.permission.BLUETOOTH_CONNECT))
                socket.remoteDevice.name ?: socket.remoteDevice.address
            else socket.remoteDevice.address
        } catch (_: Exception) { "Unknown" }

        _state.value = TransportState.Connected(
            peerId       = socket.remoteDevice.address,
            peerNickname = deviceName,
            deviceName   = deviceName
        )
        startReading()
        startHeartbeat()
    }

    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (isActive && clientSocket?.isConnected == true) {
                delay(HEARTBEAT_MS)
                val socket = clientSocket
                if (socket == null || !socket.isConnected) {
                    _state.value = TransportState.Disconnected
                    break
                }
                // Отправляем ping
                try {
                    socket.outputStream.write(byteArrayOf(0)) // минимальный ping
                } catch (_: IOException) {
                    _state.value = TransportState.Disconnected
                    break
                }
            }
        }
    }

    private fun startReading() {
        readJob?.cancel()
        readJob = scope.launch {
            val buffer      = ByteArray(MAX_CHUNK)
            val accumulator = mutableListOf<Byte>()

            while (isActive) {
                try {
                    val input    = clientSocket?.inputStream ?: break
                    val bytesRead = input.read(buffer)
                    if (bytesRead < 0) break

                    accumulator.addAll(buffer.take(bytesRead).toTypedArray())

                    while (accumulator.size >= HEADER_SIZE) {
                        val header    = accumulator.take(HEADER_SIZE).toByteArray()
                        val packetLen = bytesToInt(header)
                        if (packetLen <= 0 || packetLen > 10_000_000) {
                            // Битый заголовок — сбрасываем буфер
                            accumulator.clear()
                            break
                        }
                        val totalLen = HEADER_SIZE + packetLen
                        if (accumulator.size < totalLen) break

                        val packetBytes = accumulator.take(totalLen).toByteArray()
                        repeat(totalLen) { accumulator.removeAt(0) }

                        val packet = PacketSerializer.deserialize(packetBytes)
                        if (packet != null) _incoming.emit(packet)
                    }
                } catch (e: IOException) { break }
            }

            if (_state.value is TransportState.Connected) {
                _state.value = TransportState.Disconnected
            }
        }
    }

    private fun addPeer(device: BluetoothDevice, rssi: Int) {
        if (!checkPermission(Manifest.permission.BLUETOOTH_CONNECT)) return
        return try {
            val peer = DiscoveredPeer(
                deviceId      = device.address,
                deviceName    = device.name ?: device.address,
                signalStrength = rssi
            )
            val current = _peers.value.toMutableList()
            if (current.none { it.deviceId == peer.deviceId }) {
                current.add(peer)
                _peers.value = current.sortedByDescending { it.signalStrength }
            }
        } catch (_: SecurityException) {}
    }

    private fun checkPermission(permission: String): Boolean {
        // На Android < 12 BLUETOOTH_CONNECT не существует — считаем что есть
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S &&
            permission == Manifest.permission.BLUETOOTH_CONNECT) return true
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S &&
            permission == Manifest.permission.BLUETOOTH_SCAN) return true
        return ContextCompat.checkSelfPermission(context, permission) ==
                PackageManager.PERMISSION_GRANTED
    }

    private fun bytesToInt(bytes: ByteArray): Int =
        (bytes[0].toInt() and 0xFF shl 24) or
        (bytes[1].toInt() and 0xFF shl 16) or
        (bytes[2].toInt() and 0xFF shl 8)  or
        (bytes[3].toInt() and 0xFF)
}
