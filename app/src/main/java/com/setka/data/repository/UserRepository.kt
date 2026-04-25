package com.setka.data.repository

import android.content.Context
import androidx.core.content.edit
import com.setka.data.db.SetkaDatabase
import com.setka.data.model.User
import java.util.UUID

class UserRepository(private val context: Context, db: SetkaDatabase) {

    private val dao = db.userDao()
    private val prefs = context.getSharedPreferences("setka_prefs", Context.MODE_PRIVATE)

    /**
     * Получить текущего пользователя.
     * Если не создан — создаём с временным никнеймом.
     */
    suspend fun getMyUser(): User {
        return dao.getMyUser() ?: createNewUser("User_${UUID.randomUUID().toString().take(4)}")
    }

    suspend fun createNewUser(nickname: String): User {
        val user = User(
            id = getOrCreateUserId(),
            nickname = nickname,
            avatarColor = generateAvatarColor(nickname)
        )
        dao.insertUser(user)
        return user
    }

    suspend fun updateNickname(nickname: String) {
        val user = getMyUser()
        dao.updateNickname(user.id, nickname)
    }

    /**
     * Сохраняем id в SharedPreferences — он должен быть стабильным.
     */
    private fun getOrCreateUserId(): String {
        return prefs.getString("user_id", null) ?: run {
            val newId = UUID.randomUUID().toString()
            prefs.edit { putString("user_id", newId) }
            newId
        }
    }

    private fun generateAvatarColor(nickname: String): Int {
        val colors = listOf(
            0xFF1E88E5.toInt(), // синий
            0xFF43A047.toInt(), // зелёный
            0xFFE53935.toInt(), // красный
            0xFF8E24AA.toInt(), // фиолетовый
            0xFFF4511E.toInt(), // оранжевый
            0xFF00ACC1.toInt(), // бирюзовый
            0xFFD81B60.toInt(), // розовый
            0xFF3949AB.toInt()  // индиго
        )
        return colors[nickname.hashCode().and(0x7FFFFFFF) % colors.size]
    }
}
