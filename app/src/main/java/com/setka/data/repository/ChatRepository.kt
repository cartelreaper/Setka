package com.setka.data.repository

import com.setka.data.db.SetkaDatabase
import com.setka.data.model.Chat
import com.setka.data.model.ChatMember
import com.setka.data.model.ChatType
import com.setka.data.model.MemberRole
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class ChatRepository(db: SetkaDatabase) {

    private val chatDao   = db.chatDao()
    private val memberDao = db.chatMemberDao()

    fun getAllChats(): Flow<List<Chat>> = chatDao.getAllChats()
    fun searchChats(query: String): Flow<List<Chat>> = chatDao.searchChats(query)
    suspend fun getChatById(id: String): Chat? = chatDao.getChatById(id)

    suspend fun getOrCreateDirectChat(
        myUserId: String,
        myNickname: String,
        contactUserId: String,
        contactNickname: String
    ): String {
        val chatId   = generateDirectChatId(myUserId, contactUserId)
        val existing = chatDao.getChatById(chatId)
        if (existing != null) return chatId

        chatDao.insertChat(Chat(id = chatId, type = ChatType.DIRECT, name = contactNickname))
        memberDao.insertMembers(listOf(
            ChatMember(chatId = chatId, userId = myUserId,      nickname = myNickname),
            ChatMember(chatId = chatId, userId = contactUserId, nickname = contactNickname)
        ))
        return chatId
    }

    suspend fun createGroupChat(
        name: String,
        creatorId: String,
        creatorNickname: String,
        memberIds: List<Pair<String, String>>
    ): String {
        val chatId = UUID.randomUUID().toString()
        chatDao.insertChat(Chat(id = chatId, type = ChatType.GROUP, name = name))
        val members = mutableListOf(
            ChatMember(chatId = chatId, userId = creatorId,
                nickname = creatorNickname, role = MemberRole.OWNER)
        )
        memberIds.forEach { (uid, nick) ->
            members.add(ChatMember(chatId = chatId, userId = uid, nickname = nick))
        }
        memberDao.insertMembers(members)
        return chatId
    }

    suspend fun deleteChat(chatId: String) = chatDao.deleteChat(chatId)
    suspend fun setPinned(chatId: String, pinned: Boolean) = chatDao.setPinned(chatId, pinned)
    suspend fun setMuted(chatId: String, muted: Boolean) = chatDao.setMuted(chatId, muted)

    fun getMembers(chatId: String) = memberDao.getMembersOfChat(chatId)

    // Синхронное получение участников для шифрования
    suspend fun getMembersOnce(chatId: String): List<ChatMember> =
        memberDao.getMembersOnce(chatId)

    suspend fun addMember(member: ChatMember) = memberDao.insertMember(member)
    suspend fun removeMember(chatId: String, userId: String) =
        memberDao.removeMember(chatId, userId)

    private fun generateDirectChatId(userId1: String, userId2: String): String {
        val sorted = listOf(userId1, userId2).sorted()
        return "direct_${sorted[0]}_${sorted[1]}"
    }
}
