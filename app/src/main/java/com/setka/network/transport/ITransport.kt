package com.setka.network.transport

import kotlinx.coroutines.flow.Flow

/**
 * Состояние подключения транспорта
 */
sealed class TransportState {
    object Idle : TransportState()
    object Scanning : TransportState()
    object Listening : TransportState()
    data class Connecting(val deviceName: String) : TransportState()
    data class Connected(val peerId: String, val peerNickname: String, val deviceName: String) : TransportState()
    data class Error(val message: String) : TransportState()
    object Disconnected : TransportState()
}

/**
 * Найденный пир (устройство поблизости)
 */
data class DiscoveredPeer(
    val deviceId: String,       // MAC или уникальный id
    val deviceName: String,     // имя устройства
    val nickname: String = "",  // никнейм в Setka (после хэндшейка)
    val signalStrength: Int = 0 // RSSI (для сортировки по близости)
)

/**
 * Единый интерфейс для всех транспортов (BT, WiFi Direct, Internet).
 * TransportManager работает только через этот интерфейс.
 */
interface ITransport {

    val state: Flow<TransportState>
    val incomingPackets: Flow<NetworkPacket>
    val discoveredPeers: Flow<List<DiscoveredPeer>>

    /** Начать поиск устройств поблизости */
    fun startDiscovery()

    /** Остановить поиск */
    fun stopDiscovery()

    /** Начать слушать входящие подключения */
    fun startListening()

    /** Подключиться к конкретному устройству */
    suspend fun connect(peer: DiscoveredPeer): Boolean

    /** Отправить пакет подключённому пиру */
    suspend fun send(packet: NetworkPacket): Boolean

    /** Отключиться */
    fun disconnect()

    /** Освободить ресурсы */
    fun release()

    /** Доступен ли транспорт прямо сейчас */
    fun isAvailable(): Boolean
}
