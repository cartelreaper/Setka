package com.setka.data.model

import androidx.room.Entity

enum class MemberRole {
    MEMBER,
    ADMIN,
    OWNER
}

/**
 * Участник чата — связывает чат и пользователя.
 * Используется для групповых чатов.
 */
@Entity(
    tableName = "chat_members",
    primaryKeys = ["chatId", "userId"]
)
data class ChatMember(
    val chatId: String,
    val userId: String,
    val nickname: String,
    val role: MemberRole = MemberRole.MEMBER,
    val joinedAt: Long = System.currentTimeMillis()
)
