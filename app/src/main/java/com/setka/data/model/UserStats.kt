package com.setka.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Маршрут сообщения — хранит путь A→B→C→D
 */
@Entity(tableName = "message_routes")
data class MessageRoute(
    @PrimaryKey val messageId: String,
    val hops: String,           // JSON список: ["userA","userB","userC"]
    val hopCount: Int = 0,
    val totalDistanceM: Int = 0 // примерная дистанция в метрах
)

/**
 * Статистика пользователя — для геймификации
 */
@Entity(tableName = "user_stats")
data class UserStats(
    @PrimaryKey val userId: String = "me",
    val totalMessagesSent: Int     = 0,
    val totalMessagesRelayed: Int  = 0,  // сколько чужих сообщений переслали
    val longestChain: Int          = 0,  // рекорд самой длинной цепочки
    val longestChainPath: String   = "", // путь рекордной цепочки
    val totalConnections: Int      = 0,  // сколько раз подключались
    val uniquePeers: Int           = 0,  // сколько уникальных людей встретили
    val messagesSaved: Int         = 0,  // сообщений "спасено" (дошло через mesh)
    // Достижения (true = разблокировано)
    val achieveFirstMessage: Boolean  = false, // первое сообщение
    val achieveFirstBridge: Boolean   = false, // стал мостом
    val achieve10Nodes: Boolean       = false, // цепочка 10+ узлов
    val achieveSavedMessage: Boolean  = false, // спас сообщение от обрыва
    val achieve100Messages: Boolean   = false, // 100 сообщений
    val achieveNightOwl: Boolean      = false  // отправил сообщение после 2 ночи
)
