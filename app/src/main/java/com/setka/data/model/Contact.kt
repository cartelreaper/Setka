package com.setka.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "contacts")
data class Contact(
    @PrimaryKey val userId: String,
    val nickname: String,
    val avatarColor: Int    = 0,
    val publicKey: String   = "",   // публичный ключ для E2E шифрования
    val lastSeenAt: Long    = 0L,
    val addedAt: Long       = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val isBlocked: Boolean  = false
)
