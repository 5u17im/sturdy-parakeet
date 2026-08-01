package com.nothingsense.ns.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chats")
data class ChatEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: ChatType,
    val lastMessage: String?,
    val lastMessageTimestamp: Long?
)

enum class ChatType {
    PRIVATE, GROUP, CHANNEL
}
