package com.setka.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Пользователь — только никнейм, никакой регистрации.
 * id генерируется локально и хранится на устройстве.
 */
@Entity(tableName = "users")
data class User(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val nickname: String,
    val avatarColor: Int = 0,      // цвет аватара (генерируется из никнейма)
    val publicKey: String = "",    // для шифрования (следующая версия)
    val createdAt: Long = System.currentTimeMillis()
)
