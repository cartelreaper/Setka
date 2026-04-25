package com.setka.data.repository

import com.setka.data.db.SetkaDatabase
import com.setka.data.model.Contact
import kotlinx.coroutines.flow.Flow

class ContactRepository(db: SetkaDatabase) {

    private val dao = db.contactDao()

    fun getAllContacts(): Flow<List<Contact>> = dao.getAllContacts()

    fun getFavorites(): Flow<List<Contact>> = dao.getFavoriteContacts()

    fun searchContacts(query: String): Flow<List<Contact>> = dao.searchContacts(query)

    suspend fun getById(userId: String): Contact? = dao.getContactById(userId)

    /**
     * Добавляет контакт автоматически при первом сообщении.
     */
    suspend fun addOrUpdate(contact: Contact) = dao.insertContact(contact)

    suspend fun setFavorite(userId: String, fav: Boolean) = dao.setFavorite(userId, fav)

    suspend fun setBlocked(userId: String, blocked: Boolean) = dao.setBlocked(userId, blocked)

    suspend fun updateLastSeen(userId: String) = dao.updateLastSeen(userId)
}
