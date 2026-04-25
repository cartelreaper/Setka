package com.setka.data.db.dao

import androidx.room.*
import com.setka.data.model.MessageRoute
import com.setka.data.model.UserStats
import kotlinx.coroutines.flow.Flow

@Dao
interface StatsDao {

    @Query("SELECT * FROM user_stats WHERE userId = 'me' LIMIT 1")
    fun getMyStats(): Flow<UserStats?>

    @Query("SELECT * FROM user_stats WHERE userId = 'me' LIMIT 1")
    suspend fun getMyStatsOnce(): UserStats?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveStats(stats: UserStats)

    @Query("SELECT * FROM message_routes WHERE messageId = :messageId")
    suspend fun getRoute(messageId: String): MessageRoute?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveRoute(route: MessageRoute)

    @Query("SELECT * FROM message_routes ORDER BY hopCount DESC LIMIT 10")
    suspend fun getTopRoutes(): List<MessageRoute>
}
