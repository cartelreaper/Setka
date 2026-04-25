package com.setka.data.repository

import com.setka.data.db.SetkaDatabase
import com.setka.data.model.MessageRoute
import com.setka.data.model.UserStats
import kotlinx.coroutines.flow.Flow

class StatsRepository(db: SetkaDatabase) {

    private val dao = db.statsDao()

    fun getMyStats(): Flow<UserStats?> = dao.getMyStats()

    suspend fun getOrCreate(): UserStats =
        dao.getMyStatsOnce() ?: UserStats().also { dao.saveStats(it) }

    suspend fun onMessageSent() {
        val s = getOrCreate()
        val updated = s.copy(
            totalMessagesSent  = s.totalMessagesSent + 1,
            achieveFirstMessage = true,
            achieve100Messages  = s.totalMessagesSent + 1 >= 100
        )
        dao.saveStats(updated)
    }

    suspend fun onMessageRelayed(hopCount: Int, path: List<String>) {
        val s = getOrCreate()
        val isNewRecord = hopCount > s.longestChain
        val updated = s.copy(
            totalMessagesRelayed = s.totalMessagesRelayed + 1,
            achieveFirstBridge   = true,
            longestChain         = if (isNewRecord) hopCount else s.longestChain,
            longestChainPath     = if (isNewRecord) path.joinToString("→") else s.longestChainPath,
            achieve10Nodes       = s.achieve10Nodes || hopCount >= 10
        )
        dao.saveStats(updated)
    }

    suspend fun onMessageSaved() {
        val s = getOrCreate()
        dao.saveStats(s.copy(
            messagesSaved        = s.messagesSaved + 1,
            achieveSavedMessage  = true
        ))
    }

    suspend fun onNewConnection() {
        val s = getOrCreate()
        dao.saveStats(s.copy(totalConnections = s.totalConnections + 1))
    }

    suspend fun onUniquePeer() {
        val s = getOrCreate()
        dao.saveStats(s.copy(uniquePeers = s.uniquePeers + 1))
    }

    suspend fun checkNightOwl() {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        if (hour in 2..4) {
            val s = getOrCreate()
            dao.saveStats(s.copy(achieveNightOwl = true))
        }
    }

    suspend fun saveRoute(route: MessageRoute) = dao.saveRoute(route)
    suspend fun getRoute(messageId: String): MessageRoute? = dao.getRoute(messageId)
    suspend fun getTopRoutes(): List<MessageRoute> = dao.getTopRoutes()
}
