package com.setka.data.db.dao

import androidx.room.*
import com.setka.data.model.ChatMember
import com.setka.data.model.MemberRole
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatMemberDao {

    @Query("SELECT * FROM chat_members WHERE chatId = :chatId")
    fun getMembersOfChat(chatId: String): Flow<List<ChatMember>>

    @Query("SELECT * FROM chat_members WHERE chatId = :chatId")
    suspend fun getMembersOnce(chatId: String): List<ChatMember>

    @Query("SELECT * FROM chat_members WHERE chatId = :chatId AND userId = :userId")
    suspend fun getMember(chatId: String, userId: String): ChatMember?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: ChatMember)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMembers(members: List<ChatMember>)

    @Query("DELETE FROM chat_members WHERE chatId = :chatId AND userId = :userId")
    suspend fun removeMember(chatId: String, userId: String)

    @Query("UPDATE chat_members SET role = :role WHERE chatId = :chatId AND userId = :userId")
    suspend fun updateRole(chatId: String, userId: String, role: MemberRole)

    @Query("SELECT COUNT(*) FROM chat_members WHERE chatId = :chatId")
    suspend fun getMemberCount(chatId: String): Int
}
