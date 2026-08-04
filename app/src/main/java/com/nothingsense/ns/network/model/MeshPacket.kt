package com.nothingsense.ns.network.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class FileMetadata(
    val fileId: Long,
    val fileName: String,
    val fileType: String,
    val fileSize: Long
)

@Serializable
data class MeshPacket(
    val packetId: String = UUID.randomUUID().toString(),
    val senderId: String,
    val senderName: String,
    val recipientId: String? = null,
    val type: PacketType,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val ttl: Int = 10,
    val hopCount: Int = 0,
    val signature: String? = null,
    val fileMetadata: FileMetadata? = null,
    val replyToMessageId: String? = null,
    val autoDeleteSeconds: Long? = null,
    val proofOfWorkNonce: Long? = null
)

enum class PacketType {
    PRIVATE_MESSAGE,
    CHANNEL_MESSAGE,
    STATUS_UPDATE,
    FILE_TRANSFER,
    HANDSHAKE,
    CHANNEL_UPDATE,
    STICKER,
    AUDIO_STREAM,
    CALL_SIGNAL,
    USER_REPORT,
    ACK_DELIVERY,
    ACK_READ,
    EMOJI_REACTION,
    EDIT_MESSAGE,
    REVOKE_MESSAGE
}
