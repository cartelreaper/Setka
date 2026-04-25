package com.setka.network.transport

import android.util.Base64
import com.google.gson.Gson
import com.google.gson.GsonBuilder

object PacketSerializer {

    private val gson: Gson = GsonBuilder().create()

    /**
     * Пакет → байты для отправки через сокет.
     * Формат: [4 байта длина JSON][JSON][4 байта длина бинарных данных][бинарные данные]
     */
    fun serialize(packet: NetworkPacket): ByteArray {
        // Бинарные данные кодируем в Base64 и кладём в JSON
        val packetForJson = if (packet.binaryData != null) {
            packet.copy(
                payload = packet.payload + "|binary:" + Base64.encodeToString(packet.binaryData, Base64.NO_WRAP)
            )
        } else packet

        val json = gson.toJson(packetForJson.copy(binaryData = null))
        val jsonBytes = json.toByteArray(Charsets.UTF_8)
        val lengthBytes = intToBytes(jsonBytes.size)

        return lengthBytes + jsonBytes
    }

    /**
     * Байты → пакет.
     * Возвращает null если данные повреждены.
     */
    fun deserialize(data: ByteArray): NetworkPacket? {
        return try {
            if (data.size < 4) return null
            val jsonLength = bytesToInt(data.sliceArray(0..3))
            if (data.size < 4 + jsonLength) return null

            val json = String(data.sliceArray(4 until 4 + jsonLength), Charsets.UTF_8)
            var packet = gson.fromJson(json, NetworkPacket::class.java)

            // Извлекаем бинарные данные если есть
            if (packet.payload.contains("|binary:")) {
                val parts = packet.payload.split("|binary:")
                val binaryData = Base64.decode(parts[1], Base64.NO_WRAP)
                packet = packet.copy(
                    payload = parts[0],
                    binaryData = binaryData
                )
            }

            packet
        } catch (e: Exception) {
            null
        }
    }

    fun toJson(any: Any): String = gson.toJson(any)

    fun <T> fromJson(json: String, clazz: Class<T>): T = gson.fromJson(json, clazz)

    private fun intToBytes(value: Int): ByteArray = byteArrayOf(
        (value shr 24).toByte(),
        (value shr 16).toByte(),
        (value shr 8).toByte(),
        value.toByte()
    )

    private fun bytesToInt(bytes: ByteArray): Int =
        (bytes[0].toInt() and 0xFF shl 24) or
        (bytes[1].toInt() and 0xFF shl 16) or
        (bytes[2].toInt() and 0xFF shl 8) or
        (bytes[3].toInt() and 0xFF)
}
