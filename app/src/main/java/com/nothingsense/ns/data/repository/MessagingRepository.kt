package com.nothingsense.ns.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.provider.OpenableColumns
import android.util.Base64
import android.webkit.MimeTypeMap
import com.nothingsense.ns.data.identity.IdentityManager
import com.nothingsense.ns.data.local.dao.ChatDao
import com.nothingsense.ns.data.local.dao.MessageDao
import com.nothingsense.ns.data.local.entities.ChatEntity
import com.nothingsense.ns.data.local.entities.ChatType
import com.nothingsense.ns.data.local.entities.MessageEntity
import com.nothingsense.ns.data.local.entities.MessageType
import com.nothingsense.ns.network.HybridTransportManager
import com.nothingsense.ns.network.model.FileMetadata
import com.nothingsense.ns.network.model.MeshPacket
import com.nothingsense.ns.network.model.PacketType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessagingRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val chatDao: ChatDao,
    private val messageDao: MessageDao,
    private val transportManager: HybridTransportManager,
    private val identityManager: IdentityManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        observeIncomingPackets()
        observePeerConnections()
    }

    private fun observeIncomingPackets() {
        scope.launch {
            transportManager.incomingPackets.collect { packet ->
                handleIncomingPacket(packet)
            }
        }
    }

    private fun observePeerConnections() {
        scope.launch {
            transportManager.peerConnectedEvent.collect { node ->
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
                saveMessage(packet, "PUBLIC_CHANNEL", "Canal Público")
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

        val targetChatId = if (packet.recipientId == null || packet.recipientId == "PUBLIC_CHANNEL") "PUBLIC_CHANNEL" else packet.senderId

        var savedFileUri: String? = null
        try {
            val base64Data = packet.content
            val fileBytes = Base64.decode(base64Data, Base64.NO_WRAP)

            val receivedDir = File(context.filesDir, "received_files")
            if (!receivedDir.exists()) receivedDir.mkdirs()

            val fileName = fileMeta?.fileName ?: "file_${System.currentTimeMillis()}"
            val destFile = File(receivedDir, "${System.currentTimeMillis()}_$fileName")

            FileOutputStream(destFile).use { output ->
                output.write(fileBytes)
            }
            savedFileUri = android.net.Uri.fromFile(destFile).toString()
        } catch (e: Exception) {
            android.util.Log.e("MessagingRepository", "Failed to decode/save incoming file", e)
        }

        if (savedFileUri == null) return

        val messageEntity = MessageEntity(
            id = UUID.randomUUID().toString(),
            chatId = targetChatId,
            senderId = packet.senderId,
            text = fileMeta?.fileName ?: "Archivo Recibido",
            timestamp = packet.timestamp,
            type = messageType,
            fileUri = savedFileUri,
            fileType = mimeType,
            fileName = fileMeta?.fileName ?: "Archivo"
        )

        val chat = chatDao.getChatById(targetChatId)
        if (chat == null) {
            chatDao.insertChat(
                ChatEntity(
                    id = targetChatId,
                    name = if (targetChatId == "PUBLIC_CHANNEL") "Canal Público" else packet.senderName,
                    type = if (targetChatId == "PUBLIC_CHANNEL") ChatType.CHANNEL else ChatType.PRIVATE,
                    lastMessage = "📎 ${fileMeta?.fileName ?: "Archivo"}",
                    lastMessageTimestamp = packet.timestamp
                )
            )
        } else {
            chatDao.insertChat(
                chat.copy(
                    lastMessage = "📎 ${fileMeta?.fileName ?: "Archivo"}",
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
            recipientId = if (isChannel) null else chatId,
            type = if (isChannel) PacketType.CHANNEL_MESSAGE else PacketType.PRIVATE_MESSAGE,
            content = text,
            timestamp = timestamp
        )
        
        transportManager.sendPacket(packet, if (isChannel) null else chatId)
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

        val base64Content: String
        try {
            val rawBytes = if (finalMimeType.startsWith("image/")) {
                compressImage(uri, maxWidth = 1024, quality = 70)
            } else {
                readFileBytes(uri)
            }
            
            if (rawBytes == null || rawBytes.isEmpty()) return
            
            base64Content = Base64.encodeToString(rawBytes, Base64.NO_WRAP)
            fileSize = rawBytes.size.toLong()
        } catch (e: Exception) {
            android.util.Log.e("MessagingRepository", "Error encoding file to Base64", e)
            return
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
                lastMessage = "📎 $fileName",
                lastMessageTimestamp = timestamp
            ))
        }

        val isChannel = chatId == "PUBLIC_CHANNEL"
        val packet = MeshPacket(
            senderId = userId,
            senderName = username,
            recipientId = if (isChannel) null else chatId,
            type = PacketType.FILE_TRANSFER,
            content = base64Content,
            timestamp = timestamp,
            fileMetadata = FileMetadata(0, fileName, finalMimeType, fileSize)
        )

        transportManager.sendPacket(packet, if (isChannel) null else chatId)
    }

    private fun compressImage(uri: android.net.Uri, maxWidth: Int, quality: Int): ByteArray? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream.close()

            val originalWidth = options.outWidth
            val originalHeight = options.outHeight
            var sampleSize = 1
            while (originalWidth / sampleSize > maxWidth * 2 || originalHeight / sampleSize > maxWidth * 2) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val inputStream2 = context.contentResolver.openInputStream(uri) ?: return null
            val bitmap = BitmapFactory.decodeStream(inputStream2, null, decodeOptions)
            inputStream2.close()

            if (bitmap == null) return null

            val scaledBitmap = if (bitmap.width > maxWidth) {
                val ratio = maxWidth.toFloat() / bitmap.width
                val newHeight = (bitmap.height * ratio).toInt()
                Bitmap.createScaledBitmap(bitmap, maxWidth, newHeight, true).also {
                    if (it != bitmap) bitmap.recycle()
                }
            } else {
                bitmap
            }

            val outputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            scaledBitmap.recycle()

            outputStream.toByteArray()
        } catch (e: Exception) {
            android.util.Log.e("MessagingRepository", "Image compression failed", e)
            readFileBytes(uri)
        }
    }

    private fun readFileBytes(uri: android.net.Uri): ByteArray? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        } catch (e: Exception) {
            android.util.Log.e("MessagingRepository", "Read file bytes failed", e)
            null
        }
    }
}
