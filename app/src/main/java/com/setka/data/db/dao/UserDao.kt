package com.setka.data.db.dao

import androidx.room.*
import com.setka.data.model.User

@Dao
interface UserDao {

    @Query("SELECT * FROM users LIMIT 1")
    suspend fun getMyUser(): User?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    @Update
    suspend fun updateUser(user: User)

    @Query("UPDATE users SET nickname = :nickname WHERE id = :userId")
    suspend fun updateNickname(userId: String, nickname: String)
}
