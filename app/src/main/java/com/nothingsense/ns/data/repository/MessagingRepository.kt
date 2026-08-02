package com.nothingsense.ns.data.repository

import android.content.Context
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
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
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessagingRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val chatDao: ChatDao,
    private val messageDao: MessageDao,
    private val meshManager: MeshManager,
    private val identityManager: IdentityManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        observeIncomingPackets()
        observePeerConnections()
    }

    private fun observeIncomingPackets() {
        scope.launch {
            meshManager.incomingPackets.collect { packet ->
                handleIncomingPacket(packet)
            }
        }
    }

    private fun observePeerConnections() {
        scope.launch {
            meshManager.peerConnectedEvent.collect { node ->
                createPrivateChat(node.userId, node.username)
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
        val fileMeta = packet.fileMetadata
        val mimeType = fileMeta?.fileType ?: "application/octet-stream"
        val messageType = when {
            mimeType.startsWith("image/") -> MessageType.IMAGE
            mimeType.startsWith("video/") -> MessageType.VIDEO
            mimeType.startsWith("audio/") -> MessageType.AUDIO
            else -> MessageType.FILE
        }

        val messageEntity = MessageEntity(
            id = UUID.randomUUID().toString(),
            chatId = packet.senderId,
            senderId = packet.senderId,
            text = fileMeta?.fileName ?: "Received File",
            timestamp = packet.timestamp,
            type = messageType,
            fileUri = packet.content, // contains the saved local file Uri
            fileType = mimeType,
            fileName = fileMeta?.fileName ?: "File"
        )

        val chat = chatDao.getChatById(packet.senderId)
        if (chat == null) {
            chatDao.insertChat(
                ChatEntity(
                    id = packet.senderId,
                    name = packet.senderName,
                    type = ChatType.PRIVATE,
                    lastMessage = "[File] ${fileMeta?.fileName ?: ""}",
                    lastMessageTimestamp = packet.timestamp
                )
            )
        } else {
            chatDao.insertChat(
                chat.copy(
                    lastMessage = "[File] ${fileMeta?.fileName ?: ""}",
                    lastMessageTimestamp = packet.timestamp
                )
            )
        }
        messageDao.insertMessage(messageEntity)
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
        
        var fileName = "file_${System.currentTimeMillis()}"
        var fileSize = 0L
        var mimeType: String? = context.contentResolver.getType(uri)

        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIndex != -1) cursor.getString(nameIndex)?.let { fileName = it }
                    if (sizeIndex != -1) fileSize = cursor.getLong(sizeIndex)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("MessagingRepository", "Error querying file metadata", e)
        }

        if (mimeType == null) {
            val extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
            if (extension != null) {
                mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase())
            }
        }
        val finalMimeType = mimeType ?: "application/octet-stream"

        val type = when {
            finalMimeType.startsWith("image/") -> MessageType.IMAGE
            finalMimeType.startsWith("video/") -> MessageType.VIDEO
            finalMimeType.startsWith("audio/") -> MessageType.AUDIO
            else -> MessageType.FILE
        }

        val messageEntity = MessageEntity(
            id = UUID.randomUUID().toString(),
            chatId = chatId,
            senderId = userId,
            text = fileName,
            timestamp = timestamp,
            type = type,
            fileUri = uri.toString(),
            fileType = finalMimeType,
            fileName = fileName
        )
        
        messageDao.insertMessage(messageEntity)

        val chat = chatDao.getChatById(chatId)
        if (chat != null) {
            chatDao.insertChat(chat.copy(
                lastMessage = "[File] $fileName",
                lastMessageTimestamp = timestamp
            ))
        }

        val packet = MeshPacket(
            senderId = userId,
            senderName = username,
            type = PacketType.FILE_TRANSFER,
            content = "[File]",
            timestamp = timestamp,
            fileMetadata = FileMetadata(0, fileName, finalMimeType, fileSize)
        )
        
        val isChannel = chatId == "PUBLIC_CHANNEL"
        if (isChannel) {
            meshManager.sendFile(uri, packet, null)
        } else {
            val connectedNodes = meshManager.connectedNodes.value
            val targetEndpoint = connectedNodes.values.find { it.userId == chatId }?.endpointId
            if (targetEndpoint != null) {
                meshManager.sendFile(uri, packet, targetEndpoint)
            }
        }
    }
}
