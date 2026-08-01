package com.nothingsense.ns.data.local

import androidx.room.TypeConverter
import com.nothingsense.ns.data.local.entities.ChatType
import com.nothingsense.ns.data.local.entities.MessageType

class Converters {
    @TypeConverter
    fun fromChatType(value: ChatType): String = value.name

    @TypeConverter
    fun toChatType(value: String): ChatType = ChatType.valueOf(value)

    @TypeConverter
    fun fromMessageType(value: MessageType): String = value.name

    @TypeConverter
    fun toMessageType(value: String): MessageType = MessageType.valueOf(value)
}
