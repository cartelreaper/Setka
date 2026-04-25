package com.setka.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class MessageType {
    TEXT,
    IMAGE,
    FILE,
    VOICE,
    SYSTEM   // системное сообщение (вошёл в группу и т.д.)
}

enum class MessageStatus {
    SENDING,    // отправляется
    SENT,       // отправлено (дошло до устройства/сервера)
    DELIVERED,  // доставлено получателю
    READ,       // прочитано
    FAILED      // ошибка отправки
}

enum class TransportType {
    BLUETOOTH,
    WIFI_DIRECT,
    INTERNET,
    LOCAL      // локально (сам себе)
}

/**
 * Сообщение — центральная сущность приложения.
 * Хранит всё: текст, медиа, статус, через какой транспорт пришло.
 */
@Entity(tableName = "messages")
data class Message(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val chatId: String,                     // к какому чату относится
    val senderId: String,                   // id отправителя
    val senderNickname: String,             // никнейм на момент отправки
    val type: MessageType = MessageType.TEXT,
    val text: String = "",                  // текст сообщения
    val mediaPath: String? = null,          // путь к файлу/фото/голосу на устройстве
    val mediaName: String? = null,          // оригинальное имя файла
    val mediaSizeBytes: Long = 0L,          // размер файла
    val voiceDurationMs: Long = 0L,         // длительность голосового
    val status: MessageStatus = MessageStatus.SENDING,
    val transport: TransportType = TransportType.BLUETOOTH,
    val sentAt: Long = System.currentTimeMillis(),
    val deliveredAt: Long? = null,
    val readAt: Long? = null,
    val replyToMessageId: String? = null,   // ответ на сообщение
    val isDeleted: Boolean = false,
    val isEdited: Boolean = false
)
