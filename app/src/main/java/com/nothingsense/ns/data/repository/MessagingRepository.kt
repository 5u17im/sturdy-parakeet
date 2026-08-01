package com.nothingsense.ns.data.repository

import com.nothingsense.ns.data.identity.IdentityManager
import com.nothingsense.ns.data.local.dao.ChatDao
import com.nothingsense.ns.data.local.dao.MessageDao
import com.nothingsense.ns.data.local.entities.ChatEntity
import com.nothingsense.ns.data.local.entities.ChatType
import com.nothingsense.ns.data.local.entities.MessageEntity
import com.nothingsense.ns.data.local.entities.MessageType
import com.nothingsense.ns.network.MeshManager
import com.nothingsense.ns.network.model.FileMetadata
import com.nothingsense.ns.network.model.MeshPacket
import com.nothingsense.ns.network.model.PacketType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessagingRepository @Inject constructor(
    private val chatDao: ChatDao,
    private val messageDao: MessageDao,
    private val meshManager: MeshManager,
    private val identityManager: IdentityManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        observeIncomingPackets()
    }

    private fun observeIncomingPackets() {
        scope.launch {
            meshManager.incomingPackets.collect { packet ->
                handleIncomingPacket(packet)
            }
        }
    }

    private suspend fun handleIncomingPacket(packet: MeshPacket) {
        when (packet.type) {
            PacketType.PRIVATE_MESSAGE -> {
                saveMessage(packet, packet.senderId, packet.senderName)
            }
            PacketType.CHANNEL_MESSAGE -> {
                saveMessage(packet, "PUBLIC_CHANNEL", "Public Channel")
            }
            PacketType.FILE_TRANSFER -> {
                handleIncomingFile(packet)
            }
            else -> {}
        }
    }

    private suspend fun handleIncomingFile(packet: MeshPacket) {
        // For simplicity, we just save a placeholder message for now
        // In a real app, we'd wait for the file payload and update the URI
        saveMessage(packet, packet.senderId, packet.senderName, isFile = true)
    }

    private suspend fun saveMessage(
        packet: MeshPacket, 
        chatId: String, 
        chatName: String, 
        isFile: Boolean = false
    ) {
        val messageEntity = MessageEntity(
            id = UUID.randomUUID().toString(),
            chatId = chatId,
            senderId = packet.senderId,
            text = packet.content,
            timestamp = packet.timestamp,
            type = if (isFile) MessageType.FILE else MessageType.TEXT,
            fileType = packet.fileMetadata?.fileType,
            fileName = packet.fileMetadata?.fileName
        )

        val chat = chatDao.getChatById(chatId)
        if (chat == null) {
            chatDao.insertChat(
                ChatEntity(
                    id = chatId,
                    name = chatName,
                    type = if (chatId == "PUBLIC_CHANNEL") ChatType.CHANNEL else ChatType.PRIVATE,
                    lastMessage = packet.content,
                    lastMessageTimestamp = packet.timestamp
                )
            )
        } else {
            chatDao.insertChat(chat.copy(
                lastMessage = packet.content,
                lastMessageTimestamp = packet.timestamp
            ))
        }
        messageDao.insertMessage(messageEntity)
    }

    fun getAllChats(): Flow<List<ChatEntity>> = chatDao.getAllChats()

    fun getMessagesForChat(chatId: String): Flow<List<MessageEntity>> = messageDao.getMessagesForChat(chatId)

    suspend fun sendMessage(chatId: String, text: String, isChannel: Boolean = false) {
        val userId = identityManager.getOrCreateUserId()
        val username = identityManager.getUsername()
        val timestamp = System.currentTimeMillis()

        val messageEntity = MessageEntity(
            id = UUID.randomUUID().toString(),
            chatId = chatId,
            senderId = userId,
            text = text,
            timestamp = timestamp,
            type = MessageType.TEXT
        )
        
        messageDao.insertMessage(messageEntity)
        
        val chat = chatDao.getChatById(chatId)
        if (chat != null) {
            chatDao.insertChat(chat.copy(
                lastMessage = text,
                lastMessageTimestamp = timestamp
            ))
        }

        val packet = MeshPacket(
            senderId = userId,
            senderName = username,
            type = if (isChannel) PacketType.CHANNEL_MESSAGE else PacketType.PRIVATE_MESSAGE,
            content = text,
            timestamp = timestamp
        )
        
        if (isChannel) {
            meshManager.sendPacket(packet) // Broadcast
        } else {
            val connectedNodes = meshManager.connectedNodes.value
            val targetEndpoint = connectedNodes.values.find { it.userId == chatId }?.endpointId
            meshManager.sendPacket(packet, targetEndpoint)
        }
    }

    suspend fun createPrivateChat(userId: String, username: String) {
        if (chatDao.getChatById(userId) == null) {
            chatDao.insertChat(
                ChatEntity(
                    id = userId,
                    name = username,
                    type = ChatType.PRIVATE,
                    lastMessage = null,
                    lastMessageTimestamp = null
                )
            )
        }
    }

    suspend fun sendFile(chatId: String, uri: android.net.Uri) {
        val userId = identityManager.getOrCreateUserId()
        val username = identityManager.getUsername()
        val timestamp = System.currentTimeMillis()
        
        // Simplified mime type detection
        val mimeType = "image/jpeg" // Should use contentResolver.getType(uri)
        val type = when {
            mimeType.startsWith("image/") -> MessageType.IMAGE
            mimeType.startsWith("video/") -> MessageType.VIDEO
            mimeType.startsWith("audio/") -> MessageType.AUDIO
            else -> MessageType.FILE
        }

        val messageEntity = MessageEntity(
            id = UUID.randomUUID().toString(),
            chatId = chatId,
            senderId = userId,
            text = "[File]",
            timestamp = timestamp,
            type = type,
            fileUri = uri.toString(),
            fileType = mimeType,
            fileName = "file" // Should get real name
        )
        
        messageDao.insertMessage(messageEntity)

        val packet = MeshPacket(
            senderId = userId,
            senderName = username,
            type = PacketType.FILE_TRANSFER,
            content = "[File]",
            timestamp = timestamp,
            fileMetadata = FileMetadata(0, "file", mimeType, 0)
        )
        
        val connectedNodes = meshManager.connectedNodes.value
        val targetEndpoint = connectedNodes.values.find { it.userId == chatId }?.endpointId
        
        if (targetEndpoint != null) {
            meshManager.sendFile(uri, packet, targetEndpoint)
        }
    }
}
