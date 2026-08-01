package com.nothingsense.ns.network.model

import kotlinx.serialization.Serializable

@Serializable
data class FileMetadata(
    val fileId: Long,
    val fileName: String,
    val fileType: String,
    val fileSize: Long
)

@Serializable
data class MeshPacket(
    val senderId: String,
    val senderName: String,
    val type: PacketType,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val fileMetadata: FileMetadata? = null
)

enum class PacketType {
    PRIVATE_MESSAGE,
    CHANNEL_MESSAGE,
    STATUS_UPDATE,
    FILE_TRANSFER,
    HANDSHAKE
}
