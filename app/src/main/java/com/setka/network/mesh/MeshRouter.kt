package com.setka.network.mesh

import com.google.gson.Gson
import com.setka.network.transport.NetworkPacket
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

data class MeshPayload(
    val originalPayload: String = "",
    val targetUserId: String    = "",
    val ttl: Int                = MeshRouter.DEFAULT_TTL,
    val hopCount: Int           = 0
)

/**
 * MeshRouter — store-and-forward маршрутизация.
 * Каждый пакет содержит TTL и targetUserId.
 * Если пакет не для нас и TTL > 0 — пересылаем дальше.
 */
class MeshRouter(private val myUserId: String) {

    companion object {
        const val DEFAULT_TTL = 7
        const val MAX_SEEN    = 500
    }

    private val gson = Gson()

    private val _localPackets = MutableSharedFlow<NetworkPacket>(extraBufferCapacity = 64)
    val localPackets: SharedFlow<NetworkPacket> = _localPackets.asSharedFlow()

    private val _forwardPackets = MutableSharedFlow<NetworkPacket>(extraBufferCapacity = 64)
    val forwardPackets: SharedFlow<NetworkPacket> = _forwardPackets.asSharedFlow()

    private val seenPackets = ArrayDeque<String>()

    suspend fun route(packet: NetworkPacket) {
        if (seenPackets.contains(packet.id)) return
        addToSeen(packet.id)

        val mesh = parseMesh(packet.payload)

        when {
            mesh == null -> {
                // Обычный пакет без mesh-обёртки — доставляем локально
                _localPackets.emit(packet)
            }
            mesh.targetUserId.isBlank() -> {
                // Широковещательный — и локально и пересылаем
                _localPackets.emit(unwrap(packet, mesh))
                forward(packet, mesh)
            }
            mesh.targetUserId == myUserId -> {
                // Для нас
                _localPackets.emit(unwrap(packet, mesh))
            }
            else -> {
                // Не для нас — пересылаем
                forward(packet, mesh)
            }
        }
    }

    fun wrap(packet: NetworkPacket, targetUserId: String, ttl: Int = DEFAULT_TTL): NetworkPacket {
        val mesh = MeshPayload(
            originalPayload = packet.payload,
            targetUserId    = targetUserId,
            ttl             = ttl,
            hopCount        = 0
        )
        return packet.copy(payload = gson.toJson(mesh))
    }

    private suspend fun forward(packet: NetworkPacket, mesh: MeshPayload) {
        if (mesh.ttl <= 0) return
        val updated = mesh.copy(ttl = mesh.ttl - 1, hopCount = mesh.hopCount + 1)
        _forwardPackets.emit(packet.copy(payload = gson.toJson(updated)))
    }

    private fun unwrap(packet: NetworkPacket, mesh: MeshPayload): NetworkPacket =
        packet.copy(payload = mesh.originalPayload)

    private fun parseMesh(payload: String): MeshPayload? {
        return try {
            if (!payload.contains("targetUserId")) return null
            gson.fromJson(payload, MeshPayload::class.java)
        } catch (_: Exception) { null }
    }

    private fun addToSeen(packetId: String) {
        if (seenPackets.size >= MAX_SEEN) seenPackets.removeFirst()
        seenPackets.addLast(packetId)
    }

    fun clearSeen() = seenPackets.clear()
}
