package com.setka.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class ChatType {
    DIRECT,  // личный чат
    GROUP    // групповой чат
}

/**
 * Чат — личный или групповой.
 * Для личного чата name = никнейм собеседника.
 */
@Entity(tableName = "chats")
data class Chat(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val type: ChatType = ChatType.DIRECT,
    val name: String,                           // название группы или никнейм
    val avatarColor: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val lastMessageText: String = "",
    val lastMessageAt: Long = 0L,
    val unreadCount: Int = 0,
    val isPinned: Boolean = false,
    val isMuted: Boolean = false
)
