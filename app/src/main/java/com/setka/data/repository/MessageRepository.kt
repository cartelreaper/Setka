package com.setka.data.repository

import com.setka.data.db.SetkaDatabase
import com.setka.data.model.Message
import com.setka.data.model.MessageStatus
import kotlinx.coroutines.flow.Flow

class MessageRepository(db: SetkaDatabase) {

    private val dao = db.messageDao()
    private val chatDao = db.chatDao()

    fun getMessages(chatId: String): Flow<List<Message>> =
        dao.getMessagesForChat(chatId)

    fun getUnreadCount(chatId: String, myUserId: String): Flow<Int> =
        dao.getUnreadCount(chatId, myUserId)

    fun searchMessages(chatId: String, query: String): Flow<List<Message>> =
        dao.searchMessages(chatId, query)

    suspend fun sendMessage(message: Message) {
        dao.insertMessage(message)
        val preview = when {
            message.text.isNotEmpty() -> message.text
            message.type.name == "IMAGE" -> "Фото"
            message.type.name == "VOICE" -> "Голосовое сообщение"
            message.type.name == "FILE" -> message.mediaName ?: "Файл"
            else -> ""
        }
        chatDao.updateLastMessage(message.chatId, preview, message.sentAt)
    }

    suspend fun updateStatus(messageId: String, status: MessageStatus) =
        dao.updateMessageStatus(messageId, status)

    suspend fun markAllRead(chatId: String, myUserId: String) {
        dao.markAllReadInChat(chatId, myUserId)
        chatDao.resetUnreadCount(chatId)
    }

    suspend fun deleteMessage(messageId: String) =
        dao.softDeleteMessage(messageId)

    suspend fun editMessage(messageId: String, newText: String) =
        dao.editMessage(messageId, newText)

    suspend fun getById(messageId: String): Message? =
        dao.getMessageById(messageId)
}
