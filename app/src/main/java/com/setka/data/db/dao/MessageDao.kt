package com.setka.data.db.dao

import androidx.room.*
import com.setka.data.model.Message
import com.setka.data.model.MessageStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {

    @Query("SELECT * FROM messages WHERE chatId = :chatId AND isDeleted = 0 ORDER BY sentAt ASC")
    fun getMessagesForChat(chatId: String): Flow<List<Message>>

    @Query("SELECT * FROM messages WHERE id = :messageId")
    suspend fun getMessageById(messageId: String): Message?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: Message)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<Message>)

    @Update
    suspend fun updateMessage(message: Message)

    @Query("UPDATE messages SET status = :status WHERE id = :messageId")
    suspend fun updateMessageStatus(messageId: String, status: MessageStatus)

    @Query("UPDATE messages SET status = :status WHERE id = :messageId AND deliveredAt IS NULL")
    suspend fun markDelivered(messageId: String, status: MessageStatus = MessageStatus.DELIVERED)

    @Query("UPDATE messages SET status = :status, readAt = :readAt WHERE chatId = :chatId AND senderId != :myUserId AND status != 'READ'")
    suspend fun markAllReadInChat(chatId: String, myUserId: String, status: MessageStatus = MessageStatus.READ, readAt: Long = System.currentTimeMillis())

    @Query("UPDATE messages SET isDeleted = 1 WHERE id = :messageId")
    suspend fun softDeleteMessage(messageId: String)

    @Query("UPDATE messages SET text = :newText, isEdited = 1 WHERE id = :messageId")
    suspend fun editMessage(messageId: String, newText: String)

    @Query("SELECT COUNT(*) FROM messages WHERE chatId = :chatId AND senderId != :myUserId AND status != 'READ' AND isDeleted = 0")
    fun getUnreadCount(chatId: String, myUserId: String): Flow<Int>

    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY sentAt DESC LIMIT 1")
    suspend fun getLastMessage(chatId: String): Message?

    @Query("SELECT * FROM messages WHERE chatId = :chatId AND (text LIKE '%' || :query || '%') AND isDeleted = 0 ORDER BY sentAt DESC")
    fun searchMessages(chatId: String, query: String): Flow<List<Message>>
}
