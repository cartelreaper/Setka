package com.setka.data.db.dao

import androidx.room.*
import com.setka.data.model.Chat
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {

    @Query("SELECT * FROM chats ORDER BY isPinned DESC, lastMessageAt DESC")
    fun getAllChats(): Flow<List<Chat>>

    @Query("SELECT * FROM chats WHERE id = :chatId")
    suspend fun getChatById(chatId: String): Chat?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChat(chat: Chat)

    @Update
    suspend fun updateChat(chat: Chat)

    @Query("UPDATE chats SET lastMessageText = :text, lastMessageAt = :at, unreadCount = unreadCount + 1 WHERE id = :chatId")
    suspend fun updateLastMessage(chatId: String, text: String, at: Long)

    @Query("UPDATE chats SET unreadCount = 0 WHERE id = :chatId")
    suspend fun resetUnreadCount(chatId: String)

    @Query("UPDATE chats SET isPinned = :pinned WHERE id = :chatId")
    suspend fun setPinned(chatId: String, pinned: Boolean)

    @Query("UPDATE chats SET isMuted = :muted WHERE id = :chatId")
    suspend fun setMuted(chatId: String, muted: Boolean)

    @Query("DELETE FROM chats WHERE id = :chatId")
    suspend fun deleteChat(chatId: String)

    @Query("SELECT * FROM chats WHERE name LIKE '%' || :query || '%'")
    fun searchChats(query: String): Flow<List<Chat>>
}
