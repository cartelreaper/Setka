package com.setka.network.transport

import com.setka.data.model.TransportType
import java.util.UUID

/**
 * Тип пакета — что именно передаём
 */
enum class PacketType {
    MESSAGE,          // текстовое сообщение
    MESSAGE_STATUS,   // обновление статуса (доставлено/прочитано)
    VOICE,            // голосовое сообщение (бинарные данные)
    IMAGE,            // фото
    FILE_CHUNK,       // кусок файла (для больших файлов)
    FILE_INFO,        // мета-информация о файле перед передачей
    HANDSHAKE,        // приветствие при подключении (обмен никнеймами/id)
    PING,             // проверка соединения
    TYPING,           // печатает...
    USER_INFO         // обновление профиля
}

/**
 * Универсальный пакет — одинаковый для BT, WiFi Direct и интернета.
 * Сериализуется в JSON для передачи.
 */
data class NetworkPacket(
    val id: String = UUID.randomUUID().toString(),
    val type: PacketType,
    val senderId: String,
    val senderNickname: String,
    val chatId: String = "",
    val payload: String = "",           // JSON-строка с данными
    val binaryData: ByteArray? = null,  // для голоса/фото/файлов
    val timestamp: Long = System.currentTimeMillis(),
    val transport: TransportType = TransportType.BLUETOOTH
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is NetworkPacket) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}
