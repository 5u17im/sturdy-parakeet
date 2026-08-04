package com.nothingsense.ns.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val chatId: String,
    val senderId: String,
    val text: String,
    val timestamp: Long,
    val type: MessageType,
    val fileUri: String? = null,
    val fileType: String? = null,
    val fileName: String? = null,
    val status: DeliveryStatus = DeliveryStatus.SENT,
    val replyToMessageId: String? = null,
    val reactions: String? = null,
    val autoDeleteAt: Long? = null
)

enum class MessageType {
    TEXT, IMAGE, VIDEO, AUDIO, FILE
}

enum class DeliveryStatus {
    PENDING, SENT, DELIVERED, READ
}
