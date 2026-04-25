package com.setka.data.db.dao

import androidx.room.*
import com.setka.data.model.Contact
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {

    @Query("SELECT * FROM contacts WHERE isBlocked = 0 ORDER BY nickname ASC")
    fun getAllContacts(): Flow<List<Contact>>

    @Query("SELECT * FROM contacts WHERE isFavorite = 1 AND isBlocked = 0")
    fun getFavoriteContacts(): Flow<List<Contact>>

    @Query("SELECT * FROM contacts WHERE userId = :userId")
    suspend fun getContactById(userId: String): Contact?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: Contact)

    @Update
    suspend fun updateContact(contact: Contact)

    @Query("UPDATE contacts SET lastSeenAt = :time WHERE userId = :userId")
    suspend fun updateLastSeen(userId: String, time: Long = System.currentTimeMillis())

    @Query("UPDATE contacts SET isFavorite = :fav WHERE userId = :userId")
    suspend fun setFavorite(userId: String, fav: Boolean)

    @Query("UPDATE contacts SET isBlocked = :blocked WHERE userId = :userId")
    suspend fun setBlocked(userId: String, blocked: Boolean)

    @Query("SELECT * FROM contacts WHERE nickname LIKE '%' || :query || '%' AND isBlocked = 0")
    fun searchContacts(query: String): Flow<List<Contact>>
}
